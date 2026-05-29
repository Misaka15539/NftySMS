# Nfty-Relay (SMS-to-ntfy)

A lightweight, reliable Android application that automatically intercepts incoming SMS messages and forwards them to a specified [ntfy.sh](https://ntfy.sh/) topic via push notifications.

Perfect for monitoring SMS two-factor authentication (2FA) codes, missed delivery notifications, or automated system alerts across multiple devices.

## Key Features

*   **Robust Background Interception**: Uses Android **WorkManager** with **Expedited WorkRequests** to ensure reliable SMS relaying even when the app is in the background or the process is suspended by the system.
*   **Offline Support & Network Constraints**: Automatically queues messages if internet is unavailable and relays them the moment connectivity is restored.
*   **Intelligent Retries**: Built-in exponential backoff logic handles transient network or server failures.
*   **Real-time Activity Log**: Instant visual confirmation with a "Pending" status for intercepted messages waiting for internet.
*   **Custom Server Support**: Works with the public `ntfy.sh` server or any self-hosted Ntfy instance.
*   **Secure Credential Storage**: Uses **EncryptedSharedPreferences** to store authentication tokens and passwords safely.
*   **Battery Efficient**: Leverages the system's native broadcast and job scheduling mechanisms to minimize battery impact.

## Architecture

The application follows a modern **MVVM + Repository** architecture using:
*   **UI**: Jetpack Compose with Material 3.
*   **DI**: Hilt for dependency injection.
*   **Data**: Room for activity logging and DataStore for settings.
*   **Networking**: OkHttp 4 for secure and efficient HTTP POST requests.
*   **Background**: WorkManager for high-priority, persistent background tasks.

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

1.  Grant **SMS Permissions** when prompted.
2.  Navigate to **Settings** and configure your Ntfy Topic URL.
3.  Set up **Authentication** (Bearer Token or Basic Auth) if required by your Ntfy server.
4.  Toggle the **SMS Relay** switch on the main screen.
5.  All incoming SMS messages will now appear in the activity log and be forwarded to your Ntfy topic.

## Privacy & Security

*   **No Tracking**: No analytics or third-party SDKs.
*   **Encrypted Secrets**: Sensitive credentials never appear in logs and are encrypted on disk.
*   **Direct Communication**: Messages are sent directly from your device to your configured Ntfy server.
