# Landing Tarjetas

Local-only V1 for a personal credit-card statement dashboard. It supports local PDF upload, draft review, confirmed dashboard data, and installment projections for future months while keeping pesos and USD separate with no conversion. Real statement samples remain intentionally out of scope.

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
| Current scope | V1 local dashboard polish: upload/review, confirmed monthly dashboard data, filters, category admin, installment projections, empty states, and privacy-safe feedback. No real sample statement data. |

## UI scope

The static frontend lives under `src/main/resources/static` and is intentionally framework-free:

- `index.html` renders the dashboard shell, upload/review workflow, filters, transaction table, and category admin.
- `css/styles.css` contains the dark-mode layout.
- `js/*.js` modules call the existing local APIs and keep pesos and USD separate.

Open <http://127.0.0.1:8080> after `mvn spring-boot:run`. Empty states are expected until local statements or transactions exist. Draft statements appear only in the review panel; dashboard totals and the public transaction table stay confirmed-only. The monthly card section explicitly shows missing Santander VISA, Santander AMEX, and Naranja X statements for the selected month. Future month tabs marked `Projection` show estimated remaining installments generated from confirmed statements; real months are labeled `Actual` and suppress projection rows in the primary detail to avoid double-counting.

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
- `GET /api/dashboard/months`
- `GET /api/dashboard/months/{yyyy-MM}`

Use `month` as `YYYY-MM`. Currency fields stay separate: pesos and USD are stored and summed independently, with no conversion.

## Installment projection rules

Projection generation runs when a draft statement is confirmed with `POST /api/statements/{id}/confirm`:

- The statement `paymentMonth` is normalized to the first day of that month and used as the base month.
- The current installment remains part of the confirmed statement month as an actual transaction row.
- Future rows are generated only for remaining installments. Example: `3/6` in July creates `4/6` in August, `5/6` in September, and `6/6` in October.
- Re-confirming a statement replaces existing projection rows for that statement before inserting new rows, so confirmation is idempotent and does not duplicate projections.
- Peso amounts and USD amounts are copied into separate fields. The app never converts between currencies.
- Projection rows keep a source statement and source transaction reference so the UI can show where an estimate came from and the estimated finish month.

Plan Z / Zeta detection is conservative. If a detected row says `Plan Z` or `Zeta` and includes a current installment, the draft defaults to three total installments. If the current installment is missing, the draft row is left pending with a review note instead of inventing values; edit it before confirmation if it should generate projections.

## Backend upload scope

`POST /api/statements/upload` accepts one or more multipart PDF files using the `files` field. The backend processes each file in memory with Apache PDFBox, computes a SHA-256 hash for source tracking, detects the first matching parser, and creates a `DRAFT` statement when a parser is detected.

The browser upload/review UI uses the same endpoint and existing statement/transaction APIs:

- Select one or more local PDF files using multipart field `files`.
- Review per-file parser status, hash, detected provider/card, warnings, and draft links without exposing extracted text.
- Edit draft statement fields, including payment month, dates, totals, minimum payment in pesos, provider, card, and local alias.
- Edit/delete detected draft transactions and assign existing categories.
- Confirm the draft only after payment month and at least one total are present.

Current parser detection covers minimal synthetic-safe patterns for:

- Santander Visa
- Santander American Express
- Naranja X

Parsing is intentionally conservative. Missing or low-confidence fields remain `null` with warnings instead of invented values. Draft statements do not affect public transaction listing or dashboard totals; only `CONFIRMED` statements are included there.

## Privacy rules

- Do not commit real PDFs, statements, exports, database files, logs, or local secrets.
- Do not use real personal data, real card numbers, or real financial values in examples.
- Uploaded PDFs are processed in memory. The app stores metadata, hashes, parser status, and extracted draft data only; raw PDF bytes are not persisted.
- User-facing errors are intentionally short and do not display extracted statement text or raw PDF content.
- Keep this app local. Do not deploy or publish personal financial data.
- Treat everything under `data/`, `exports/`, and `logs/` as private local runtime material.

## Verification

Run:

```bash
mvn test
mvn package
```

The package command creates build output under `target/`, which is ignored by Git.

`mvn test` also runs a lightweight Node.js static UI behavior check. Install Node.js before running the full test suite. Static browser modules can be syntax-checked directly with:

```bash
node --check src/main/resources/static/js/api.js
node --check src/main/resources/static/js/app.js
node --check src/main/resources/static/js/categories.js
node --check src/main/resources/static/js/dashboard.js
node --check src/main/resources/static/js/statements.js
node --check src/main/resources/static/js/transactions.js
node --check src/main/resources/static/js/utils.js
```
