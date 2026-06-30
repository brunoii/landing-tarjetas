# Landing Tarjetas

Local-only foundation for a personal credit-card statement dashboard. Etapa 4A adds backend-only PDF upload, text extraction, statement parser detection, and safe draft creation. The browser upload/review screen, projection generation logic, and real statement samples are intentionally out of scope.

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
| Current scope | Etapa 4A backend upload and parser-detection infrastructure. No browser upload/review screen, projection generation, or real sample statement data. |

## UI scope

The static frontend lives under `src/main/resources/static` and is intentionally framework-free:

- `index.html` renders the dashboard shell, filters, transaction table, and category admin.
- `css/styles.css` contains the dark-mode layout.
- `js/*.js` modules call the existing local APIs and keep pesos and USD separate.

Open <http://127.0.0.1:8080> after `mvn spring-boot:run`. Empty states are expected until local statements or transactions exist.

## API foundation

Available endpoints:

- `GET /api/statements`
- `POST /api/statements/upload`
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

## Backend upload scope

`POST /api/statements/upload` accepts one or more multipart PDF files using the `files` field. The backend processes each file in memory with Apache PDFBox, computes a SHA-256 hash for source tracking, detects the first matching parser, and creates a `DRAFT` statement when a parser is detected.

Current parser detection covers minimal synthetic-safe patterns for:

- Santander Visa
- Santander American Express
- Naranja X

Parsing is intentionally conservative. Missing or low-confidence fields remain `null` with warnings instead of invented values. Draft statements do not affect public transaction listing or dashboard totals; only `CONFIRMED` statements are included there.

## Privacy rules

- Do not commit real PDFs, statements, exports, database files, logs, or local secrets.
- Do not use real personal data, real card numbers, or real financial values in examples.
- Uploaded PDFs are processed in memory. The app stores metadata, hashes, parser status, and extracted draft data only; raw PDF bytes are not persisted.
- Keep this app local. Do not deploy or publish personal financial data.
- Treat everything under `data/`, `exports/`, and `logs/` as private local runtime material.

## Verification

Run:

```bash
mvn test
mvn package
```

The package command creates build output under `target/`, which is ignored by Git.
