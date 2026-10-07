package org.tensorflow.lite.examples.imageclassification.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.tensorflow.lite.examples.imageclassification.R;
import org.tensorflow.lite.examples.imageclassification.databinding.FragmentMapBinding;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 300;

    private FragmentMapBinding binding;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        checkLocationPermissionAndShowLocation();
    }

    private void checkLocationPermissionAndShowLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
            return;
        }

        showCurrentLocation();
    }

    private void showCurrentLocation() {
        if (googleMap == null) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        googleMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (binding == null) {
                        return;
                    }

                    if (location != null) {
                        moveMapToLocation(location);
                    } else {
                        binding.tvMapStatus.setText("Position non disponible. Activez le GPS puis réessayez.");
                        Toast.makeText(
                                requireContext(),
                                "Position GPS non disponible",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding != null) {
                        binding.tvMapStatus.setText("Erreur GPS : " + e.getMessage());
                    }

                    Toast.makeText(
                            requireContext(),
                            "Erreur GPS : " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void moveMapToLocation(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        LatLng currentPosition = new LatLng(latitude, longitude);

        googleMap.clear();

        googleMap.addMarker(
                new MarkerOptions()
                        .position(currentPosition)
                        .title("Ma position actuelle")
        );

        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(currentPosition, 16f)
        );

        binding.tvMapStatus.setText(
                "Latitude : " + latitude + " | Longitude : " + longitude
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showCurrentLocation();
            } else {
                Toast.makeText(
                        requireContext(),
                        "Permission GPS refusée",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}