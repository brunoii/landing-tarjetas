# Landing Tarjetas

Local-only foundation for a personal credit-card statement dashboard. Etapa 2 adds the backend model, local H2 persistence, validation rules, repositories, and non-upload REST endpoints. PDF upload/parsing, dashboard UI behavior, projection generation UI, and real statement samples are intentionally out of scope.

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
| Database | Local H2 file database under `./data/landing-tarjetas`; Hibernate uses `ddl-auto=update` for local development. |
| Runtime folders | `data/`, `exports/`, and `logs/` are placeholders only; their real contents are ignored by Git. |
| Current scope | Etapa 2 backend foundation only. No PDF upload/parsing endpoint and no real sample statement data. |

## API foundation

Available non-upload endpoints:

- `GET /api/statements`
- `GET /api/statements/{id}`
- `PUT /api/statements/{id}`
- `POST /api/statements/{id}/confirm`
- `DELETE /api/statements/{id}`
- `GET /api/transactions?month=&card=&category=&type=`
- `PUT /api/transactions/{id}`
- `DELETE /api/transactions/{id}`
- `GET /api/categories`
- `POST /api/categories`
- `PUT /api/categories/{id}`
- `DELETE /api/categories/{id}`
- `GET /api/dashboard/summary?month=`
- `GET /api/dashboard/categories?month=`

Use `month` as `YYYY-MM`. Currency fields stay separate: pesos and USD are stored and summed independently, with no conversion.

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
