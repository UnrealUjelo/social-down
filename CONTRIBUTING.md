# Contributing to Social Down

Thanks for helping make Social Down safer, simpler, and more reliable.

## Before starting

1. Search existing issues and pull requests.
2. Open an issue for substantial behavior or architecture changes.
3. Keep work within the project's non-DRM, user-authorized download scope.

Changes that bypass DRM, authentication, paywalls, or access controls will not be accepted.

## Development setup

Use Android Studio, JDK 17, Android SDK Platform 37, and Build Tools 36.0.0. Build and verify with:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

On Windows, use `gradlew.bat`.

## Engineering guidelines

- Keep Compose screens dependent on immutable UI/domain models, not raw extractor objects.
- Put blocking extractor, file, database, and media work off the main thread.
- Preserve existing light/dark behavior and accessibility labels.
- Display only formats actually returned by the extractor.
- Sanitize all untrusted URLs, metadata, filenames, paths, and process arguments.
- Add focused tests for business logic and regressions.
- Avoid unrelated formatting or dependency changes in the same pull request.

## Pull requests

Keep commits focused and explain user-visible behavior, implementation choices, and verification. Update `CHANGELOG.md` when the change affects users. By contributing, you agree that your work is licensed under GPL-3.0.
