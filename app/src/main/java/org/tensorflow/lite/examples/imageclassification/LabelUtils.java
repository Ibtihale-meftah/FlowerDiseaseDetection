package org.tensorflow.lite.examples.imageclassification;

import android.content.Context;

import org.tensorflow.lite.support.label.Category;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LabelUtils {

    private static final List<String> labels = new ArrayList<>();

    public static void loadLabels(Context context) {
        labels.clear();

        try {
            InputStream inputStream = context.getAssets().open("labels.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (!line.isEmpty()) {
                    String[] parts = line.split(" ", 2);

                    if (parts.length >= 2) {
                        labels.add(parts[1].trim());
                    } else {
                        labels.add(line);
                    }
                }
            }

            reader.close();
            inputStream.close();

        } catch (IOException e) {
            labels.clear();

            labels.add("Daisy");
            labels.add("Lily");
            labels.add("Orchid");
            labels.add("Rose");
            labels.add("Tulip");
        }
    }

    public static String getLabel(int index) {
        if (index >= 0 && index < labels.size()) {
            return labels.get(index);
        }

        return "Inconnu";
    }

    public static String getLabelFromCategory(Category category) {
        if (category == null) {
            return "Inconnu";
        }

        String modelLabel = category.getLabel();

        if (modelLabel == null || modelLabel.trim().isEmpty()) {
            return "Inconnu";
        }

        modelLabel = modelLabel.trim();

        // Cas 1 : le modèle retourne un index : "0", "1", "2", "3", "4"
        try {
            int index = Integer.parseInt(modelLabel);
            return getLabel(index);
        } catch (NumberFormatException ignored) {
            // Ce n'est pas un index numérique
        }

        // Cas 2 : le modèle retourne directement le nom
        // Exemple : daisy, lily, orchid, rose, tulip
        String normalized = modelLabel.toLowerCase();

        if (normalized.contains("daisy")) {
            return "Daisy";
        } else if (normalized.contains("lily")) {
            return "Lily";
        } else if (normalized.contains("orchid")) {
            return "Orchid";
        } else if (normalized.contains("rose")) {
            return "Rose";
        } else if (normalized.contains("tulip")) {
            return "Tulip";
        }

        return modelLabel;
    }
}