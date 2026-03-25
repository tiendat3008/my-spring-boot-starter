# My Spring Boot Starter

A production-style Spring Boot 3.5 starter focused on authentication, user profile management, and social login integration.

This project provides:
- JWT authentication with access/refresh token flow
- Email OTP for verification and password recovery
- Social OAuth login/linking (Google, GitHub)
- User profile and avatar upload API
- Local development stack with PostgreSQL, Redis, MinIO, and MailHog

> [!NOTE]
> The application uses Java 21 and Spring Boot 3.5.11.

## Tech Stack

- Java 21
- Spring Boot 3.5.11
- Spring Security + OAuth2 Resource Server + OAuth2 Client
- Spring Data JPA (PostgreSQL)
- Spring Data Redis
- MinIO (S3-compatible object storage)
- Spring Mail + Thymeleaf (email templates)
- MapStruct + Lombok
- Maven Wrapper (`./mvnw`)

## Project Structure

```text
src/main/java/com/spring/starter
|- auth/            # authentication, JWT, OAuth2, OTP, recovery, sessions
|- user/            # user profile APIs and business logic
|- infrastructure/  # integrations: mail, object storage
|- common/          # shared config, security, exceptions, dto, utils
```

## Quick Start

### 1. Prerequisites

- JDK 21
- Docker + Docker Compose

### 2. Start local dependencies

```bash
docker compose up -d
```

Services started by default:
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`
- MailHog SMTP: `localhost:1025`
- MailHog UI: `http://localhost:8025`

### 3. Prepare JWT key pair

The app expects:
- `src/main/resources/app.key` (private key)
- `src/main/resources/app.pub` (public key)

Generate keys locally if missing:

```bash
openssl genrsa -out src/main/resources/app.key 2048
openssl rsa -in src/main/resources/app.key -pubout -out src/main/resources/app.pub
```

> [!IMPORTANT]
> `app.key` and `app.pub` are ignored by git. Create them locally before running the app.

### 4. Run the app (dev profile)

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

Application runs on: `http://localhost:8080`

## Build and Test

```bash
./mvnw clean compile
./mvnw test
./mvnw clean package
```

If full tests fail due to missing infrastructure, ensure Docker services are up first.

## Configuration

Default config is split into:
- `application.yml`: shared defaults (JWT, OAuth providers, storage)
- `application-dev.yml`: local datasource, Redis, MinIO, MailHog

Important environment variables:
- `JWT_ACCESS_EXPIRY`
- `JWT_REFRESH_EXPIRY`
- `JWT_ISSUER`
- `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`
- `GITHUB_OAUTH_CLIENT_ID`, `GITHUB_OAUTH_CLIENT_SECRET`
- `STORAGE_PROVIDER`, `STORAGE_BUCKET`
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
- `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`

## API Overview

Base URL: `http://localhost:8080`

### Authentication (`/api/v1/auth`)

- `POST /register`
- `POST /token`
- `POST /refresh`
- `POST /logout`
- `POST /refresh/mobile`
- `POST /logout/mobile`
- `POST /change-password`
- `POST /forgot-password`
- `POST /reset-password`
- `POST /resend-otp`
- `POST /verify-email`
- `GET /sessions`
- `DELETE /sessions/{sessionId}`

### Social OAuth

- `GET /api/v1/auth/oauth/state/{provider}`
- `POST /api/v1/auth/oauth/callback/{provider}`
- `GET /api/v1/auth/social/accounts`
- `POST /api/v1/auth/social/link/{provider}`
- `DELETE /api/v1/auth/social/unlink/{provider}`

### User Profile (`/api/v1/users`)

- `GET /me`
- `PUT /me`
- `POST /me/avatar` (multipart/form-data)

### Health Check

- `GET /actuator/health`

## Security Notes

- Access token is sent as Bearer token.
- Refresh token can be handled by secure HTTP cookie flow (`/token`, `/refresh`, `/logout`) or mobile flow endpoints.
- Public endpoints are limited to auth bootstrap/recovery and health check.

## Useful Local URLs

- API base: `http://localhost:8080`
- MailHog UI: `http://localhost:8025`
- MinIO Console: `http://localhost:9001`

## Troubleshooting

- `Unable to load key from: classpath:app.key`
  - Generate key pair under `src/main/resources`.
- DB/Redis/MinIO connection errors
  - Verify `docker compose up -d` is running and ports are free.
- OAuth callback fails
  - Check provider client ID/secret and redirect URI settings.
