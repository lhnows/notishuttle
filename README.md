# NotiShuttle

Forward **every Android notification** — including SMS — to your own webhook, automatically and reliably, even after a reboot.

NotiShuttle is a tiny, open-source Android app built around the system
[`NotificationListenerService`](https://developer.android.com/reference/android/service/notification/NotificationListenerService).
Grant it notification access once and it silently streams every notification to any HTTP(S)
endpoint you control, as a JSON `POST`.

> **No cloud, no third-party server, no account.** Your notifications go directly from your
> phone to your webhook.

---

## Features

- 📨 **All notifications** — title, text, big text, sub text, ticker, category, channel,
  actions, timestamps. **SMS is included automatically**: incoming texts are captured as
  notifications from your SMS app, so no extra `READ_SMS` permission is required.
- 🔁 **Runs in the background forever** — once you grant notification access, Android keeps
  the listener bound. No app needs to stay open in the foreground.
- 🔌 **Survives reboot** — notification access is a persistent permission and Android
  re-binds the listener after every boot, so forwarding resumes with zero interaction.
- 🛡️ **Reliable delivery** — every notification is enqueued through
  [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) with
  exponential backoff. Delivery survives process death and reboot, and waits for network.
- 🎛️ **Filters** — forward all apps, an allow-list, or a block-list; ignore ongoing
  (persistent) notifications; ignore silent notifications; or filter by keyword.
- 🔐 **HMAC-SHA256 signing** — optionally sign each request so your endpoint can verify
  authenticity with a shared secret.
- 🧩 **Custom headers** — attach any extra HTTP headers (e.g. `Authorization: Bearer …`).
- 🍏 **Bark target** — push straight to [Bark](https://github.com/Finb/Bark) on your iPhone
  (`title`/`subtitle`/`body`), in addition to the generic JSON webhook.
- 🧪 **Test button** — send a sample payload to verify your webhook is reachable.
- 🔋 **Battery-friendly** — uses the system notification pipeline and WorkManager; no polling.

---

## How it works

```
App posts notification
        │
        ▼
NotificationRelayService  ── filters ──►  builds JSON payload
        │
        ▼
WorkManager (persistent queue + retry/backoff)
        │
        ▼
OkHttp  POST  ──►  your webhook
```

## Requirements

- Android 8.0 (API 26) or newer
- The device must grant **notification access** to NotiShuttle (one-time, in system settings)

## Build

```bash
# Requires JDK 17+ and the Android SDK.
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
Or open the project in Android Studio and press **Run**.

## Setup

1. Build and install the app.
2. Open NotiShuttle and tap **Grant notification access**.
3. Choose a target — **Generic webhook** or **Bark** — enter its URL (for Bark:
   `https://api.day.app/<your-key>`), and turn on **Enable forwarding**.
4. (Optional) tap **Send test webhook** to verify.
5. Done — notifications start streaming immediately. Nothing to restart after a reboot.

> **Battery optimization:** on some OEMs (Xiaomi, Huawei, Samsung, …) aggressive battery
> management can delay the listener. Tap **Disable battery optimization** in the app to
> exempt NotiShuttle.

## Webhook contract

Every notification is delivered as a single HTTP `POST` with a JSON body. See
[`docs/webhook.md`](docs/webhook.md) for the full schema, signature verification, and
ready-to-use example receivers (Python / Node.js).

Quick example payload:

```json
{
  "version": 1,
  "event": "notification",
  "id": 42,
  "key": "0|com.example|42|null|1000",
  "tag": null,
  "app": {
    "packageName": "com.example.app",
    "label": "Example",
    "category": "msg",
    "channelId": "messages"
  },
  "notification": {
    "title": "Alice",
    "text": "Are you free later?",
    "bigText": null,
    "subText": null,
    "tickerText": null,
    "isOngoing": false,
    "isClearable": true,
    "groupKey": null,
    "actions": ["Reply", "Mark as read"]
  },
  "time": { "postTime": 1700000000000, "when": 1700000000000 },
  "device": {
    "id": "9f6a…",
    "name": "Pixel 8",
    "model": "Pixel 8",
    "manufacturer": "Google",
    "androidVersion": 35
  }
}
```

## Privacy & security

- Notifications are sent **only** to the webhook URL you configure. There is no bundled
  analytics, telemetry, or third-party service.
- The device identifier is a random UUID generated on first launch, not the Android ID,
  IMEI, or any advertising ID.
- Use the optional shared secret to sign requests (`X-NotiShuttle-Signature`), and configure
  your endpoint to verify it before trusting the payload.
- Be mindful of what you forward: notifications may contain personal or sensitive content.
  Use an HTTPS endpoint and keep your webhook URL private.

## Limitations

- Notification content is limited to what apps expose via `NotificationListenerService`;
  some apps post empty or redacted notifications.
- Fields are truncated (2,000 chars each) to stay within WorkManager's per-item size limit.
- Android may briefly throttle notification delivery if the app is force-stopped; simply
  relaunch it once in that case.

## Roadmap

- [ ] Per-app custom webhook overrides
- [ ] MQTT / ntfy / gotify built-in targets
- [ ] Notification history & re-send from the app
- [ ] Chinese (zh-rCN) and other localizations

## License

[MIT](LICENSE)
