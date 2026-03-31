# Feature 6: Admin Verification

Last updated: 2026-03-31

This feature area is implemented.

---

## Endpoints

- `GET /api/admin/ngos/pending`
- `GET /api/admin/ngos`
- `GET /api/admin/ngos/{id}/needs`
- `POST /api/admin/ngos/{id}/approve`
- `POST /api/admin/ngos/{id}/reject`
- `POST /api/admin/ngos/{id}/suspend`

---

## Approval behavior

When an admin approves an NGO:

- NGO status becomes `APPROVED`
- `verifiedAt` is set
- trust score is recalculated
- approval email is sent

When an admin rejects an NGO:

- rejection reason is stored
- rejection email is sent

---

## Suspension behavior

Suspension is transactional and includes:

- NGO status -> `SUSPENDED`
- active needs -> closed as expired
- active pledges -> cancelled
- affected donors -> notified

---

## Files of interest

- `controller/AdminController.java`
- `service/AdminService.java`
- `dto/AdminNgoSummaryResponse.java`
