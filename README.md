# MyAi - Android App with Google Sign-In

A modern Android application built with Kotlin that features Google Sign-In authentication and a welcome screen.

## Features

- 🔐 Google Sign-In authentication using Firebase Auth
- 🎨 Modern Material Design 3 UI
- 📱 Latest Android SDK (API 35) and Kotlin 2.1.0
- 🚀 Clean architecture with proper separation of concerns
- ✨ Smooth user experience with loading states and error handling

## Project Structure

```
app/
├── src/main/
│   ├── java/com/ratnesh/singh/myai/
│   │   ├── SignInActivity.kt      # Google Sign-In screen
│   │   └── WelcomeActivity.kt     # Welcome screen after sign-in
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_sign_in.xml
│   │   │   └── activity_welcome.xml
│   │   ├── values/
│   │   │   ├── strings.xml
│   │   │   ├── colors.xml
│   │   │   └── themes.xml
│   │   └── drawable/
│   │       └── circle_background.xml
│   └── AndroidManifest.xml
└── google-services.json           # Firebase configuration
```

## Setup Instructions

### 1. Firebase Configuration

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project named "MyAi" or use existing project
3. Add an Android app with package name: `com.ratnesh.singh.myai`
4. Download the `google-services.json` file and replace the existing one in the `app/` directory
5. Enable Authentication in Firebase Console
6. Enable Google Sign-In provider in Authentication > Sign-in method

### 2. Google Cloud Console Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your Firebase project
3. Go to APIs & Services > Credentials
4. Create OAuth 2.0 Client ID for Android:
   - Application type: Android
   - Package name: `com.ratnesh.singh.myai`
   - SHA-1 certificate fingerprint: Get from your debug keystore
5. Copy the Web Client ID and update it in `app/src/main/res/values/strings.xml`

### 3. Get SHA-1 Fingerprint

Run this command in your project root:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### 4. Build and Run

1. Open the project in Android Studio
2. Sync the project with Gradle files
3. Build and run on an emulator or physical device

## Dependencies

- **Android SDK**: API 35 (Android 15)
- **Kotlin**: 2.1.0
- **Gradle**: 8.7.2
- **Firebase Auth**: 23.0.0
- **Google Play Services Auth**: 21.2.0
- **Material Design**: 1.12.0

## App Flow

1. **Launch**: App opens to SignInActivity
2. **Sign In**: User taps "Sign in with Google" button
3. **Authentication**: Google Sign-In flow completes
4. **Welcome**: User is redirected to WelcomeActivity with their name and email
5. **Sign Out**: User can sign out and return to SignInActivity

## Key Features

### SignInActivity
- Google Sign-In button with proper styling
- Loading states during authentication
- Error handling with user feedback
- Automatic navigation to welcome screen on success

### WelcomeActivity
- Displays user's name and email
- Clean, modern UI with user avatar
- Sign out functionality
- Proper navigation back to sign-in screen

## Security Notes

- The app uses Firebase Authentication for secure user management
- Google Sign-In tokens are properly handled and validated
- User sessions are managed securely through Firebase Auth
- All network requests are made over HTTPS

## Troubleshooting

### Common Issues

1. **Google Sign-In not working**: 
   - Verify SHA-1 fingerprint is correct
   - Check that OAuth client is properly configured
   - Ensure `google-services.json` is in the correct location

2. **Build errors**:
   - Clean and rebuild the project
   - Ensure all dependencies are properly synced
   - Check that Google Services plugin is applied

3. **Authentication errors**:
   - Verify Firebase project configuration
   - Check that Google Sign-In is enabled in Firebase Console
   - Ensure internet connectivity

## Development

This project follows Android development best practices:
- Clean code architecture
- Proper error handling
- Material Design guidelines
- Accessibility considerations
- Modern Kotlin features

## License

This project is created for educational purposes.