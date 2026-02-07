# Scan Sentry Android - Play Store Readiness

## Build validation status

- `./gradlew lint` passes
- `./gradlew test` passes
- `./gradlew bundleRelease` passes
- Release bundle generated at:
  - `app/build/outputs/bundle/release/app-release.aab`

## Policy baseline

- Target API policy is satisfied in code:
  - `targetSdk = 36` in `app/build.gradle.kts`
- Google Play requires new apps and updates to target API 35+ (Android 15+) as of August 31, 2025.

## Code/config updates completed

- Removed non-functional UI placeholders and implemented import flow from:
  - gallery image (barcode decode)
  - clipboard text
- Scanner screen hardened:
  - CameraX opt-in lint fix
  - analyzer executor and scanner lifecycle cleanup
  - permission state handling cleanup
  - scan persistence flow cleanup
- Release build hardening:
  - resource shrinking + minification enabled for release
- Manifest and app identity polish:
  - launcher icon + round icon + monochrome icon wired
  - app label moved to string resource
  - deprecated flashlight permission removed
  - backup/data extraction behavior explicitly configured
- Gradle wrapper repaired/regenerated so `./gradlew` works again.

## Manual Play Console steps still required

These cannot be fully automated from source code:

1. Upload key and signing:
   - Use a production upload keystore (not a debug keystore) for first production upload.
   - Ensure Play App Signing is enabled in Play Console.
2. Store listing:
   - App title, short description, full description
   - Screenshots (phone required, tablet if supported)
   - 512x512 app icon and 1024x500 feature graphic
3. Policy forms:
   - Data safety
   - Content rating questionnaire
   - App access declaration (if any gated content)
   - Ads declaration
4. App details:
   - Privacy policy URL (required if applicable by data practices/features)
   - Contact email and support details
5. Testing and rollout:
   - Upload AAB to internal testing first
   - Resolve pre-launch report findings before production rollout

## Notes

- The project currently uses Android Gradle Plugin `8.5.1`, which warns that compile SDK 36 is newer than the plugin's tested level. Builds are passing with current configuration.
- If you want zero tooling warnings, the next step is upgrading AGP/Kotlin/KSP together to a mutually compatible newer set.
