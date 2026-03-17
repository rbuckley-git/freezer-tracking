# Freezer Tracker iOS App

This is a SwiftUI client for the Freezer Tracking API. It scans a QR code to capture the 8-digit reference, looks up the item, and either displays the item details or prompts the user to create a new one.

## Dependencies

External package dependencies:

- None currently. The iOS app does not declare Swift Package Manager, CocoaPods, or Carthage dependencies.

Apple frameworks used directly in the codebase:

- `SwiftUI` — application UI
- `Foundation` — networking and model utilities
- `AVFoundation` — QR code scanning via the camera

## Setup

1. Open Xcode and create a new iOS App project.
2. Replace the generated SwiftUI files with the contents of `apps/ios/FreezerTracker/`.
3. Ensure `Info.plist` includes the camera permission string:
   - `NSCameraUsageDescription`: "Camera access is required to scan QR codes."
4. Run on a real device for camera access.

## Configuration

The app uses a configurable API base URL. Default is `http://127.0.0.1:8080` (for local development).

From the app, tap **Settings** to change the API base URL for production.
