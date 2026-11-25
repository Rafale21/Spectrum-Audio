# License scope and third-party tooling

Spectrum Audio's application code, tests, original documentation and artwork in this repository are released under **GPL-3.0-only**, except for files with their own existing license notices.

The Gradle wrapper launch scripts (`android/gradlew`, `android/gradlew.bat`) and wrapper JAR are Gradle tooling under **Apache-2.0**. Their original notices are preserved; see LICENSES/Apache-2.0.txt. Upstream: https://github.com/gradle/gradle/tree/v8.9.0 . Gradle itself, Android SDK and the JDK are external build prerequisites, not included in this source distribution.

The application uses Android platform APIs and does not bundle third-party runtime SDKs. Test fixtures in android/checks/reference preserve the previous Android algorithm and the Spectrum Audio Windows 1.1 DSP for regression comparisons; they are part of this project's GPLv3 distribution. No Windows codec DLLs are included.

The source distribution includes the original raster artwork used by the application and its identity notes. A license to the code or artwork does not imply that an unofficial distribution is endorsed by No Ads Studio.
