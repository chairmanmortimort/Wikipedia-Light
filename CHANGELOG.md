# Changelog

## 0.3.0 — "Wikipedia Light"

- **Removed Random Article and On This Day** — the Light variant is search + recents only.
- **Recents are collapsed by default** — the home page shows only "Tap to search"
  (centered) and the word "Recents" (bottom-left); tapping "Recents" expands the
  recent-articles list beneath it, tapping again collapses it.
- **New About toggle: Show recents** — hides the Recents section entirely when off.

## 0.2.0

- Haptics now follow the phone's system haptic setting (previously always on).
- Removed the invert-colors option; the phone's system theme is left untouched.
- "Tap to search" is centered on the home page (matching the Dictionary app);
  Random Article / On This Day / Recents moved to the bottom-left corner.
- Full attribution on the About page (content license, data source, tool).

## 1.1.0

- Restructured for submission to the Light Tool Library: standalone repo with the Light SDK as a
  git submodule, `tool/` module layout, MIT license, README, and privacy disclosure.
- Now targets the real Light Phone III (`serverPackage = "com.lightos"`).
- Version bumped to 1.1.0 (versionCode 2).

## 1.0.0

- Initial release.
