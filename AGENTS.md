# F6S repository guidance

This branch uses the user's uploaded F6S_Store-main ZIP as its foundation. It is Java 17 / Spring Boot 4.1 / Thymeleaf / SQL Server, not the previous Vue/backend split.

- Code: `src/main/java/com/example/demodatn/`; templates: `src/main/resources/templates/`; assets: `src/main/resources/static/`.
- Preserve Vietnamese UTF-8 and Controller → Service → Repository structure. Source modifications are authorized by the current maintenance task. Verification-only restrictions in old uploaded reports are historical, not an active prohibition on edits.
- `./mvnw test` runs portable unit tests and `RefreshFlowTest` on isolated H2. The latter mocks SQL Server-specific metadata/range-lock queries; never describe it as native SQL Server verification.
- `./mvnw -Psqlserver-tests test` opts into old SQL Server integration tests. Use an isolated, prepared `shopbangiay_kt_*` database; never mutate a real store DB in tests.
- Browser: `npm ci --prefix ui-tests`, install Chromium, `npm test --prefix ui-tests` after Maven generates HTML in `target/refresh-ui/`. Node is a test tool, not needed to run this web app. Browser POSTs are intercepted contracts.
- Follow README.md and docs/RELEASE-20261103.md. Do not reset existing databases or overwrite uploads. Never commit local configuration, passwords, cookies, generated HTML with CSRF tokens, or traces.
- Use Java 17, targeted regressions and evidence scoped to checks actually run.
