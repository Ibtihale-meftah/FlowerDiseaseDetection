# Flower Disease Detection 🌸

### AI-Powered Android Application for Flower Health Classification

Flower Disease Detection is an Android mobile application designed to identify healthy and diseased flowers using artificial intelligence and real-time camera input.

The application integrates a custom TensorFlow Lite model for on-device image classification and Firebase for backend services and data management.

This project is adapted from the TensorFlow Lite Image Classification Android example and extended with flower health classification and Firebase integration.

## Features

- **AI-Powered Flower Classification:** Classify flowers as healthy or diseased using a custom TensorFlow Lite model.
- **Real-Time Camera Analysis:** Capture and analyze camera frames directly within the Android application.
- **On-Device Machine Learning:** Run model inference locally using TensorFlow Lite.
- **Firebase Integration:** Connect the mobile application to Firebase backend services.
- **User Authentication:** Login and registration interfaces integrated into the application.
- **Detection History:** Interface for accessing previous classification results.
- **User Profile:** Dedicated interface for user profile management.
- **Mobile User Interface:** Android interface with navigation, camera controls and classification results.

## Technologies Used

| Technology | Purpose |
|---|---|
| Java | Android application development |
| Android Studio | Development environment |
| TensorFlow Lite | On-device AI inference |
| Custom TFLite Model | Flower health classification |
| Camera | Real-time image acquisition |
| Firebase | Backend services and data management |
| Gradle | Build and dependency management |
| XML | Android user interface layouts |
| Git & GitHub | Version control and source code hosting |

## Project Structure

```text
FlowerDiseaseDetection/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   ├── model.tflite
│   │   │   └── labels.txt
│   │   ├── java/
│   │   │   └── org/tensorflow/lite/examples/imageclassification/
│   │   │       ├── firebase/
│   │   │       ├── fragments/
│   │   │       ├── adapters/
│   │   │       ├── utils/
│   │   │       ├── MainActivity.java
│   │   │       └── ImageClassifierHelper.java
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── gradle/
├── .gitignore
├── build.gradle
├── settings.gradle
└── README.md
```

## AI Model

The application includes a custom TensorFlow Lite model stored at:

`app/src/main/assets/model.tflite`

The model is intended to classify flower health based on image input.

The `labels.txt` file contains the classification labels used by the application.

Additional TensorFlow Lite example models, including MobileNet and EfficientNet Lite, are also included in the project.

## Firebase Integration

Firebase is used to support application backend functionality.

The Android project contains components for user accounts, profiles and flower detection data.

### Firebase Configuration

For security and project portability, the `google-services.json` file is excluded from version control.

To configure your own Firebase project:

1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Register an Android application using the package name configured in the Android project.
3. Download the `google-services.json` configuration file.
4. Place it inside the `app/` directory.
5. Enable and configure the Firebase services required by the application.
6. Configure appropriate authentication providers and database security rules.

Firebase configuration and access permissions must be configured before using backend-dependent features.

## Installation

### Prerequisites

- Android Studio
- Android SDK
- Java Development Kit compatible with the project's Gradle configuration
- An Android device with a camera
- A configured Firebase project

### Steps

**1. Clone the repository**

```bash
git clone https://github.com/Ibtihale-meftah/FlowerDiseaseDetection.git
```

**2. Open the project**

Open Android Studio and select the cloned `FlowerDiseaseDetection` directory.

**3. Configure Firebase**

Add your `google-services.json` file to the `app/` directory.

**4. Synchronize Gradle**

Allow Android Studio to download and synchronize the required dependencies.

**5. Run the application**

Connect an Android device, grant the requested camera permissions and run the application from Android Studio.

## Application Workflow

1. The user opens the Android application.
2. The application accesses the device camera after permission is granted.
3. Camera images are processed for AI classification.
4. The TensorFlow Lite model generates flower health predictions.
5. The application displays the classification results.
6. Firebase supports the application's connected features, including user and detection data management.

## Future Improvements

Potential improvements include:

- Expanding the model to recognize specific flower diseases.
- Training with a larger and more diverse image dataset.
- Improving classification accuracy under different lighting conditions.
- Providing treatment recommendations for detected diseases.
- Enhancing model evaluation and performance monitoring.

## Credits

This application is based on the TensorFlow Lite Image Classification Android example.

Original reference: [TensorFlow Lite Examples](https://github.com/tensorflow/examples)

The application has been adapted and extended to support flower health classification and Firebase integration.

The original project's license and attribution requirements should be respected.

## Disclaimer

AI predictions may be inaccurate and should not be treated as a definitive plant disease diagnosis. Model performance depends on training data, image quality and environmental conditions.