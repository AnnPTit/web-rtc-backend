# Web RTC Backend

## Local MySQL setup

1. Create database `webrtc_backend`.
2. Update `src/main/resources/application-local.properties` with your MySQL username and password.

## Run (local profile)

```powershell
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Authentication API

Base path: `/api/auth`

### Register
```
POST /api/auth/register
Content-Type: application/json

{
  "username": "student1",
  "password": "secret123",
  "fullName": "Student One",
  "email": "student1@example.com",
  "role": "STUDENT"
}
```

### Login
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "student1",
  "password": "secret123"
}
```

### Get Current User (requires JWT token)
```
GET /api/auth/me
Authorization: Bearer <token>
```

## User management API (requires authentication)

Base path: `/api/users`

- `GET /api/users` - List all users
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

## Video upload / Cloudflare R2

Two endpoints support direct-to-R2 uploads with metadata tracking.

### Environment variables

Set the following values in your environment or during containerisation:

```
R2_ACCESS_KEY_ID=<your key>
R2_SECRET_ACCESS_KEY=<your secret>
R2_ENDPOINT=https://<account>.r2.cloudflarestorage.com/<bucket>   # includes bucket path
R2_BUCKET_NAME=<bucket>
R2_REGION=<region>         # defaults to us-east-1
```

The R2 bucket must be configured with a CORS rule permitting PUT from your frontend origin (e.g. `http://localhost:4200`).

### API

* `POST /api/videos/presign` – request a presigned upload URL.  **Requires JWT token in `Authorization` header**. JSON body: `courseId`, `lessonId`, `fileName`, optional `fileSize`.
  Returns `{ uploadUrl, objectKey, expiresAt }`.
* `POST /api/videos/metadata` – call after the file has been PUT to R2. **Also requires authentication**. Body should include `courseId`, `lessonId`, `originalFileName`, `objectKey`, `fileSize`.
  Response returns the saved record with `videoUrl`.

Uploads are limited to mp4/mov/mkv files and 1 GB; URLs expire after 10 minutes.

## Tests  

```powershell
./mvnw test
```

