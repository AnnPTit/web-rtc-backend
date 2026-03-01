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

## Tests

```powershell
./mvnw test
```

