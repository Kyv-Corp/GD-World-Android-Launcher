# GDWorld Android validation

Date: 22 November 2026. Results below describe the version and time of each check; historical limits are not claims about later releases.

## Current status and the 0.1.5 regression report

The owner confirms that two affected users played on0.1.4 for over30 minutes without a crash, then began seeing the same native crash after updating to0.1.5. Treat0.1.4 as the reported working baseline; the presence of a similar upstream issue does not exclude a GDWorld regression.

The actual release sources are `890ca4b1b5a941d2fa9129ebb641955e369b8fe8` (0.1.4) and `94ad82191bd9de3f968377752e06f26d70c22317` (0.1.5). Their complete source diff changes only README/validation, the version properties and `AccountManageScreen.kt`. FlowRow becomes bounded Column/Row panels; the skin preview creation/disposal and account operations are retained. Launch arguments, authentication, native sources, dependencies and R8 configuration are unchanged.

Local APK SHA-256 values match the published GitHub asset digests. Comparing unpacked release entries establishes that all59 native libraries and all67 non-baseline-profile assets are byte-identical, including Java archives, MobileGlues, libpojavexec, LWJGL and Java agents. The decoded manifests differ only in versionCode/versionName. DEX files, baseline profiles and obfuscated service names differ after recompilation. This is not proof that compiled app behavior is identical. The account UI runs in the main Android process; the JVM activity/service uses `:game`. The pack is fetched from the server on each preparation, so equal embedded assets do not prove equal downloaded mods/configuration.

A control branch, `codex/control-014`, was prepared from the0.1.4 source with only installation version properties changed to code8 / `0.1.7-control014` (commit `57674aa`). The owner then paused mobile work. The local control build and CI45065309593 were stopped; no control APK was published or registered. Do not treat this branch as a fix or a completed comparison. Future production version codes must account for the reserved code8 if this control build is ever distributed.

## Full native report received from0.1.6

The supplied `hs_err_pid27262.log` and matching `latest_game.log` came from OnePlus CPH2769. Raw logs are not published: their command line includes a game access token and player details.

