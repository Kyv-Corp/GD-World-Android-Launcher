# GDWorld Android

[Скачать APK 0.1.0 — ARM64](https://github.com/Kyv-Corp/GD-World-Android-Launcher/releases/download/v000001/GDWorld-Android.apk) · [Релиз и SHA-256](https://github.com/Kyv-Corp/GD-World-Android-Launcher/releases/tag/v000001) · [Исходники](https://github.com/Kyv-Corp/GD-World-Android-Launcher)

Android-клиент GDWorld на базе [ZalithLauncher2Plus](https://github.com/Star1xr/ZalithLauncher2Plus), точная версия указана в `UPSTREAM.json`. Изменения GDWorld и оригинальная база распространяются под GPLv3; оригинальные авторские уведомления сохранены.

Главный экран: вход GDWorld, автоматическое получение сборки `GDWorldTest`, запуск с подключением к серверу, кабинет на сайте. Полный интерфейс Zalith доступен через «Расширенные возможности». MobileGlues выбран по умолчанию. Состав модов и версии Minecraft/NeoForge читаются из профиля сервера: отдельных правил для модов нет. Рендереры, Java и LWJGL устанавливает Android-база, настольные библиотеки не копируются.

## Сборка

NDK требует путь к исходникам без пробелов. Нужны JDK 21+, Android SDK Platform 37.0, Build Tools 37.0.0 и NDK 25.2.9519653. Укажите `sdk.dir` в локальном `local.properties` либо `ANDROID_HOME`.

```sh
./gradlew :ZalithLauncher:testDebugUnitTest --tests "com.movtery.zalithlauncher.gdworld.*" :ZalithLauncher:assembleDebug -Darch=arm64
```

Release требует собственную подпись через переменные `GDWORLD_KEYSTORE` (абсолютный путь), `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Исходные публичные ключи Zalith не используются. APK находится в `ZalithLauncher/build/outputs/apk/`.

## Подключение к Gravit

Gravit проверяет разрешённую Android-сборку перед обычной CLIENT-авторизацией. Серверная часть находится в `server/`; она не заменяет проверку пароля, 2FA, UUID, токена, whitelist и серверного входа. Сборки разрешаются по SHA-256 APK; это механизм регистрации версии клиента, а не аппаратная аттестация устройства.

Соберите модуль против библиотек действующего Gravit 5.7.14:

```sh
GRAVIT_CLASSPATH='/path/to/distribution/lib/*' bash server/build.sh
```

Установите JAR в каталог модулей LaunchServer и добавьте `GDWorldAndroid_module` в `loadModules` рабочего `modules.json`. Создайте `config/GDWorldAndroid/sha256.txt` в рабочем каталоге LaunchServer. Каждая строка — разрешённый SHA-256 APK в нижнем регистре. Перезапустите только Gravit после подключения модуля. Изменение списка хешей перезапуска не требует. Не отключайте штатную защиту Gravit ради Android.

GitHub Actions собирает тестовые APK. Используйте теги с шестизначным кодом сборки: `v000001` для `launcher_version_code=1`; так штатная проверка обновлений сопоставляет код APK с тегом. Релиз по тегу `v*` использует секреты `GDWORLD_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. До установки нового APK добавьте его хеш на сервер. Сохраняйте предыдущие разрешённые версии, пока они нужны игрокам.

## Состояние

Первый тестовый релиз **0.1.0**, тег `v000001`, опубликован 21 ноября 2026. APK подписан ключом GDWorld; его SHA-256 зарегистрирован в рабочем Gravit. Состав релиза: `GDWorld-Android.apk` и `SHA256SUMS`.

Первый выпуск собран локально. Полный прогон GitHub Actions остановлен по просьбе владельца ради публикации первого релиза; успешное выполнение всего CI ещё не подтверждено. Подробные результаты и ограничения находятся в [VALIDATION.md](VALIDATION.md). Работа произвольного модпака на конкретном GPU подтверждается только запуском на устройстве. Производительность отдельных модов не оптимизируется лаунчером.

Название, логотип и иллюстрации GDWorld принадлежат их владельцам; лицензия исходников не передаёт права на товарные обозначения и чужие изображения.

В репозиторий не входят приватная инфраструктура проекта, данные игроков, аккаунты, серверные ключи и ключ подписи приложения.
