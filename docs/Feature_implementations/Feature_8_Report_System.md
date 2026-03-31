# Feature 8: Report System

Last updated: 2026-03-31

This feature area is implemented.

---

## Endpoint

- `POST /api/ngos/{id}/report`

Auth:

- authenticated donor path in the current frontend flow

---

## Current behavior

- report stores reporter, NGO, reason, and timestamp
- reporter receives acknowledgement email
- once an NGO reaches 3 or more reports, admin is notified

---

## Files of interest

- `controller/NgoController.java`
- `service/ReportService.java`
- `repository/ReportRepository.java`
