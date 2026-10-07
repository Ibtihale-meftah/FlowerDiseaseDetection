package org.tensorflow.lite.examples.imageclassification;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.examples.imageclassification.firebase.FirebaseRepository;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerTextView;

    private FirebaseRepository firebaseRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseRepository = new FirebaseRepository();

        if (firebaseRepository.isUserLoggedIn()) {
            openMainActivity();
            return;
        }

        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.buttonLogin);
        registerTextView = findViewById(R.id.textViewRegister);

        loginButton.setOnClickListener(v -> loginUser());

        registerTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseRepository.loginWithEmail(email, password, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Connexion réussie", Toast.LENGTH_SHORT).show();
                openMainActivity();
            } else {
                Toast.makeText(this, "Erreur de connexion : " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}