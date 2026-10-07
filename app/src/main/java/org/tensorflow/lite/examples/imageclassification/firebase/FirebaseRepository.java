package org.tensorflow.lite.examples.imageclassification.firebase;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class FirebaseRepository {

    private static final String TAG = "FirebaseRepository";

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    public FirebaseRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public void registerWithEmail(
            String name,
            String email,
            String password,
            OnCompleteListener<AuthResult> listener
    ) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        String uid = auth.getCurrentUser().getUid();
                        createUserProfile(uid, name, email);
                    }

                    if (listener != null) {
                        listener.onComplete(task);
                    }
                });
    }

    public void loginWithEmail(
            String email,
            String password,
            OnCompleteListener<AuthResult> listener
    ) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(listener);
    }

    public void logout() {
        auth.signOut();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public void createUserProfile(String uid, String name, String email) {
        long now = System.currentTimeMillis();

        UserProfile profile = new UserProfile(
                uid,
                name,
                email,
                now,
                now,
                0
        );

        db.collection("users")
                .document(uid)
                .set(profile, SetOptions.merge());
    }

    public ListenerRegistration listenUserProfile(EventListener<DocumentSnapshot> listener) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return null;
        }

        return db.collection("users")
                .document(user.getUid())
                .addSnapshotListener(listener);
    }

    public void updateUserProfile(String newName, OnCompleteListener<Void> listener) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("updatedAt", System.currentTimeMillis());

        db.collection("users")
                .document(user.getUid())
                .update(updates)
                .addOnCompleteListener(listener);
    }

    public void deleteUserAccount(OnCompleteListener<Void> listener) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        String uid = user.getUid();

        db.collection("users")
                .document(uid)
                .delete()
                .addOnSuccessListener(unused -> user.delete().addOnCompleteListener(listener));
    }

    public void saveDetection(
            String flowerClass,
            float confidence,
            long inferenceTime,
            double lat,
            double lon,
            String qrCode
    ) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        String note = "";

        if (qrCode != null && !qrCode.isEmpty()) {
            note = "QR Code: " + qrCode;
        }

        Map<String, Object> detection = new HashMap<>();
        detection.put("flowerClass", flowerClass);
        detection.put("confidence", confidence);
        detection.put("inferenceTime", inferenceTime);
        detection.put("createdAt", System.currentTimeMillis());
        detection.put("note", note);
        detection.put("isFavorite", false);
        detection.put("latitude", lat);
        detection.put("longitude", lon);
        detection.put("qrCode", qrCode != null ? qrCode : "");
        detection.put("imageUrl", "");

        db.collection("users")
                .document(user.getUid())
                .collection("detections")
                .add(detection)
                .addOnSuccessListener(ref -> incrementTotalDetections(user.getUid()))
                .addOnFailureListener(e -> Log.e(TAG, "Erreur sauvegarde détection sans image", e));
    }

    public void uploadImageAndSaveDetection(
            String flowerClass,
            float confidence,
            long inferenceTime,
            double lat,
            double lon,
            String qrCode,
            Bitmap bitmap
    ) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null || bitmap == null) {
            return;
        }

        String uid = user.getUid();

        com.google.firebase.firestore.DocumentReference detectionRef =
                db.collection("users")
                        .document(uid)
                        .collection("detections")
                        .document();

        String detectionId = detectionRef.getId();

        StorageReference imageRef = storage.getReference()
                .child("users")
                .child(uid)
                .child("detections")
                .child(detectionId + ".jpg");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] imageData = baos.toByteArray();

        imageRef.putBytes(imageData)
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    String note = "";

                                    if (qrCode != null && !qrCode.isEmpty()) {
                                        note = "QR Code: " + qrCode;
                                    }

                                    Map<String, Object> detection = new HashMap<>();
                                    detection.put("flowerClass", flowerClass);
                                    detection.put("confidence", confidence);
                                    detection.put("inferenceTime", inferenceTime);
                                    detection.put("createdAt", System.currentTimeMillis());
                                    detection.put("note", note);
                                    detection.put("isFavorite", false);
                                    detection.put("latitude", lat);
                                    detection.put("longitude", lon);
                                    detection.put("qrCode", qrCode != null ? qrCode : "");
                                    detection.put("imageUrl", uri.toString());

                                    detectionRef.set(detection)
                                            .addOnSuccessListener(unused -> incrementTotalDetections(uid))
                                            .addOnFailureListener(e -> Log.e(TAG, "Erreur Firestore après upload image", e));
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Erreur récupération URL image", e);
                                    saveDetection(flowerClass, confidence, inferenceTime, lat, lon, qrCode);
                                })
                )
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur upload image Firebase Storage", e);

                    saveDetection(
                            flowerClass,
                            confidence,
                            inferenceTime,
                            lat,
                            lon,
                            qrCode
                    );
                });
    }

    private void incrementTotalDetections(String uid) {
        db.collection("users")
                .document(uid)
                .update(
                        "totalDetections", FieldValue.increment(1),
                        "updatedAt", System.currentTimeMillis()
                )
                .addOnFailureListener(e -> Log.e(TAG, "Erreur incrément totalDetections", e));
    }

    private void decrementTotalDetections(String uid) {
        db.collection("users")
                .document(uid)
                .update(
                        "totalDetections", FieldValue.increment(-1),
                        "updatedAt", System.currentTimeMillis()
                )
                .addOnFailureListener(e -> Log.e(TAG, "Erreur décrément totalDetections", e));
    }

    public ListenerRegistration listenDetections(EventListener<QuerySnapshot> listener) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return null;
        }

        return db.collection("users")
                .document(user.getUid())
                .collection("detections")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    public void deleteDetection(String detectionId) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null || detectionId == null || detectionId.isEmpty()) {
            return;
        }

        String uid = user.getUid();

        db.collection("users")
                .document(uid)
                .collection("detections")
                .document(detectionId)
                .delete()
                .addOnSuccessListener(unused -> decrementTotalDetections(uid))
                .addOnFailureListener(e -> Log.e(TAG, "Erreur suppression Firestore", e));

        StorageReference imageRef = storage.getReference()
                .child("users")
                .child(uid)
                .child("detections")
                .child(detectionId + ".jpg");

        imageRef.delete()
                .addOnFailureListener(e -> Log.e(TAG, "Erreur suppression image Storage", e));
    }

    public void updateDetectionFavorite(String detectionId, boolean isFavorite) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null || detectionId == null || detectionId.isEmpty()) {
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .collection("detections")
                .document(detectionId)
                .update("isFavorite", isFavorite)
                .addOnFailureListener(e -> Log.e(TAG, "Erreur mise à jour favori", e));
    }

    public void updateDetectionNote(String detectionId, String note) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null || detectionId == null || detectionId.isEmpty()) {
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .collection("detections")
                .document(detectionId)
                .update("note", note != null ? note : "")
                .addOnFailureListener(e -> Log.e(TAG, "Erreur mise à jour note", e));
    }
}