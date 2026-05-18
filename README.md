(## Build and CI

Local build steps:

- Install Android SDK and set `sdk.dir` in a `local.properties` at the project root. Example content:

```
sdk.dir=/path/to/Android/Sdk
```

- Make sure the Gradle wrapper is executable and present (`./gradlew`), then run:

```bash
./gradlew clean assembleDebug
```

CI (GitHub Actions):

- This repository includes a workflow at `.github/workflows/android.yml` that installs the Android command-line tools, accepts licenses, installs `platforms;android-34` and `build-tools;34.0.0`, and runs `./gradlew :app:assembleDebug`. Pushing to `main` or creating a pull request will trigger the workflow.

If you want me to trigger a CI build or help add secrets, tell me which branch to push and I'll prepare a PR.
)

