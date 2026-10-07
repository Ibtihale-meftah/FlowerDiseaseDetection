package org.tensorflow.lite.examples.imageclassification.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import org.tensorflow.lite.examples.imageclassification.adapters.HistoryAdapter;
import org.tensorflow.lite.examples.imageclassification.databinding.FragmentHistoriqueBinding;
import org.tensorflow.lite.examples.imageclassification.firebase.FirebaseRepository;
import org.tensorflow.lite.examples.imageclassification.firebase.FlowerDetection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoriqueFragment extends Fragment {

    private FragmentHistoriqueBinding binding;

    private FirebaseRepository firebaseRepository;
    private HistoryAdapter historyAdapter;
    private ListenerRegistration detectionsListener;

    private final List<FlowerDetection> allDetections = new ArrayList<>();
    private final List<FlowerDetection> filteredDetections = new ArrayList<>();

    private SpeechRecognizer speechRecognizer;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentHistoriqueBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        firebaseRepository = new FirebaseRepository();

        setupRecyclerView();
        setupSearchBar();
        setupVoiceSearch();
        listenDetectionsFromFirestore();
    }

    private void setupRecyclerView() {
        historyAdapter = new HistoryAdapter(filteredDetections, firebaseRepository);

        binding.rvHistorique.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistorique.setAdapter(historyAdapter);
    }

    private void setupSearchBar() {
        binding.editTextSearchHistory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDetections(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.btnClearSearchHistory.setOnClickListener(v -> {
            binding.editTextSearchHistory.setText("");
            filterDetections("");
        });
    }

    private void setupVoiceSearch() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches == null || matches.isEmpty()) {
                    Toast.makeText(requireContext(), "Aucun mot détecté", Toast.LENGTH_SHORT).show();
                    return;
                }

                String spokenText = matches.get(0).trim();

                binding.editTextSearchHistory.setText(spokenText);
                binding.editTextSearchHistory.setSelection(spokenText.length());

                filterDetections(spokenText);

                Toast.makeText(
                        requireContext(),
                        "Recherche vocale : " + spokenText,
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override public void onReadyForSpeech(Bundle params) {
                Toast.makeText(requireContext(), "Parlez maintenant...", Toast.LENGTH_SHORT).show();
            }

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

        binding.btnMicSearchHistory.setOnClickListener(v -> startVoiceSearch());
    }

    private void startVoiceSearch() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    20
            );
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Dites le nom d'une fleur");

        speechRecognizer.startListening(intent);
    }

    private void listenDetectionsFromFirestore() {
        detectionsListener = firebaseRepository.listenDetections((querySnapshot, error) -> {
            if (error != null) {
                Toast.makeText(
                        requireContext(),
                        "Erreur chargement historique : " + error.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            allDetections.clear();

            if (querySnapshot != null) {
                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                    FlowerDetection detection = document.toObject(FlowerDetection.class);

                    if (detection != null) {
                        detection.id = document.getId();
                        allDetections.add(detection);
                    }
                }
            }

            String currentQuery = binding.editTextSearchHistory.getText().toString();
            filterDetections(currentQuery);
        });
    }

    private void filterDetections(String query) {
        filteredDetections.clear();

        String cleanQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        if (cleanQuery.isEmpty()) {
            filteredDetections.addAll(allDetections);
            binding.tvSearchResultInfo.setText("Toutes les détections : " + filteredDetections.size());
        } else {
            for (FlowerDetection detection : allDetections) {
                String flowerName = detection.flowerClass == null
                        ? ""
                        : detection.flowerClass.toLowerCase(Locale.ROOT);

                if (flowerName.contains(cleanQuery)) {
                    filteredDetections.add(detection);
                }
            }

            binding.tvSearchResultInfo.setText(
                    "Résultats pour \"" + query + "\" : " + filteredDetections.size()
            );
        }

        historyAdapter.notifyDataSetChanged();

        if (filteredDetections.isEmpty()) {
            binding.tvEmptyHistorique.setVisibility(View.VISIBLE);
            binding.rvHistorique.setVisibility(View.GONE);
        } else {
            binding.tvEmptyHistorique.setVisibility(View.GONE);
            binding.rvHistorique.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (detectionsListener != null) {
            detectionsListener.remove();
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        binding = null;
    }
}