- ErrorFile saving to `files/logs/native` is now confirmed by a real Android crash. Runtime: Internal-21, OpenJDK21.0.1, ARM64, G1, Xms/Xmx2048M. The fault offset matches the previous log: `libjvm.so+0xe86c68`.
- Process lifetime:26.938 seconds. Signal:SIGSEGV/SEGV_MAPERR on `Service Thread`. Stack: `SymbolTable::do_concurrent_work → clean_dead_entries → ConcurrentHashTable::do_bulk_delete_locked_for`. Instructions/registers show a read at invalid address `0x3900000009`; the report does not establish who corrupted the pointer.
- Heap usage:527360KiB of2097152KiB (515/2048MiB); metaspace about156MiB; MemAvailable about1.77GiB. No OutOfMemoryError. This crash does not justify increasing Xmx, nor does it exclude native memory corruption or other memory problems.
- G1 concurrent marking begins at26.436 due to CodeCache GC Threshold. Classes unload at26.857–26.858, G1PauseRemark ends at26.915, symbol cleanup crashes at26.938. This motivates testing class unloading; it does not establish it as the root cause. The textures-signature warning concerns a skin; the fatal stack contains neither profile-key validation nor renderer frames.
- Upstream [issue1918](https://github.com/ZalithLauncher/ZalithLauncher2/issues/1918) describes this family of stacks on Internal-21 and25 without a confirmed fix. A matching fault location does not prove an identical cause.

A reversible diagnostic trial was proposed, but has not been performed: save existing JVM arguments, then add `-XX:-ClassUnloading -Xlog:gc=info,class+unload=info,symboltable=debug`. Temporarily replace an explicit `-XX:+ClassUnloading` if present; leave other settings unchanged. Play for20–30 minutes with several connections/world loads, then restore the original arguments. Collect a fresh latest_game and, after a crash, hs_err; after a successful trial, record duration and latest_game to verify the applied arguments. One successful run is not a confirmed fix.

The argument path was checked: GameSettingsScreen/AllSettings.jvmArgs → GameLauncher.customArgs → Launcher.getJavaArgs/progressFinalUserArgs retains this flag. Nonempty per-version arguments replace global arguments; apply the trial there when appropriate. Host Java21.0.11 accepted the syntax and PrintFlagsFinal showed ClassUnloading=false; this does not test the Android21.0.1 port. Logging syntax is documented in the [Java21 manual](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html#enable-logging-with-the-jvm-unified-logging-framework).

ADB is unavailable on the affected OnePlus. Disabling unloading may increase native/metaspace usage, so this is a temporary diagnostic trial, not a default. Class unloading and symbol cleanup are different operations; the flag does not guarantee bypassing the failing path. No launch-code, Java, GC or server-setting change was made for this trial. Investigation is now paused; root cause and final fix remain unconfirmed.

## Changes in0.1.6: sharing diagnostic files

- Inspected the sole FileItemLayout caller, existing utils.file.shareFile, manifest and provider_paths. Share is available for ordinary files only, using ACTION_SEND, a content URI and FLAG_GRANT_READ_URI_PERMISSION. The system chooser owns recipient selection. Intent-preparation failures go through the existing error handler.
- generic_share already existed in English/Russian; Ukrainian, Belarusian and Polish overrides were added. No new dependencies or permissions.
- The built-in manager already roots at `.minecraft`; navigate to `versions/GDWorld-1.21.1-21.1.267`. The system FolderProvider is named GDWorld and exposes `android_data/files/.minecraft`. These are different navigation paths, not a missing game directory.
- After the owner could not find the old OnePlus report, a default ErrorFile was added under existing DIR_NATIVE_LOGS: `files/logs/native/hs_err_pid%p.log`. Explicit user ErrorFile settings are preserved. Host JVM21 accepted spaces and the PID placeholder; later, the supplied Android report confirmed actual creation.
- The owner manually installed0.1.6 and confirmed the system Share chooser opens. Sending to a recipient was not tested. Report retrieval does not fix the native JVM crash.

## Published0.1.6

- [0.1.6 / v000007](https://github.com/Kyv-Corp/GD-World-Android-Launcher/releases/tag/v000007), prerelease, versionCode7, source `789be827ba3f1a0345d1da9ae23d162750205e28`.
- APK:176,444,876 bytes; SHA-256 `7390f34ee16b343153c14420fabac5c913bf50f2f421413789eb0a0fcfd397d8`. apksigner verified v2 and the original certificate `2c3a1c48d5ce6763d089288d50f4ed5257ff7835de82ba6020c9038a827e51fb`. Package retained; ARM64/MobileGlues/libpojavexec/Java21 checked. Source matches build copy. Local artifact/log/signature/mapping: `dist/releases/0.1.6/`.
- Signed local build and [CI45060081441](https://github.com/Kyv-Corp/GD-World-Android-Launcher/actions/runs/45060081441) passed:34 JVM tests passed,3 manual external-network checks skipped. Uploaded APK size/hash and SHA256SUMS hash matched before publication. Authenticated API checks confirmed both assets and downloaded matching checksums. Anonymous download initially returned404 because the repository was private; the owner subsequently authorized making it public, and an anonymous ranged APK GET returned206.
- Exact SHA registered before publication, preserving all11 previous hashes. A live credential-free WSS check accepted the new SHA and rejected an unknown SHA. No service restart; game port25566 remained available.
- Share chooser opening is confirmed. Actual native-report creation is confirmed by hs_err_pid27262.log. No native-crash fix is claimed.

## Account-screen crash fix in0.1.5

- The user and a server join event at15:57:24.540 (server-log time) confirmed0.1.4 sign-in on Poco with no subsequent key error. ADB captures confirmed the full-width file manager, `65 B` formatting and compact credits with Best_Jaro/Endayt.
- Opening accounts caused FATAL EXCEPTION main: `Asking for intrinsic measurements of SubcomposeLayout layouts is not supported`. Exact0.1.4 R8 mapping identifies wg3 as FlowMeasurePolicy, called from the new FlowRow around a lazy panel. This is a0.1.4 layout regression, not OAuth failure.
- Version0.1.5 replaces FlowRow with Row/Column. Callers supply preview/list bounds without asking LazyColumn for intrinsic measurements. After manual installation of code6, the owner and an ADB capture confirmed readable full-width preview/buttons/list and account actions without that UI crash. Earlier passing JVM/CI tests had not detected this runtime UI defect.
- Final signed build and [CI45030648163](https://github.com/Kyv-Corp/GD-World-Android-Launcher/actions/runs/45030648163) passed:34 tests passed,3 manual network checks skipped. Original certificate and compiled Best_Jaro/Endayt credits checked. Exact SHA registered before publication. These checks do not establish game stability on other devices.

## Published0.1.5

- [0.1.5 / v000006](https://github.com/Kyv-Corp/GD-World-Android-Launcher/releases/tag/v000006), prerelease, code6, source `94ad82191bd9de3f968377752e06f26d70c22317`.
- APK:176,443,552 bytes; SHA-256 `ec53a3f20c74fd7cee5b733a052e671273ba7ea89fba03226f571e115eab12a9`. APKv2/original certificate checked; package retained. ARM64/MobileGlues/libpojavexec/Java21 and compiled credits checked; source matches build copy. Artifacts: `dist/releases/0.1.5/`.
- Local build and CI45030648163 passed:34 tests passed,3 skipped. Uploaded APK/checksum hashes and size matched before publication; the recorded download check returnedHTTP200.
- Registration preserved all10 earlier hashes. Credential-free WSS accepted the new SHA and rejected unknown; no game-server restart; port25566 available. Poco account opening passed manual/ADB verification with code6 installed. File manager, credits and sign-in were checked in0.1.4 and retained. Other devices, nested-screen landscape and account import/export were not separately tested. Later reports of native game crashes are recorded above; preview status remains.

## Changes in0.1.4: nested screens and authentication

- ADB confirmed `Invalid signature for profile public key`; the server saw a1-byte signature instead of384 bytes. This matches AA== in authlib-injector's [ProfileKeyFilter](https://github.com/yushijinhun/authlib-injector/blob/develop/src/main/java/moe/yushi/authlibinjector/httpd/ProfileKeyFilter.java). The GDWorld bridge declares feature.enable_profile_key; POST `/minecraftservices/player/certificates` returns204 for the expected Bearer and401 otherwise. Phone authlib6.0.54 bytecode confirmed that an empty successful response becomes null. Server online-mode/enforce-secure-profile were unchanged.
- Gravit AuthLimiter (3/8000ms) returned auth.trylater after repeated full authentication. Restore replaces repeated auth; refresh runs only after auth.expiretoken. A GdSession monitor serializes read/restore/write; response UUID is checked and rotated refresh saved even if a later request fails. Limiter messages support five languages.
- Approved Android-SHA registration sets CLIENT like desktop LauncherResponse without assigning isAuth or UUID. Isolated checks using Gravit5.7.14 libraries reject absent/unknown/malformed hashes and post-auth registration. The module was updated and only Gravit restarted; game port25566 remained available.
- JVM suite:37 tests,34 passed,3 manual network checks skipped. Five new cases cover repeated restore without auth/refresh, refresh after expiry, no retry after token/network errors, UUID-change refusal and saving rotated refresh after interruption. org.json is a JVM-test dependency only, replacing Android stubs.
- Narrow file-manager sidebar overlays content; commands wrap. Account panels stack vertically with actions below names. File-size formatting passes resource arguments. About uses a compact standard dialog with correct upstream names; Best_Jaro/Endayt added in five languages.
- User/server confirmed0.1.4 join; ADB checked file manager and credits. The account crash found during physical QA was fixed in0.1.5. No automated production account was created for these checks; fixture replies are not presented as live authentication.

## Published0.1.4

- [0.1.4 / v000005](https://github.com/Kyv-Corp/GD-World-Android-Launcher/releases/tag/v000005), prerelease, code5, source `890ca4b1b5a941d2fa9129ebb641955e369b8fe8`.
- APK:176,443,724 bytes; SHA-256 `8d39271fc4ba68b361ac22e0f0f63ca18b9a1ea79b0443ffebd31ca87e0b7d6d`. APKv2/original certificate and package net.gdworld.launcher retained. ARM64-only/MobileGlues/libpojavexec/Java21 and compiled credits checked. Source matches build copy; local `testDebugUnitTest assembleRelease` passed.
- 34 tests passed,3 skipped. [CI45028956968](https://github.com/Kyv-Corp/GD-World-Android-Launcher/actions/runs/45028956968) passed the full JVM suite and ARM64 Debug build. Real authlib6.0.54 was tested against a synthetic loopback HTTP service:204 means no certificate;401 is rejected.
- Uploaded APK size/hash and SHA256SUMS hash matched; recorded direct downloadHTTP200. Local artifact/signature/log/mapping: `dist/releases/0.1.4/`.
- Exact APK registered before publication, preserving all9 earlier hashes. Credential-free live build registration accepted the new hash and rejected unknown. Game service was not restarted.
- Physical QA confirmed sign-in, file manager and credits, but found the account-screen crash subsequently fixed in0.1.5. No USB installation.

## Changes in0.1.3: advanced interface

- ADB reproduced the original issue on Poco X3 Pro (`M2102J20SG`, API31):1080×2400 screen,440dpi,392dp window,91px top DisplayCutout inset.
- Advanced UI uses safeDrawingPadding/imePadding. Narrow headers split title and scrolling actions; titles respect available width. Wide actions use at most60% width, retaining navigation space. Touch targets are at least48dp high.
- Advanced-home back returns to GDWorld; nested screens use the normal back dispatcher. Home navigation remains; the back label supports five languages.
- Below600dp, advanced home uses LazyColumn with account/launch, shortcuts and bounded full-width cards. Configured homepage content remains; nested scrolling is limited to400dp to avoid infinite LazyColumn constraints.
- Manual update verified0.1.3/code4. ADB captures before/after scrolling showed readable cards/log/changelog and header below camera. Back bounds `[22,91]–[154,223]` are below the cutout and at least48dp. Owner confirmed back/re-entry/scrolling. MIUI blocked INJECT_EVENTS; taps were manual, not automated. No USB installation.

## Changes in0.1.2

- JvmService scheduled the exit-result UDP send in a coroutine, then ZLNativeInvoker.jvmExit killed the process; the receiver also bound after service launch. Either race could lose the sole result and wait indefinitely.
- JvmExitSignal binds synchronously before JVM startup and sends synchronously before child exit. No global stale receiveMsg or unknown-code-as-success fallback. Cancellation has a bounded wait; finally closes the socket. Timeout becomes a normal installation error so TaskFlowExecutor does not swallow it as user cancellation.
- Installer Java follows Minecraft javaVersion.majorVersion and a compatible bundled runtime:Java21 immediately for1.21.1; retry progression8→17→21→25. Processor index and actual runtime replace empty `Installing in the foreground`; progress advances after output verification.
- Landscape launch/actions and login/progress panels scroll independently. Preparation state lives above orientation branches.
- Full local suite:32 checks,29 passed,3 manual network checks skipped. New cases include5 real loopback UDP tests (fast exit, no stale success, malformed packet/following longer code, port release, occupied port) and4 metadata-based Java-selection tests. Kotlin/Java compiled. This is not a physical NeoForge installation/gameplay test; landscape with keyboard remained unverified.

## Published0.1.3

- [0.1.3 / v000004](https://github.com/Kyv-Corp/GD-World-Android-Launcher/releases/tag/v000004), prerelease, code4, source `9eb691b505db19e70877c1be9100e346b8876a67`.
- APK:176,438,900 bytes; SHA-256 `9820465794ec3e01c344da93032bd0e35be00fc998a35071349ba67b2aabaec0`. Artifacts/log/signature/R8 mapping: `dist/releases/0.1.3/`.
- `testDebugUnitTest assembleRelease` and [CI45024591571](https://github.com/Kyv-Corp/GD-World-Android-Launcher/actions/runs/45024591571) passed:29 tests passed,3 skipped; ARM64 Debug built. Local source matched the published commit.
- APKv2/original certificate checked; net.gdworld.launcher retained, version incremented. ARM64-only libraries/MobileGlues/libpojavexec/Java21 checked. GitHub APK/checksum digests and size matched before publication; recorded downloadHTTP200.
- Exact hash registered before publication, preserving8 earlier hashes. Live WSS accepted0.1.3 and rejected unknown; no service restart; port25566 available.
- Original issue reproduced on Poco0.1.2/code3;0.1.3/code4 upper/lower cards and camera-safe navigation checked. Owner confirmed back/scroll. No USB installation. New landscape, keyboard and gameplay were not separately checked; preview status retained.

## Published0.1.2

- [0.1.2 / v000003](https://github.com/Kyv-Corp/GD-World-Android-Launcher/releases/tag/v000003), prerelease, code3, source `c4282369e8b8377cbfa5b0d1a02c5147492780c4`.
- APK:176,436,616 bytes, ARM64/net.gdworld.launcher; SHA-256 `b267c1fc3c4ca2bbd0feddc1b930de22f246400679b2d1eb52453e1099843bf6`. Artifacts/log/signature/R8 mapping: `dist/releases/0.1.2/`.
- Local `testDebugUnitTest assembleRelease` and [CI45021276521](https://github.com/Kyv-Corp/GD-World-Android-Launcher/actions/runs/45021276521) passed:29 tests passed,3 skipped; ARM64 Debug built. Source matches the published commit.
- APKv2/original certificate retained, package retained/versionCode incremented. ARM64-only libraries/MobileGlues/libpojavexec/Java21/version tables checked. APK and SHA256SUMS digests/size matched GitHub; recorded downloadHTTP200.
- Registration preserved7 earlier hashes. Live WebSocket accepted the new APK and rejected unknown; no service restart; game port25566 available.
- Signed locally. The0.1.1 tag workflow built/signed but publication failed because a release already existed. Workflow now skips existing releases to preserve verified/registered APKs.
- Physical phone, actual NeoForge installation, landscape/keyboard and gameplay were unverified at publication; preview release.

## Published0.1.1

- [0.1.1 / v000002](https://github.com/Kyv-Corp/GD-World-Android-Launcher/releases/tag/v000002), prerelease, code2, source `0ba189b5fe64ce439cf5629ad19f33be4b8b6282`.
- APK:176,432,316 bytes, ARM64/net.gdworld.launcher; SHA-256 `54e7f9c405bc361fc995b61ecd8558823df30d7411e674718308a3f9e0068fe5`. Local artifacts: `dist/releases/0.1.1/`.
- `testDebugUnitTest assembleRelease` and [CI44952264172](https://github.com/Kyv-Corp/GD-World-Android-Launcher/actions/runs/44952264172) passed:20 tests passed,3 manual network checks skipped. The earlier Maven download failure did not recur.
- APKv2/original0.1.0 certificate, retained package/increased code, ARM64-only libraries/MobileGlues/libpojavexec/Java21/version tables checked. GitHub APK/checksum digests and size matched before draft publication.
- Exact SHA registered before publication, preserving6 earlier hashes. Live WebSocket accepted it and rejected unknown; no restart; Minecraft listened on25566.
- Physical Android/GPU/gameplay were not checked; automatic tag-based signing/publication was not established by this local release.

## Changes in0.1.1

- Real current GameInstaller/file-check/download-byte progress, localized in five languages; indeterminate operations receive no fabricated percentage.
- Native Minecraft launch tasks remain visible on home, including managed-pack rechecks. Cancellation no longer clears busy state before cleanup.
- Home caps form width, uses a compact header and cutout/keyboard insets; MainActivity uses adjustResize.
- Update checking uses Kyv-Corp and published releases including prereleases, selecting the highest six-digit code with a nonempty APK. SHA256SUMS is not offered as an installer.
- Fixed numeric EGL error format strings, including an unsafe `%s`, and the VirGL thread-function return. No GPU/renderer tuning change.
- Six standalone JUnit PackFiles tests passed: path/symlink safety, cleanup/content rules, URL-safe SHA-1, real byte/chunk boundaries, cancellation, empty files and bad size/hash.
- Repaired nonportable file fixtures, JVM version-table access and invalid version examples. Manual external log-upload and third-party Minecraft ping tests explicitly ignored; CI runs the remaining full JVM suite.
- Related fixture checks passed:6 identity tests, network-isolated cabinet container, mod-sync/ModsSite UI, snapshot publisher,3 desktop mini-game rule sets and Android build registration. Production accounts/player data were unchanged during these checks.
- Desktop Publish launcher schedule run created21 November2026 at15:51:16UTC was confirmed, closing the earlier unobserved-schedule limit.
- WebsiteHTTP200, project containers/MCSManager active, Minecraft listening25566. These do not substitute for phone gameplay.
- Full local suite:23 checks,20 passed,3 skipped; all9 targeted GDWorld tests passed; Kotlin/Java compiled.

## Historical working Debug APK

- `dist/working/GDWorld-Android-debug.apk`,216,938,438 bytes, ARM64, net.gdworld.launcher.debug,0.1.0-debug/code1. Separate app, not an update to the production package/signature.
- assembleDebug passed after native fixes; APKv2 verified. MobileGlues/libpojavexec/Java21/version tables present; no other ABIs.
- SHA-256 `502c93e1fe6f33fa3b8f97a41ae6087cd6cbe9c7ad7c53766115e60149d2779d`. Transfer copy rehashed; checksum/JVM HTML report/logs stored alongside it.
- Live WebSocket rejected unknown and accepted the exact distributed Debug SHA; password sign-in and game rendering were not tested.
- Exact Debug SHA approved while preserving5 earlier hashes; prior allowlist privately backed up, no service restart. Published0.1.0 was not replaced; no new public release then. Phone/keyboard/rotation/gameplay were unverified.

## Historical0.1.0 checks

- Local Debug/Release builds passed; signed0.1.0/v000001 published.
- Four targeted GDWorld JVM tests passed, covering directory escape, absolute paths, symlink traversal, user-data preservation, desktop-component exclusion, symlink-safe temporary cleanup and Gravit URL-safe Base64 hashes.
- Server module compiled against Gravit5.7.14. Isolated checks reject missing/unknown SHA, accept approved SHA without player authorization and reject re-registration after authentication.
- ARM64 Debug built; APKv2 checked. MobileGlues/libpojavexec/Android Java21/MobileGlues license present; no other architectures.
- MobileGlues2.0.0 came from its official release after published-SHA verification. Version/hashes recorded in UPSTREAM.json.

## Published0.1.0

- [0.1.0 / v000001](https://github.com/Kyv-Corp/GD-World-Android-Launcher/releases/tag/v000001), published21 November2026 as a prerelease.
- GDWorld-Android.apk:178,928,592 bytes (about171MiB), ARM64/net.gdworld.launcher/code1. SHA-256 `52c255271447653f14cbf8d4f1a77c49ea8bca469c39467eb306fab950efd261`; GitHub digest matched local and APK was approved in Gravit.
- Local release APKv2 signature checked; certificate CN=GDWorld Android.
- First CI failed on removed SDK tools package; configuration fixed. Release Actions run stopped at owner's request, and verified local build published. Full CI was not considered passed for that first release.

## Historical validation limits and access

Before the final two GDWorld tests, the first full JVM run had16 tests:8 passed/8 failed. Two file tests depended on missing `F:\\Download\\geckolib-forge-1.21.8-5.2.2.jar`;6 version-comparison tests failed during GameVersionNumber initialization. They/base logic were unchanged at that historical point; CI then targeted GDWorld checks. Later releases repaired those fixtures as recorded above.

The Android module was installed in production Gravit with owner authorization. Historical live protocol checks covered unknown-APK rejection, approved-build registration, temporary CMS registration, wrong-password refusal, CLIENT login with the same UUID, profile/files, SHA-1-verified download, joinServer, wrong game-token refusal and OAuth refresh/restore. Temporary accounts were removed; the game server was not restarted. 2FA and in-game skin display were not separately checked. This historical record does not authorize new production test accounts.

At the initial release no Android device was connected, so touch UI, GPU rendering and gameplay were unverified. Building an APK alone does not prove Minecraft starts; subsequent device checks are dated/versioned above.

The signing key remains outside source control. With explicit owner authorization, protected signing secrets were configured in Actions. The initial release did not establish a completed automatic signing/publication run; later outcomes are recorded separately.
