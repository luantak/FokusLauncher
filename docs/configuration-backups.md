# Configuration backups

Open Settings, scroll to Data, and select **Import / export configuration**.

- **Export configuration** saves a JSON file through Android's document picker. You can choose local storage or a document provider such as a cloud drive.
- **Import configuration** opens a saved file, validates it, and asks before replacing the current configuration. Cancelling the picker or confirmation leaves your setup unchanged.

Backups contain launcher preferences, favorites and shortcuts, default clock/weather/calendar apps, gestures, weather display preferences, custom app ordering, profile names, app categories and their order, renamed and hidden apps, home notes, and the selected custom font file.

Wallpaper style and photo-wallpaper text/scrim settings are included. A black-wallpaper configuration applies black wallpaper when restored. Android owns the actual system wallpaper image, so a photo-wallpaper configuration keeps the current device's image. Set that image separately when moving to another device.

Android widget IDs, permissions, accessibility consent, onboarding state, cached GPS coordinates, usage counts, and active Pomodoro timer state are device-local and are not copied. Existing Android widgets and permissions remain unchanged. Apps, icon packs, system fonts, shortcuts, and user profiles must still be available on the destination device.

The file is not encrypted. It can contain private notes, app names, and other personal data. Store it privately and do not attach it to a public bug report. Diagnostic log export remains a separate action.

## File format

Version 1 uses `format: "fokus-launcher-configuration"` and `version: 1`. It contains a typed `preferences` object and arrays named `hidden`, `renamed`, `categories`, `definitions`, and `suppressed`. A selected imported font uses an optional base64 `font` field, restored only to the fixed app-private `fonts/active.ttf` path.

Import rejects unsupported versions, missing tables, unknown or incorrectly typed preferences, invalid custom font references, and malformed font data before asking for confirmation. Files are limited to 16 MiB, fonts to 8 MiB, and each database array to 10,000 rows. Restore replaces the configuration rather than merging it. Room changes use a transaction, and ordinary write failures restore the previous preference and font state. An OS kill or power loss during the multi-store write is not a transactional durability guarantee across Room, DataStore, and files.
