# Build Spectrum Audio 1.21 / Compiler

Application ID: `studio.noads.spectrumaudio` · versionCode **22** · versionName **1.21** · minimum Android **8 / API 26** · target/compile SDK **35**.

## Android Studio or Gradle

Open the **android/** directory as the Android Studio project. Install Android SDK Platform 35 and accept SDK licenses. The project uses Android Gradle Plugin 8.7.3, Gradle wrapper 8.9 and Java 17 source compatibility. Use a compatible JDK (17 or 21). Initial Gradle setup downloads its tools and needs network access.

From android/, on Windows:

```powershell
.\gradlew.bat assembleDebug
```

On Linux/macOS:

```sh
sh gradlew assembleDebug
```

The generated debug APK is under `app/build/outputs/apk/debug/`. Gradle generates a local development signing key; the official signing key is not necessary to build or install your own variant. A variant cannot replace an official installation directly unless its signing identity is compatible. Preserve recordings before uninstalling an app.

## Local SDK build (validated path)

With JDK 17+ and Android SDK Platform 35 / Build Tools 35.0.0 installed, from android/ in Bash:

```sh
export JAVA_HOME=/path/to/jdk
export ANDROID_HOME=/path/to/android-sdk
bash build-local.sh
```

Output: `manual-build/SpectrumAudio.apk`. Without release-signing environment variables, the script generates a local development key if needed. The standard development password in this script is not an official release secret. This build path was executed successfully for the GPL source package on JDK 21, with Java 17 output. The Android Studio/Gradle path was not executed in the packaging environment.

Official maintainers can set `SPECTRUM_SIGNING_KEY` and `SPECTRUM_SIGNING_PASSWORD_FILE` to private files outside the repository before invoking the local build. Alias: `spectrum-audio`. These files must never be committed. The official APK keeps the No Ads Studio application ID and signing key introduced in 1.20. Byte-identical reconstruction is not promised because signing identity and build environment affect the binary.

## Checks

From android/, with javac/java on PATH:

```sh
bash checks/run.sh
```

Python 3 is required for catalog checks; FFmpeg/ffprobe and a C compiler enable extra codec/reference checks. The optional device checks are documented in checks/device/README.md. Host tests do not replace tests on Android hardware.

## Français

Ouvrez **android/** dans Android Studio, installez le SDK 35 et utilisez un JDK compatible. La commande Windows ci-dessus produit un APK de développement avec une clé générée localement. Aucun fichier de clé officielle n’est nécessaire pour compiler une variante.

Le chemin Bash `build-local.sh` a été compilé et vérifié ici ; le parcours Android Studio/Gradle n’a pas été exécuté ici. Les sources correspondent à l’APK 1.21 : correction du curseur et suivi de la lecture. La définition du spectrogramme, l’identifiant Android et la clé de la 1.20 sont conservés.
