# Contributing

Thanks for taking a look at this project. It's a small hobby app, so the
process is intentionally light.

## Getting set up

Open the project in Android Studio, or build from the command line with JDK 21
and the Android SDK. See the [README](README.md#building). No accounts or keys
are needed.

## Making a change

1. Fork the repo and create a branch off `main`.
2. Keep changes focused: one feature or fix per PR is easier to review.
3. Match the existing code style. CI runs `./gradlew lintDebug testDebugUnitTest
   assembleDebug`; make sure it passes before opening a PR.
4. If you change the database, bump its version in `AppDatabase` and add a
   migration, so existing attendance survives the update.
5. Open a PR describing what changed and why.

## Reporting bugs and suggesting features

Open a GitHub issue using the relevant template. Steps to reproduce and the
phone model and Android version are always appreciated for bugs.
