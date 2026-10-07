package org.tensorflow.lite.examples.imageclassification;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.tensorflow.lite.examples.imageclassification.databinding.ActivityMainBinding;
import org.tensorflow.lite.examples.imageclassification.firebase.FirebaseRepository;

public class MainActivity extends AppCompatActivity {

    private FirebaseRepository firebaseRepository;
    private ActivityMainBinding activityMainBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseRepository = new FirebaseRepository();

        if (!firebaseRepository.isUserLoggedIn()) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(activityMainBinding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            BottomNavigationView bottomNav = activityMainBinding.bottomNav;

            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.camera_fragment) {
                    navController.navigate(R.id.camera_fragment);
                    return true;
                }

                if (itemId == R.id.historique_fragment) {
                    navController.navigate(R.id.historique_fragment);
                    return true;
                }

                if (itemId == R.id.map_fragment) {
                    navController.navigate(R.id.map_fragment);
                    return true;
                }

                if (itemId == R.id.profile_menu) {
                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    return true;
                }

                return false;
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            finishAfterTransition();
        } else {
            super.onBackPressed();
        }
    }
}