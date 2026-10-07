package org.tensorflow.lite.examples.imageclassification.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Base64;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private static final String PREFS_NAME = "history_prefs";
    private static final String HISTORY_KEY = "history_list";
    private static final int MAX_HISTORY_SIZE = 50;
    
    private SharedPreferences prefs;
    private Gson gson;
    
    public HistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }
    
    public void addHistoryItem(HistoryItem item) {
        List<HistoryItem> history = getHistory();
        history.add(0, item); // Add at the beginning
        
        // Keep only the last MAX_HISTORY_SIZE items
        if (history.size() > MAX_HISTORY_SIZE) {
            history = history.subList(0, MAX_HISTORY_SIZE);
        }
        
        saveHistory(history);
    }
    
    public List<HistoryItem> getHistory() {
        String json = prefs.getString(HISTORY_KEY, "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }
        
        Type listType = new TypeToken<ArrayList<HistoryItem>>(){}.getType();
        return gson.fromJson(json, listType);
    }
    
    public void clearHistory() {
        prefs.edit().remove(HISTORY_KEY).apply();
    }
    
    private void saveHistory(List<HistoryItem> history) {
        String json = gson.toJson(history);
        prefs.edit().putString(HISTORY_KEY, json).apply();
    }
    
    public static class HistoryItem {
        private String label;
        private String score;
        private String time;
        private String inferenceTime;
        private String timestamp;
        private String imagePath;

        public HistoryItem(String label, String score, String time, String inferenceTime, String timestamp, String imagePath) {
            this.label = label;
            this.score = score;
            this.time = time;
            this.inferenceTime = inferenceTime;
            this.timestamp = timestamp;
            this.imagePath = imagePath;
        }

        // Getters
        public String getLabel() { return label; }
        public String getScore() { return score; }
        public String getTime() { return time; }
        public String getInferenceTime() { return inferenceTime; }
        public String getTimestamp() { return timestamp; }
        public String getImagePath() { return imagePath; }

        // Setters for Gson
        public void setLabel(String label) { this.label = label; }
        public void setScore(String score) { this.score = score; }
        public void setTime(String time) { this.time = time; }
        public void setInferenceTime(String inferenceTime) { this.inferenceTime = inferenceTime; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    }
}
