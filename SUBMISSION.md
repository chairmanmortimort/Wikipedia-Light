# Submission: Wikipedia

Light Tool Library metadata - paste into the **Add Tool** form.

| Field | Value |
|---|---|
| **Title** | Wikipedia Light |
| **GitHub Repo** | https://github.com/chairmanmortimort/Wikipedia-Light |
| **Description** | A calm, text-first Wikipedia reader for Light Phone III. Search any article, reopen recent reads from a collapsible Recents list, and read clean plain-text extracts with related pages. No images, no feeds, no account. |
| **Tags** | Reading (provisional) |

## Checklist
- [x] Public repo created (https://github.com/chairmanmortimort/Wikipedia-Light)
- [x] >=1 screenshot added (required for approval) — docs/screenshots/, 4 real LP3 captures
- [x] Description <= 255 chars (217)
- [x] versionCode bumped (3)
- [x] Built with the latest light-sdk

## Build artifact
- **Release APK (minified + shrink + arm64-v8a):** `WikipediaLight-v0.3.0-release.apk`
  - Path: `Wikipedia Light/v0.3.0/WikipediaLight-v0.3.0-release.apk` (4,679,795 bytes)
  - Package: `com.thelightphone.wikipedia` / `serverPackage = com.lightos`
  - versionName = 0.3.0, versionCode = 3 (verified inside the APK via aapt2)
- **Debug APK:** `WikipediaLight-v0.3.0-debug.apk` (56,920,428 bytes)
  - Path: `Wikipedia Light/v0.3.0/WikipediaLight-v0.3.0-debug.apk`
- **Source:** `tool/src/main/kotlin/com/thelightphone/wikipedia/` (EntryPoint, Api, Models,
  ViewModel, Home, SearchResults, ArticleScreen, light-sdk plugin)
