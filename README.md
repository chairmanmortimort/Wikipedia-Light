# Wikipedia

A text-first Wikipedia reader for the Light Phone III. Search and read article summaries and full article text without images or scripts.

A community tool for the [Light Phone III](https://www.thelightphone.com/), built on the official
[light-sdk](https://github.com/lightphone/light-sdk) with Kotlin, Jetpack Compose, `LightScreen`
and `LightViewModel`, themed with `sdk:ui`.

## Layout

```
wikipedia/
├── light-sdk/            # git submodule -> upstream Light SDK (pinned commit)
├── tool/                 # the ONLY dev-owned module
│   ├── lighttool.toml    # tool id, label, version, declared permissions
│   ├── build.gradle.kts  # dependencies (project(":sdk:client") + light.sdk plugin)
│   └── src/main/**       # tool source
├── settings.gradle.kts   # grafts the submodule's SDK projects into this build
├── build.gradle.kts      # thin root: plugin classpath + ext build knobs
├── gradle.properties
└── gradlew, gradle/       # wrapper
```

Light's containerized builder clones this repo and extracts **only**
`tool/build.gradle.kts`, `tool/lighttool.toml`, and `tool/src/main/**`, then compiles them against
its own pinned, sandboxed copy of the SDK. It generates `AndroidManifest.xml` from `lighttool.toml`,
so this repo intentionally contains no hand-written manifest and never sets `applicationId`,
`versionCode`, `versionName`, or `namespace`.

## Building locally

Requires JDK 17 and an Android SDK. Then:

```
git submodule update --init --recursive
./gradlew :tool:assembleDebug
# -> tool/build/outputs/apk/debug/tool-debug.apk
```

The APK is signed with the shared Light dev keystore (from the submodule) for local sideloading.
Light's build service re-signs with its own key.

## Privacy

The Wikipedia tool contacts the public Wikipedia API (wikipedia.org) to search and fetch article text. Your queries and IP address reach Wikimedia under their normal request handling. There is no account, no analytics, and no data stored on any server we operate; nothing is kept on the device beyond the current session.

See PRIVACY.md for the network endpoints it contacts.

## Content & licensing

- Article text: Wikipedia, CC BY-SA 4.0 (Wikimedia contributors). Attribution is
  shown in the app's About screen.

## License

MIT, the same as upstream light-sdk. See [LICENSE](LICENSE).

The Light Phone, LightOS, and Light Phone III are names of The Light Phone; this project is not
affiliated with or endorsed by The Light Phone.
