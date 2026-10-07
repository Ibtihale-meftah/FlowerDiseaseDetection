package org.tensorflow.lite.examples.imageclassification.firebase;

public class UserProfile {
        public String uid;
        public String name;
        public String email;
        public long createdAt;
        public long updatedAt;
        public int totalDetections;

        public UserProfile() {
        }

        public UserProfile(String uid, String name, String email, long createdAt, long updatedAt, int totalDetections) {
            this.uid = uid;
            this.name = name;
            this.email = email;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.totalDetections = totalDetections;
        }
    }

