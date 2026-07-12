# Deploy the secured personal dashboard

This app now requires a login before any dashboard or `/api/**` route is available. Set the production environment variables first, then run the Spring Boot app with the `prod` profile.

## Quick path

1. Generate a BCrypt hash for your password.
2. Set the required environment variables:

   ```bash
   export APP_SECURITY_USER="your-user"
   export APP_SECURITY_PASSWORD_HASH='your-bcrypt-hash'
   export APP_DATASOURCE_URL='jdbc:h2:file:/var/lib/landing-tarjetas/landing-tarjetas;DB_CLOSE_ON_EXIT=FALSE'
   export APP_DATASOURCE_USERNAME='landing_user'
   export APP_DATASOURCE_PASSWORD='change-this-private-password'
   export SPRING_PROFILES_ACTIVE=prod
   ```

3. Put HTTPS in front of the app, then open `https://<your-domain>/login`.
4. For a short HTTP-only smoke test before HTTPS exists, set `APP_SESSION_COOKIE_SECURE=false`, test `http://<vm-public-ip>:8080/login`, then remove the override before normal use.

## Required production variables

| Variable | Required | Purpose |
|----------|----------|---------|
| `SPRING_PROFILES_ACTIVE=prod` | Yes | Loads production-safe settings. |
| `APP_SECURITY_USER` | Yes | Login username. |
| `APP_SECURITY_PASSWORD_HASH` | Yes | BCrypt password hash used by Spring Security. |
| `APP_DATASOURCE_URL` | Yes | Explicit production database URL; there is no prod fallback to the local dev database. |
| `APP_DATASOURCE_USERNAME` | Yes | Production database user. The app refuses the default H2 `sa` user in prod. |
| `APP_DATASOURCE_PASSWORD` | Yes | Production database password. Empty passwords fail startup in prod. |
| `APP_SESSION_COOKIE_SECURE` | Recommended | Defaults to `true` in prod. Set `false` only for temporary HTTP testing. |

## Generate a BCrypt hash

Use any trusted BCrypt generator. If Java dependencies are available on the VM, you can generate one with Spring Security from a temporary local snippet or trusted admin tooling. Do not commit the raw password, the hash, `.env` files, database files, logs, uploads, or PDFs.

## Production behavior

| Area | Behavior |
|------|----------|
| Login | `/login` is public; valid credentials redirect to the dashboard. |
| Routes | Dashboard routes and `/api/**` require authentication. |
| CSRF | CSRF stays enabled. Browser `fetch` calls send the `X-XSRF-TOKEN` header from the CSRF cookie. |
| Logout | `POST /logout` clears the session and cookies. |
| H2 Console | Disabled in `application-prod.properties`. |
| Error details | Stack traces and technical error details are not included in production error responses. |
| Session cookie | HttpOnly and SameSite=Lax; Secure defaults to `true` for HTTPS. |
| Fail closed | The prod profile refuses to start if `app.security.enabled=false`, required datasource variables are missing, or the datasource user is `sa`. |

## Smoke tests

Use HTTPS first because production session cookies default to `Secure`.

1. Open `https://<your-domain>/login` and sign in.
2. Confirm the dashboard loads and logout returns to `/login?logout`.
3. Check the service state on the VM with `systemctl status landing-tarjetas` and the latest logs with `journalctl -u landing-tarjetas -n 100 --no-pager`.

Temporary HTTP testing is only for pre-HTTPS connectivity checks. Set `APP_SESSION_COOKIE_SECURE=false`, restart, test login once over HTTP, then restore the default before public use. Do not leave the secure-cookie override disabled.

## Public VM observability

Production enables Tomcat access logs in `logs/landing-tarjetas-access.*.log`. The pattern logs method, path, protocol, status, bytes, and request latency without query strings, request bodies, cookies, or headers.

Check recent error rate and slow responses:

```bash
tail -n 1000 logs/landing-tarjetas-access.*.log \
  | awk '{ total++; if ($(NF-2) ~ /^5/) errors++; if ($(NF) > 2000) slow++; } END { if (total == 0) total = 1; printf "5xx_rate=%.2f%% slow_over_2s=%.2f%% total=%d\n", errors * 100 / total, slow * 100 / total, total }'
```

Alert if `5xx_rate` stays above 1% or `slow_over_2s` stays above 10% for two consecutive checks. The safe first action is to inspect `journalctl -u landing-tarjetas` and the access log window, then roll back to the previous known-good jar and environment file if the spike started with the latest deploy.

## Recovery and rollback

1. If startup fails, check the service logs first. Missing `APP_SECURITY_*` or `APP_DATASOURCE_*` variables are expected hard failures.
2. Restore the previous known-good service environment file or unit override, then restart the service.
3. If the database file is damaged or a bad deployment wrote unexpected data, stop the service, restore the private VM backup of the database path from `APP_DATASOURCE_URL`, then start the previous known-good jar.
4. Do not bypass the guard with `app.security.enabled=false` in prod. For emergency local debugging, bind to `127.0.0.1` without the `prod` profile instead.

## Privacy checklist

- [ ] No real PDFs are committed or copied into the repository.
- [ ] `data/`, `uploads/`, `logs/`, `.env*`, and local database files remain ignored by Git.
- [ ] The production VM filesystem is backed up privately if it contains real financial data.
- [ ] HTTPS is configured before leaving `APP_SESSION_COOKIE_SECURE=true` in normal use.
