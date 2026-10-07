package org.tensorflow.lite.examples.imageclassification.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.tensorflow.lite.examples.imageclassification.ImageClassifierHelper;
import org.tensorflow.lite.examples.imageclassification.LabelUtils;
import org.tensorflow.lite.examples.imageclassification.R;
import org.tensorflow.lite.examples.imageclassification.databinding.FragmentCameraBinding;
import org.tensorflow.lite.examples.imageclassification.firebase.FirebaseRepository;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.vision.classifier.Classifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraFragment extends Fragment implements ImageClassifierHelper.ClassifierListener {

    private static final String TAG = "CameraFragment";

    private FragmentCameraBinding fragmentCameraBinding;
    private ImageClassifierHelper imageClassifierHelper;
    private Bitmap bitmapBuffer;
    private ImageAnalysis imageAnalyzer;
    private ProcessCameraProvider cameraProvider;
    private final Object task = new Object();

    private FirebaseRepository firebaseRepository;
    private ExecutorService cameraExecutor;

    private FusedLocationProviderClient fusedLocationClient;
    private BarcodeScanner qrScanner;
    private SpeechRecognizer speechRecognizer;

    private boolean isFreeze = false;

    private String lastLabel = "";
    private float lastConfidence = 0.0f;
    private long lastInferenceTime = 0;
    private String currentQrCode = "";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false);
        return fragmentCameraBinding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        firebaseRepository = new FirebaseRepository();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();

        qrScanner = BarcodeScanning.getClient(options);

        initVoiceCommands();

        LabelUtils.loadLabels(requireContext());

        cameraExecutor = Executors.newSingleThreadExecutor();

        fragmentCameraBinding.viewFinder.post(() -> {
            imageClassifierHelper = ImageClassifierHelper.create(requireContext(), this);
            setUpCamera();
        });

        initFreezeButton();
        initActionButtons();

        fragmentCameraBinding.btnScanQr.setOnClickListener(v -> scanQrCode());
        fragmentCameraBinding.btnVoice.setOnClickListener(v -> startVoiceListening());
    }

    private void initVoiceCommands() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches == null || matches.isEmpty()) {
                    Toast.makeText(requireContext(), "Aucune commande détectée", Toast.LENGTH_SHORT).show();
                    return;
                }

                String command = matches.get(0).toLowerCase(Locale.ROOT);
                handleVoiceCommand(command);
            }

            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                Toast.makeText(requireContext(), "Erreur reconnaissance vocale", Toast.LENGTH_SHORT).show();
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void handleVoiceCommand(String command) {
        if (command.contains("détecter")
                || command.contains("detecter")
                || command.contains("sauvegarder")
                || command.contains("enregistrer")) {

            captureAndSave();

        } else if (command.contains("historique")) {

            Navigation.findNavController(requireView())
                    .navigate(R.id.action_camera_to_historique);

        } else if (command.contains("profil")) {

            Intent intent = new Intent(
                    requireContext(),
                    org.tensorflow.lite.examples.imageclassification.ProfileActivity.class
            );
            startActivity(intent);
        }

        Toast.makeText(requireContext(), "Commande : " + command, Toast.LENGTH_SHORT).show();
    }

    private void startVoiceListening() {
        if (speechRecognizer == null) {
            Toast.makeText(requireContext(), "Reconnaissance vocale indisponible", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    2
            );
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.startListening(intent);
    }

    private void scanQrCode() {
        if (bitmapBuffer == null) {
            Toast.makeText(requireContext(), "Aucune image disponible pour scanner QR", Toast.LENGTH_SHORT).show();
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmapBuffer, 0);

        qrScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes == null || barcodes.isEmpty()) {
                        Toast.makeText(requireContext(), "Aucun QR code détecté", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (Barcode barcode : barcodes) {
                        String value = barcode.getRawValue();

                        if (value != null && !value.isEmpty()) {
                            currentQrCode = value;

                            Toast.makeText(
                                    requireContext(),
                                    "QR Code scanné : " + currentQrCode,
                                    Toast.LENGTH_LONG
                            ).show();

                            fragmentCameraBinding.tvQrInfo.setVisibility(View.VISIBLE);
                            fragmentCameraBinding.tvQrInfo.setText("Plante associée : " + currentQrCode);
                            break;
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(
                        requireContext(),
                        "Erreur scan QR : " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show());
    }

    private void captureAndSave() {
        if (lastLabel == null || lastLabel.isEmpty()) {
            Toast.makeText(requireContext(), "Aucune détection en cours", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bitmapBuffer == null) {
            Toast.makeText(requireContext(), "Aucune image à sauvegarder", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1
            );
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    double lat = 0.0;
                    double lon = 0.0;

                    if (location != null) {
                        lat = location.getLatitude();
                        lon = location.getLongitude();
                    }

                    Bitmap imageToSave = bitmapBuffer.copy(Bitmap.Config.ARGB_8888, false);

                    firebaseRepository.uploadImageAndSaveDetection(
                            lastLabel,
                            lastConfidence,
                            lastInferenceTime,
                            lat,
                            lon,
                            currentQrCode,
                            imageToSave
                    );

                    Toast.makeText(
                            requireContext(),
                            "✅ Image + détection enregistrées",
                            Toast.LENGTH_SHORT
                    ).show();

                    currentQrCode = "";

                    if (fragmentCameraBinding != null) {
                        fragmentCameraBinding.tvQrInfo.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(
                        requireContext(),
                        "Erreur GPS : " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show());
    }
    private void initActionButtons() {
        fragmentCameraBinding.btnCapture.setOnClickListener(v -> captureAndSave());

        fragmentCameraBinding.btnHistory.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_camera_to_historique)
        );

        fragmentCameraBinding.settingsButton.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.parametres_fragment)
        );
    }

    private void initFreezeButton() {
        fragmentCameraBinding.btnFreeze.setOnClickListener(v -> {
            isFreeze = !isFreeze;

            if (isFreeze) {
                fragmentCameraBinding.btnFreeze.setImageResource(R.drawable.ic_play);
                fragmentCameraBinding.txtFreeze.setVisibility(View.VISIBLE);

                if (imageAnalyzer != null) {
                    imageAnalyzer.clearAnalyzer();
                }

            } else {
                fragmentCameraBinding.btnFreeze.setImageResource(R.drawable.ic_pause);
                fragmentCameraBinding.txtFreeze.setVisibility(View.GONE);
                bindCameraUseCases();
            }
        });
    }

    private void setUpCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "CameraProvider initialization failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null || fragmentCameraBinding == null) {
            return;
        }

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();

        imageAnalyzer = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();

        imageAnalyzer.setAnalyzer(cameraExecutor, image -> {
            if (bitmapBuffer == null) {
                bitmapBuffer = Bitmap.createBitmap(
                        image.getWidth(),
                        image.getHeight(),
                        Bitmap.Config.ARGB_8888
                );
            }

            classifyImage(image);
        });

        try {
            cameraProvider.unbindAll();

            cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
            );

            preview.setSurfaceProvider(fragmentCameraBinding.viewFinder.getSurfaceProvider());

        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void classifyImage(@NonNull ImageProxy image) {
        if (imageClassifierHelper == null || bitmapBuffer == null) {
            image.close();
            return;
        }

        try {
            bitmapBuffer.copyPixelsFromBuffer(image.getPlanes()[0].getBuffer());

            int rotation = image.getImageInfo().getRotationDegrees();

            image.close();

            synchronized (task) {
                imageClassifierHelper.classify(bitmapBuffer, rotation);
            }

        } catch (Exception e) {
            image.close();
            Log.e(TAG, "Erreur classification image", e);
        }
    }
    @Override
    public void onResults(List<Classifications> results, long inferenceTime) {
        if (!isAdded()) {
            return;
        }

        requireActivity().runOnUiThread(() -> {
            if (fragmentCameraBinding == null) {
                return;
            }

            // Vérifier si TensorFlow Lite n'a retourné aucun résultat
            if (results == null || results.isEmpty()) {
                fragmentCameraBinding.mainLabel.setText("Aucune fleur détectée");
                fragmentCameraBinding.mainScore.setText("--");
                fragmentCameraBinding.confidenceProgress.setProgress(0);
                fragmentCameraBinding.inferenceTimeVal.setText("Inférence : -- ms");
                return;
            }

            Classifications classification = results.get(0);

            if (classification == null
                    || classification.getCategories() == null
                    || classification.getCategories().isEmpty()) {

                fragmentCameraBinding.mainLabel.setText("Aucune fleur détectée");
                fragmentCameraBinding.mainScore.setText("--");
                fragmentCameraBinding.confidenceProgress.setProgress(0);
                fragmentCameraBinding.inferenceTimeVal.setText("Inférence : " + inferenceTime + " ms");
                return;
            }

            Category topCategory = classification.getCategories().get(0);

            if (topCategory == null) {
                fragmentCameraBinding.mainLabel.setText("Aucune fleur détectée");
                fragmentCameraBinding.mainScore.setText("--");
                fragmentCameraBinding.confidenceProgress.setProgress(0);
                fragmentCameraBinding.inferenceTimeVal.setText("Inférence : " + inferenceTime + " ms");
                return;
            }

            // Ici on utilise LabelUtils au lieu de topCategory.getLabel()
            String flowerName = LabelUtils.getLabelFromCategory(topCategory);

            float confidence = topCategory.getScore();
            int score = Math.round(confidence * 100);

            // Sauvegarder le dernier résultat pour Firebase
            lastLabel = flowerName;
            lastConfidence = confidence;
            lastInferenceTime = inferenceTime;

            // Affichage UI
            fragmentCameraBinding.mainLabel.setText(flowerName);
            fragmentCameraBinding.mainScore.setText(score + "%");
            fragmentCameraBinding.confidenceProgress.setProgress(score);
            fragmentCameraBinding.inferenceTimeVal.setText("Inférence : " + inferenceTime + " ms");
        });
    }

    @Override
    public void onError(String error) {
        if (!isAdded()) {
            return;
        }

        requireActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!PermissionsFragment.hasPermission(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                    .navigate(CameraFragmentDirections.actionCameraToPermissions());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
            cameraExecutor.shutdown();
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        if (qrScanner != null) {
            qrScanner.close();
        }

        fragmentCameraBinding = null;
    }
}