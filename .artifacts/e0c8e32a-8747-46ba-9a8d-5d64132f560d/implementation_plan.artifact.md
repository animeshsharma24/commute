# Commute Tracker Implementation Plan

This plan outlines the development of a complete native Android application for tracking commutes based on Wi-Fi connectivity.

## Proposed Changes

### [Core Configuration]
#### [MODIFY] [libs.versions.toml](file:///C:/Users/anime/AndroidStudioProjects/CommuteTracker/gradle/libs.versions.toml)
Add versions and libraries for Room, DataStore, Navigation, and KSP.
#### [MODIFY] [build.gradle.kts (project)](file:///C:/Users/anime/AndroidStudioProjects/CommuteTracker/build.gradle.kts)
Add KSP plugin.
#### [MODIFY] [build.gradle.kts (app)](file:///C:/Users/anime/AndroidStudioProjects/CommuteTracker/app/build.gradle.kts)
Apply KSP plugin and add dependencies.
#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/anime/AndroidStudioProjects/CommuteTracker/app/src/main/AndroidManifest.xml)
Add required permissions: `ACCESS_FINE_LOCATION`, `ACCESS_WIFI_STATE`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`.

---

### [Data Layer]
#### [NEW] [CommuteRecord.kt](file:///C:/Users/anime/AndroidStudioProjects/CommuteTracker/app/src/main/java/com/animesh/commutetracker/data/model/CommuteRecord.kt)
Room Entity for commute records.
#### [NEW] [CommuteDao.kt](file:///C:/Users/anime/AndroidStudioProjects/CommuteTracker/app/src/main/java/com/animesh/commutetracker/data/database/CommuteDao.kt)
DAO for database operations.
#### [NEW] [AppDatabase.kt](file:///C:/Users/anime/AndroidStudioProjects/CommuteTracker/app/src/main/java/com/animesh/commutetracker/data/database/AppDatabase.kt)
Room database class.
#### [NEW] [PreferenceManager.kt](file:///C:/Users/anime/AndroidStudioProjects/CommuteTracker/app/src/main/java/com/animesh/commutetracker/data/repository/PreferenceManager.kt)
DataStore for settings (SSIDs, tracking toggle) and state machine persistence.
#### [NEW] [CommuteRepository.kt](file:///C:/Users/anime/AndroidStudioProjects/CommuteTracker/app/src/main/java/com/animesh/commutetracker/data/repository/CommuteRepository.kt)
Repository to handle business logic and data access.

---

### [Background Service]
#### [NEW] [CommuteTrackerService.kt](file:///C:/Users/anime/AndroidStudioProjects/CommuteTracker/app/src/main/java/com/animesh/commutetracker/service/CommuteTrackerService.kt)
Foreground service that monitors Wi-Fi connectivity changes using `ConnectivityManager.NetworkCallback`.
Implements the state machine logic:
- `IDLE` -> `HOME_TO_OFFICE_PENDING` (Home Disconnected)
- `HOME_TO_OFFICE_PENDING` -> `Confirmed` (Office Connected) or `Cancelled` (4h timeout)
- Vice versa for Office to Home.

---

### [UI Layer]
#### [NEW] [NavGraph.kt](file:///C:/Users/anime/AndroidStudioProjects/CommuteTracker/app/src/main/java/com/animesh/commutetracker/ui/navigation/NavGraph.kt)
Setup Navigation for the app.
#### [NEW] ViewModels
- `MainViewModel.kt`
- `HistoryViewModel.kt`
- `StatsViewModel.kt`
- `SettingsViewModel.kt`
#### [NEW] Screens
- `MainScreen.kt`: Status and today's history.
- `HistoryScreen.kt`: Grouped history with edit functionality.
- `StatsScreen.kt`: Statistical overview.
- `SettingsScreen.kt`: SSID config, clear history, CSV export.
- `SetupScreen.kt`: First-run configuration.
#### [NEW] Components
- `TransportDialog.kt`: "How did you travel?" dialog with cost input and suggestions.

---

### [Utilities]
#### [NEW] [CsvExporter.kt](file:///C:/Users/anime/AndroidStudioProjects/CommuteTracker/util/CsvExporter.kt)
Utility to generate CSV data and trigger the share/save intent.

---

## Verification Plan

### Automated Tests
- Build the project to ensure all dependencies and code are correct.
- Verification of Room migrations (if any, but starting with v1).

### Manual Verification
- Deploy to an emulator/device.
- Test settings configuration.
- Mock Wi-Fi transitions (if possible) or use logs to verify state machine transitions.
- Verify CSV export and file content.
- Check statistics calculations.
