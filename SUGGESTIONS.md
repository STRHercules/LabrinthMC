## Foundation follow-ups

- Use the repository-local wrapper in CI so every validation path runs the
  same pinned Gradle distribution.
- Add the Phase 0.2 package, registry, and generation boundaries after this
  minimal common entry point remains stable.
- Add a small automated metadata smoke check to catch mod ID, display-name, and
  dependency-range drift before dedicated-server startup.

## 0.1.1 Wrapper follow-up

- Consider adding a Gradle distribution checksum once the project standardizes
  its CI and release environment.
