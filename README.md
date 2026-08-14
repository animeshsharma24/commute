# Commute

A production-ready Android application to track daily commutes automatically using Wi-Fi SSIDs, GPS Geofencing, or Manual input.

## Features
- **Triple Tracking Modes**: Wi-Fi, Location (Geofence), and Manual.
- **Smart Cost Management**: Remembers recent costs and handles shuttle rides (₹0) automatically.
- **Statistics**: Detailed breakdown of travel time and expenses.
- **Privacy Focused**: All data remains on-device in a Room database.
- **CSV Export**: Export your travel history for external use.
- **Background Service**: Reliable monitoring using a foreground service with proper Android 14+ permissions.

## Installation
You can download the latest installable APK from the [Releases](https://github.com/animesharma2405/commute/releases) section of this repository.

## Development
Built using:
- Jetpack Compose (Material 3)
- Room Database
- DataStore Preferences
- Google Play Services Location & Geofencing
