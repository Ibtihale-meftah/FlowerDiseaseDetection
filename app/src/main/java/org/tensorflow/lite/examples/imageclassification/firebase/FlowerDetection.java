package org.tensorflow.lite.examples.imageclassification.firebase;

public class FlowerDetection {

    public String id;
    public String flowerClass;
    public float confidence;
    public long inferenceTime;
    public long createdAt;
    public String note;
    public boolean isFavorite;
    public double latitude;
    public double longitude;
    public String qrCode;
    public String imageUrl;

    public FlowerDetection() {
    }

    public FlowerDetection(
            String flowerClass,
            float confidence,
            long inferenceTime,
            long createdAt,
            String note,
            boolean isFavorite,
            double latitude,
            double longitude,
            String qrCode,
            String imageUrl
    ) {
        this.flowerClass = flowerClass;
        this.confidence = confidence;
        this.inferenceTime = inferenceTime;
        this.createdAt = createdAt;
        this.note = note;
        this.isFavorite = isFavorite;
        this.latitude = latitude;
        this.longitude = longitude;
        this.qrCode = qrCode;
        this.imageUrl = imageUrl;
    }
}