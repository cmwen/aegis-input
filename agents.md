# AegisInput agent guide

Use the repository docs as the first stop for tracing product intent and implementation details:

- `README.md` for the current feature set, module boundaries, and release summary.
- `docs/ARCHITECTURE.md` for system design, threading, and component flow.
- `docs/SETUP.md` for local development, native-library setup, and day-to-day build usage.

When you need to trace specs, features, or overall project direction, start from those docs before changing code.

## Delivery rules

1. Prefer unit tests for local testing.
2. Keep the build green with the same validation path used in CI:

   ```bash
   gradle --stacktrace \
     :engine-rime:testDebugUnitTest \
     :ui-compose:testDebugUnitTest \
     :app:testDebugUnitTest \
     :app:assembleDebug
   ```

3. Push completed changes to GitHub.
4. After the validation build is green, create and push a new release tag in the format `v<major>.<minor>.<patch>` so the existing release workflow publishes a new build. Default to a patch bump unless the work clearly requires a minor or major version change.
