package org.tensorflow.lite.examples.imageclassification.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.Slider;

import org.tensorflow.lite.examples.imageclassification.R;
import org.tensorflow.lite.examples.imageclassification.databinding.FragmentParametresBinding;

public class ParametresFragment extends Fragment {

    private FragmentParametresBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentParametresBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup toolbar
        setupToolbar();

        // Setup threshold slider
        setupThresholdSlider();

        // Setup max results controls
        setupMaxResultsControls();

        // Setup delegate toggle
        setupDelegateToggle();

        // Setup theme spinner
        setupThemeSpinner();

        // Setup save button
        setupSaveButton();
    }

    private void setupToolbar() {
        binding.toolbarParametres.setNavigationOnClickListener(v -> {
            Navigation.findNavController(requireView()).navigateUp();
        });
    }

    private void setupThresholdSlider() {
        binding.sliderThreshold.setValue(70);
        binding.sliderThreshold.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                binding.tvThresholdPct.setText((int) value + "%");
            }
        });
    }

    private void setupMaxResultsControls() {
        binding.maxResultsValue.setText("3");

        binding.maxResultsMinus.setOnClickListener(v -> {
            int current = Integer.parseInt(binding.maxResultsValue.getText().toString());
            if (current > 1) {
                current--;
                binding.maxResultsValue.setText(String.valueOf(current));
                Toast.makeText(requireContext(), "Max résultats: " + current, Toast.LENGTH_SHORT).show();
            }
        });

        binding.maxResultsPlus.setOnClickListener(v -> {
            int current = Integer.parseInt(binding.maxResultsValue.getText().toString());
            if (current < 10) {
                current++;
                binding.maxResultsValue.setText(String.valueOf(current));
                Toast.makeText(requireContext(), "Max résultats: " + current, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDelegateToggle() {
        binding.toggleDelegate.check(binding.btnCpu.getId());
        
        binding.toggleDelegate.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                String delegate = "";
                if (checkedId == R.id.btn_cpu) {
                    delegate = "CPU";
                } else if (checkedId == R.id.btn_gpu) {
                    delegate = "GPU";
                }
                Toast.makeText(requireContext(), "Délégué: " + delegate, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupThemeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.theme_spinner_titles,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTheme.setAdapter(adapter);

        binding.spinnerTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] themes = getResources().getStringArray(R.array.theme_spinner_titles);
                Toast.makeText(requireContext(), "Thème: " + themes[position], Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupSaveButton() {
        binding.btnSauvegarder.setOnClickListener(v -> {
            // Save settings (mock implementation)
            String threshold = binding.tvThresholdPct.getText().toString();
            String maxResults = binding.maxResultsValue.getText().toString();
            String delegate = "";
            
            int checkedId = binding.toggleDelegate.getCheckedButtonId();
            if (checkedId == R.id.btn_cpu) {
                delegate = "CPU";
            } else if (checkedId == R.id.btn_gpu) {
                delegate = "GPU";
            }

            Toast.makeText(requireContext(), 
                "Paramètres sauvegardés:\n" +
                "Seuil: " + threshold + "\n" +
                "Max résultats: " + maxResults + "\n" +
                "Délégué: " + delegate,
                Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
