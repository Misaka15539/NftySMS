# NftySMS (SMS-to-ntfy)

A lightweight, reliable Android application that automatically intercepts incoming SMS messages and forwards them to a specified [ntfy.sh](https://ntfy.sh/) topic via push notifications.

Perfect for monitoring SMS two-factor authentication (2FA) codes, missed delivery notifications, or automated system alerts across multiple devices.

## Key Features

*   **Robust Background Interception**: Uses Android **WorkManager** with **Expedited WorkRequests** to ensure reliable SMS relaying even when the app is in the background or the system is under memory pressure.
*   **Offline Support & Automatic Retries**: Automatically queues messages if internet is unavailable and relays them the moment connectivity is restored using WorkManager's persistent job scheduling.
*   **Real-time Activity Log**: Instant visual confirmation of relay status (Pending, Success, Failure) with a rolling **200-entry capacity** to keep storage usage low.
*   **Secure Credential Storage**: Uses **EncryptedSharedPreferences** (backed by Android Keystore) to store sensitive authentication tokens and passwords.
*   **Flexible Authentication**: Supports **Bearer Token** and **HTTP Basic Authentication** for private ntfy topics.
*   **SSL/TLS Customization**: Optional toggle to disable SSL certificate validation for connections to self-hosted ntfy instances with self-signed certificates (use with caution).
*   **Modern Native UI**: Fully built with **Jetpack Compose** and **Material Design 3**, supporting dynamic colors (Android 12+) and dark mode.
*   **Battery Efficient**: Leverages the system's native broadcast and job scheduling mechanisms to minimize battery impact.

## Architecture

The application follows a modern **MVVM + Repository** architecture with three layers:

1.  **Presentation Layer**: Jetpack Compose UI with Hilt-injected ViewModels.
2.  **Domain Layer**: Use cases that orchestrate business logic (Relay, Settings, Credentials).
3.  **Data Layer**:
    *   **LogRepository**: Room-backed storage for the activity log.
    *   **SettingsRepository**: DataStore (Preferences) for non-sensitive app settings.
    *   **CredentialRepository**: EncryptedSharedPreferences for secure auth storage.
    *   **NtfyRepository**: OkHttp-based client for posting to the ntfy API.

## Prerequisites

*   Android 8.0 (API 26) or higher.
*   A target [ntfy](https://ntfy.sh/) topic.
*   **MIUI/Xiaomi Devices**: Require manual enabling of "Auto-start" and "Get SMS while in background" permissions in App Info.

## Building from Source

1.  Clone the repository:
    ```bash
    git clone https://github.com/misaka15539/nfty-relay.git
    ```
2.  Open the project in **Android Studio**.
3.  Sync the Gradle files.
4.  Build and run the project on your emulator or physical device.

## Usage

1.  **Grant Permissions**: Grant **SMS Permissions** when prompted on first launch.
2.  **Configure Topic**: Navigate to **Settings** and enter your Ntfy Topic URL (e.g., `https://ntfy.sh/my-topic`).
3.  **Set Authentication**: If your topic is protected, select **Bearer Token** or **Basic Auth** and enter your credentials.
4.  **Security Options**: Toggle **SSL Validation** if using a self-hosted server with untrusted certificates.
5.  **Enable Relay**: Toggle the **SMS Relay** switch on the main screen.
6.  **Test Connection**: Use the test action in settings to verify your configuration.

## Privacy & Security

*   **No Tracking**: No analytics, no third-party SDKs, no ads.
*   **Encryption at Rest**: Sensitive credentials are encrypted on disk using AES-256-GCM via the Android Keystore.
*   **Zero Leakage**: Credentials never appear in logs, UI (except masked placeholders), or system logcat.
*   **Direct Communication**: Messages are sent directly from your device to your configured ntfy server.

## License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

*Note: This project is an independent client that interacts with the [ntfy](https://ntfy.sh) service via its public API. It is not affiliated with or endorsed by the ntfy project.*
