package org.tensorflow.lite.examples.imageclassification;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import org.tensorflow.lite.examples.imageclassification.firebase.FirebaseRepository;

public class ProfileActivity extends AppCompatActivity {

    private EditText nameEditText;
    private TextView emailTextView;
    private TextView totalDetectionsTextView;
    private Button updateButton;
    private Button logoutButton;
    private Button deleteAccountButton;

    private FirebaseRepository firebaseRepository;
    private ListenerRegistration profileListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firebaseRepository = new FirebaseRepository();

        nameEditText = findViewById(R.id.editTextName);
        emailTextView = findViewById(R.id.textViewEmail);
        totalDetectionsTextView = findViewById(R.id.textViewTotalDetections);
        updateButton = findViewById(R.id.buttonUpdate);
        logoutButton = findViewById(R.id.buttonLogout);
        deleteAccountButton = findViewById(R.id.buttonDeleteAccount);

        listenProfile();

        updateButton.setOnClickListener(v -> {
            String newName = nameEditText.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "Le nom ne peut pas être vide", Toast.LENGTH_SHORT).show();
                return;
            }
            firebaseRepository.updateUserProfile(newName, task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Profil mis à jour", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
                }
            });
        });

        logoutButton.setOnClickListener(v -> {
            firebaseRepository.logout();
            navigateToLogin();
        });

        deleteAccountButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Supprimer le compte")
                    .setMessage("Êtes-vous sûr de vouloir supprimer définitivement votre compte et vos données ?")
                    .setPositiveButton("Supprimer", (dialog, which) -> {
                        firebaseRepository.deleteUserAccount(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Compte supprimé", Toast.LENGTH_SHORT).show();
                                navigateToLogin();
                            } else {
                                Toast.makeText(this, "Erreur : Reconnectez-vous avant de supprimer le compte", Toast.LENGTH_LONG).show();
                            }
                        });
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
        });
    }

    private void listenProfile() {
        profileListener = firebaseRepository.listenUserProfile((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) {
                return;
            }

            String name = snapshot.getString("name");
            String email = snapshot.getString("email");
            Long totalDetections = snapshot.getLong("totalDetections");

            nameEditText.setText(name != null ? name : "");
            emailTextView.setText(email != null ? email : "");
            totalDetectionsTextView.setText("Total détections : " + (totalDetections != null ? totalDetections : 0));
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (profileListener != null) {
            profileListener.remove();
        }
    }
}
