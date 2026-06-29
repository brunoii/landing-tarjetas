# Landing Tarjetas

Local-only foundation for a personal credit-card statement dashboard. Etapa 1 only sets up the Spring Boot project, static dark-mode shell, local H2 configuration, and privacy-safe folders; it does not parse PDFs, store statement data, or expose statement APIs.

## Quick path

1. Install Java 17 or newer and Maven 3.9+.
2. Run the app locally:

   ```bash
   mvn spring-boot:run
   ```

3. Open <http://127.0.0.1:8080>.
4. Optional health check: <http://127.0.0.1:8080/actuator/health>.

## Local development details

| Area | Decision |
|------|----------|
| Backend | Spring Boot with Maven and Java 17+. |
| Frontend | Static files served from `src/main/resources/static`. |
| Database | Local H2 file database under `./data/landing-tarjetas`. |
| Runtime folders | `data/`, `exports/`, and `logs/` are placeholders only; their real contents are ignored by Git. |
| Current scope | Etapa 1 foundation only. No domain model, repositories, PDF upload, PDF parsing, dashboard calculations, categories CRUD, or persistence schema. |

## Privacy rules

- Do not commit real PDFs, statements, exports, database files, logs, or local secrets.
- Do not use real personal data, real card numbers, or real financial values in examples.
- Keep this app local. Do not deploy or publish personal financial data.
- Treat everything under `data/`, `exports/`, and `logs/` as private local runtime material.

## Verification

Run:

```bash
mvn test
mvn package
```

The package command creates build output under `target/`, which is ignored by Git.
