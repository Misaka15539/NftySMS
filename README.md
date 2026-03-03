# Nfty-Relay

A lightweight Android application that automatically intercepts incoming SMS messages and forwards them to a specified [ntfy.sh](https://www.google.com/search?q=https://ntfy.sh/) topic via push notifications.

Perfect for users who need to monitor SMS two-factor authentication (2FA) codes, missed delivery notifications, or automated system alerts across multiple devices without keeping their primary phone nearby.

## Features

* **Real-time Forwarding:** Forwards SMS messages to your Ntfy topic the moment they arrive.
* **Custom Server Support:** Works with the public `ntfy.sh` server or your own self-hosted Ntfy instance.
* **Background Service:** Runs reliably in the background to ensure no messages are missed.
* **Privacy Focused:** No analytics, no third-party tracking, and messages are only sent directly to your configured Ntfy server.
* **Battery Efficient:** Uses Android's native SMS broadcast receiver, consuming minimal battery life.

## Prerequisites

* Android 8.0 (API 26) or higher.
* A target [ntfy](https://www.google.com/search?q=https://ntfy.sh/) topic (e.g., `ntfy.sh/my_secret_sms_topic`).

## Building from Source

1. Clone the repository:
```bash
git clone https://github.com/misaka15539/nfty-relay.git

```


2. Open the project in **Android Studio**.
3. Sync the Gradle files.
4. Build and run the project on your emulator or physical device.
