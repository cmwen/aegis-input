# GitHub Copilot repository instructions

This repository is an Android project with `:engine-rime`, `:ui-compose`, and `:app` modules.

## Build and validation

- Validate Android changes with the same Gradle tasks used in `.github/workflows/ci.yml`:

  ```bash
  gradle --stacktrace \
    :engine-rime:testDebugUnitTest \
    :ui-compose:testDebugUnitTest \
    :app:testDebugUnitTest \
    :app:assembleDebug
  ```

- Keep the build green. Treat task work as incomplete until these tasks pass.

## Push and release workflow

- Push completed changes to GitHub.
- The release automation already exists in `.github/workflows/release.yml`.
- Releases are created by pushing a tag that matches `v<major>.<minor>.<patch>`.
- After the validation build is green, create a new release tag and push it so GitHub Actions publishes the release.
- Default to a patch version bump unless the task clearly requires a minor or major version change.
- Do not invent a separate release path or skip the existing GitHub Actions workflow.
