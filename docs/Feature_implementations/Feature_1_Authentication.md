# Feature 1: Authentication

Last updated: 2026-03-31

This feature area is implemented. The current product flow is Firebase-first.

---

## Primary endpoints

- `POST /api/auth/firebase/register`
- `POST /api/auth/firebase/login`

## Secondary legacy endpoints still present

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/resend-verification`
- `POST /api/auth/send-registration-otp`
- `POST /api/auth/send-otp`
- `POST /api/auth/verify-otp`
- `GET /api/auth/verify`

---

## Current frontend flow

### Login

1. Frontend signs in with Firebase Email/Password.
2. Frontend gets Firebase ID token.
3. Frontend calls `POST /api/auth/firebase/login`.
4. Backend verifies the Firebase token.
5. Backend resolves the local app user and returns role-aware session data.

### Register

1. Frontend creates Firebase user.
2. Frontend sends Firebase verification email.
3. Frontend sends Firebase ID token to `POST /api/auth/firebase/register`.
4. Backend creates or links the local user.
5. If role is `NGO`, backend creates the NGO record and uploads the document if provided.

---

## Current backend behavior

- local users are stored in PostgreSQL
- `firebaseUid` links local users to Firebase identities
- existing users can be linked by matching email on first Firebase login
- `emailVerified` is still mirrored locally but is no longer used to block pledge creation

---

## Files of interest

- `controller/AuthController.java`
- `service/FirebaseAuthService.java`
- `service/AuthService.java`
- `service/FirebaseTokenService.java`
- `config/FirebaseConfig.java`
- `config/JwtFilter.java`

---

## Notes

- OTP endpoints remain in the backend, but they are not part of the active deployed frontend flow.
- Firebase is the source of truth for signup, password reset, and email verification in the current architecture.
