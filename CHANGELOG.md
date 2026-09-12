# Changelog

## [1.0.0] - unreleased

### Added
- Capture all notifications (title, text, big text, sub text, ticker, category, channel, actions, timestamps).
- Forward notifications to a configurable webhook as JSON over HTTP POST.
- Persistent WorkManager delivery queue with exponential backoff and network constraint.
- Automatic resume after reboot (listener re-bind + WorkManager).
- App filters: all / allow-list / block-list.
- Ignore ongoing and silent notifications; keyword filter.
- Optional HMAC-SHA256 request signing and custom HTTP headers.
- Test webhook button and battery-optimization exemption helper.
- Boot receiver with a "running" status notification.
