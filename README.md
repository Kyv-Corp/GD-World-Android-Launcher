# GDWorld Android

[Download APK 0.1.6 — ARM64](https://github.com/Kyv-Corp/GD-World-Android-Launcher/releases/download/v000007/GDWorld-Android.apk) · [Release and SHA-256](https://github.com/Kyv-Corp/GD-World-Android-Launcher/releases/tag/v000007) · [Source code](https://github.com/Kyv-Corp/GD-World-Android-Launcher) · [Desktop launcher](https://github.com/Kyv-Corp/GDWorld-Launcher)

GDWorld's Android client is based on [ZalithLauncher2Plus](https://github.com/Star1xr/ZalithLauncher2Plus). The exact upstream revision is recorded in `UPSTREAM.json`. GDWorld changes and the original base are distributed under GPLv3; upstream copyright notices are retained.

The home screen provides GDWorld sign-in, automatic preparation of the `GDWorldTest` pack, launch with a server connection, and access to the website account panel. The full Zalith interface is available through **Advanced features**. MobileGlues is the default renderer. Minecraft, NeoForge and mod versions come from the server profile; there are no special rules for individual mods. The Android base supplies renderers, Java and LWJGL; desktop native libraries are not copied.

## Current status

**0.1.6 is a preview release, not a confirmed fix for native game crashes.** Two users report that 0.1.4 ran for over 30 minutes, while identical native JVM crashes started after updating to 0.1.5. The investigation is paused at the owner's request. The 0.1.4 → 0.1.5 source diff changes only the account layout and app version; all 59 native libraries and 67 non-profile assets in the released APKs are byte-identical. This does not rule out an update regression. See [VALIDATION.md](VALIDATION.md) for evidence and limits.

Version 0.1.6 / `v000007` / versionCode 7 was published on 22 November 2026. It uses the original GDWorld signing identity; its exact APK SHA-256 was registered in Gravit before publication. Assets: `GDWorld-Android.apk` and `SHA256SUMS`. Both launcher repositories are now public.

## Changes in 0.1.6

The built-in file manager adds **⋮ → Share** for ordinary files, using Android's system chooser and the existing FileProvider with a read grant. The user selects the recipient. Folder sharing is not offered. The label is translated into five UI languages; no new dependencies or permissions were added. The owner confirmed that the chooser opens after updating.

The manager already opens inside `.minecraft`. Older JVM reports were expected under `versions → GDWorld-1.21.1-21.1.267 → hs_err_pid*.log`. The system document picker exposes that path as `GDWorld → android_data → files → .minecraft → versions`.

Starting with 0.1.6, future native reports default to `files/logs/native/hs_err_pid%p.log`; the JVM substitutes the PID, and an explicit user `-XX:ErrorFile` is preserved. In the system picker, use `GDWorld → android_data → files → logs → native`. A real report from the affected OnePlus confirmed that saving works. It shows a Service Thread crash while cleaning SymbolTable; the root cause and a fix remain unconfirmed. Install over 0.1.5 to retain application data.

## Account-screen fix in 0.1.5

The new 0.1.4 FlowRow requested intrinsic measurements from LazyColumn/BoxWithConstraints and crashed when opening accounts. Version 0.1.5 replaces it with bounded Column/Row panels. The owner and an ADB screenshot confirmed readable panels and buttons without that UI crash on Poco X3 Pro. Sign-in, the file manager and credits had already been checked in 0.1.4. These checks did not establish long-session game stability.

## Changes in 0.1.4

On narrow screens, the file manager uses a full-width list, an overlay sidebar and wrapping commands. Account management stacks vertically and places card actions below the name. The About dialog is compact, correctly credits MovTery/Star1xr, and includes Best_Jaro and Endayt in all five languages. File sizes no longer show raw format placeholders or duplicate values.

The GDWorld bridge returns no chat certificate (HTTP204), replacing authlib-injector's dummy signature. Server key-validation settings are unchanged. OAuth uses Gravit restore; refresh runs only after token expiry. Before distributing the APK, update the server module: registration of an approved build sets CLIENT type as on desktop, without authenticating a player. Unknown APKs and invalid tokens remain rejected. Install over 0.1.3 and fully restart the game.

## Changes in 0.1.3

Advanced features respect camera cutouts and system insets. Narrow screens use a two-row header and a scrolling list of full-width cards instead of side panels. A persistent back arrow returns from the advanced home to GDWorld; nested screens use normal back navigation. Actions no longer cover navigation.

The original issue was reproduced through ADB on Poco X3 Pro: 392dp window, Android12, 91px top cutout. After a manual update, upper/lower layout captures were checked; the owner confirmed back navigation and scrolling. No USB installation was performed. Install over 0.1.2 with the same signing identity.

## Changes in 0.1.2

Fixed the first NeoForge processor hanging: the JVM result is sent before the child Android process exits, the receiver binds before service launch, and results cannot leak between runs. Installer Java follows Minecraft metadata: Java21 for 1.21.1. Opaque JVM steps show the processor name, step and Java version; progress advances after completion and output verification.

The landscape home uses independently scrolling launch/actions and sign-in/progress columns. Rotation does not recreate pack preparation. Physical landscape validation was still pending at that release. Install over 0.1.1.

## Changes in 0.1.1

Preparation reports the current Minecraft/NeoForge installer task, file checking and actual downloaded pack bytes. Percentages apply to the named phase; unknown totals use an indeterminate indicator. SHA-1 is checked during download, and installation starts only after all downloads are verified. Native launch tasks remain visible after handoff. Cancellation keeps the operation busy until cleanup completes.

The portrait home has a compact header, a form capped at480dp, scrolling, cutout/keyboard insets and wrapping secondary buttons. New strings support all five languages. Update checking uses the current repository and includes published prereleases with a nonempty `GDWorld-Android.apk`. Version0.1.1 retains the0.1.0 signing identity for in-place updates.

## Building

The NDK requires a source path without spaces. Install JDK21+, Android SDK Platform37.0, Build Tools37.0.0 and NDK25.2.9519653. Set `sdk.dir` in a local `local.properties` or use `ANDROID_HOME`.

```sh
./gradlew :ZalithLauncher:testDebugUnitTest :ZalithLauncher:assembleDebug -Darch=arm64
```

Release signing uses your own `GDWORLD_KEYSTORE` (absolute path), `STORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` environment variables. The public Zalith signing keys are not used. APKs are written under `ZalithLauncher/build/outputs/apk/`.

## Gravit integration

Gravit checks an approved Android build before normal CLIENT authentication. The module in `server/` does not replace password, 2FA, UUID, token, whitelist or server-join checks. APK SHA-256 approval registers a client build; it is not hardware device attestation.

Build against the running Gravit5.7.14 distribution libraries:

```sh
GRAVIT_CLASSPATH='/path/to/distribution/lib/*' bash server/build.sh
```

Install the JAR in LaunchServer's modules directory, add `GDWorldAndroid_module` to `loadModules` in the active `modules.json`, and create `config/GDWorldAndroid/sha256.txt` under the LaunchServer working directory. Each line is an approved lowercase APK SHA-256. Restart only Gravit when installing the module; hash-list changes need no restart. Keep normal Gravit protections enabled.

GitHub Actions builds test APKs. Six-digit tags match version codes: `v000001` corresponds to `launcher_version_code=1`. Tagged `v*` releases use `GDWORLD_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` Actions secrets. Register the new APK hash before distribution, retaining previous versions while players need them.

The full CI JVM/ARM64 Debug run and local signed Release build passed. APKv2, the certificate match to0.1.0, architecture and uploaded hashes were verified. The0.1.1 tag workflow built and signed successfully but encountered an existing release during publication; the workflow now preserves previously verified assets. A successful build does not establish GPU compatibility or long-session stability. Detailed historical results are in [VALIDATION.md](VALIDATION.md).

GDWorld names, logos and artwork belong to their respective owners. The source license grants no trademark or third-party artwork rights. Private infrastructure, player data, accounts, server keys and the application signing key are excluded from this repository.
