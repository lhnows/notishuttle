# Webhook contract

NotiShuttle sends one HTTP `POST` per notification.

## Request

| Aspect  | Value                                                        |
|---------|--------------------------------------------------------------|
| Method  | `POST`                                                       |
| Body    | JSON (see schema below)                                      |
| `Content-Type` | `application/json; charset=utf-8`                      |
| `User-Agent`   | `NotiShuttle/1.0`                                      |
| `X-NotiShuttle-Event` | `notification`                                       |
| `X-NotiShuttle-Signature` | `sha256=<hex>` (only when a secret is configured) |
| Custom  | Any headers you configured in the app                       |

## Payload schema

| Field | Type | Description |
|-------|------|-------------|
| `version` | int | Schema version (`1`) |
| `event` | string | Always `"notification"` |
| `id` | int | Notification id (unique per app) |
| `key` | string \| null | System notification key |
| `tag` | string \| null | Notification tag, if any |
| `app.packageName` | string | Package of the app that posted the notification |
| `app.label` | string \| null | Human-readable app name |
| `app.category` | string \| null | Notification category (e.g. `msg`, `call`, `email`) |
| `app.channelId` | string \| null | Notification channel id |
| `notification.title` | string \| null | Title (truncated to 2000 chars) |
| `notification.text` | string \| null | Body text (truncated) |
| `notification.bigText` | string \| null | Expanded (big) text, when present |
| `notification.subText` | string \| null | Sub-text |
| `notification.tickerText` | string \| null | Legacy ticker text |
| `notification.isOngoing` | bool | Ongoing (persistent) notification |
| `notification.isClearable` | bool | Whether the user can dismiss it |
| `notification.groupKey` | string \| null | Notification group key |
| `notification.actions` | string[] \| null | Titles of the notification action buttons |
| `time.postTime` | long | Epoch millis when the notification was posted |
| `time.when` | long | Epoch millis of the notification's own timestamp |
| `device.id` | string | Random UUID generated on first launch |
| `device.name` | string \| null | Device name (falls back to model) |
| `device.model` | string | Build model |
| `device.manufacturer` | string | Build manufacturer |
| `device.androidVersion` | int | API level (e.g. `35`) |

## Verifying the signature

When a secret is configured, the app signs the **exact raw request body** with HMAC-SHA256
using the secret as the key, and sends the hex digest in:

```
X-NotiShuttle-Signature: sha256=<hex>
```

Verify it on your server with a constant-time comparison before trusting the payload.

### Python (Flask)

```python
import hashlib
import hmac
from flask import Flask, request, jsonify

app = Flask(__name__)
SECRET = b"change-me"  # must match the app's secret

@app.post("/hook")
def hook():
    raw = request.get_data()
    expected = request.headers.get("X-NotiShuttle-Signature", "")
    if SECRET:
        digest = hmac.new(SECRET, raw, hashlib.sha256).hexdigest()
        if not hmac.compare_digest(f"sha256={digest}", expected):
            return jsonify(ok=False, error="bad signature"), 401
    payload = request.get_json(force=True)
    print(payload["app"]["packageName"], "->", payload["notification"]["title"])
    return jsonify(ok=True), 200
```

### Node.js (Express)

```js
const express = require("express");
const crypto = require("crypto");

const app = express();
const SECRET = "change-me"; // must match the app's secret

app.post(
  "/hook",
  express.raw({ type: "application/json" }),
  (req, res) => {
    if (SECRET) {
      const digest = crypto.createHmac("sha256", SECRET).update(req.body).digest("hex");
      const expected = req.headers["x-notishuttle-signature"];
      const a = Buffer.from(`sha256=${digest}`);
      const b = Buffer.from(expected || "");
      if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
        return res.status(401).json({ ok: false, error: "bad signature" });
      }
    }
    const payload = JSON.parse(req.body.toString("utf8"));
    console.log(payload.app.packageName, "->", payload.notification.title);
    res.json({ ok: true });
  }
);

app.listen(3000);
```

## Delivery & retry

- Each notification is a single WorkManager job, persisted on disk.
- If the request fails (non-2xx, timeout, or network error), WorkManager retries with
  **exponential backoff** (30s minimum) and waits for network connectivity.
- Jobs are preserved across reboots and resumed automatically.
- A `2xx` response is treated as success; anything else is retried. Respond `2xx` only after
  you have durably stored the notification to avoid losing data.
