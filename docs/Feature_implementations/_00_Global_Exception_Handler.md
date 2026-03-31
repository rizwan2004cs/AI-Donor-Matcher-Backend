# Global Exception Handler

Last updated: 2026-03-31

The backend currently exposes a single REST exception handler:

- `controller/GlobalExceptionHandler.java`

It normalizes these cases:

- `BadCredentialsException` -> `401`
- `AccessDeniedException` -> `403`
- `MethodArgumentNotValidException` -> `400` with `fieldErrors`
- generic `RuntimeException` -> `400`

Current response pattern:

```json
{
  "error": "Message here",
  "status": 400,
  "timestamp": "2026-03-31T18:00:00"
}
```

Validation pattern:

```json
{
  "error": "Validation failed.",
  "fieldErrors": {
    "fieldName": "must not be blank"
  },
  "status": 400,
  "timestamp": "2026-03-31T18:00:00"
}
```
