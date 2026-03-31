# Feature 7: Admin Moderation

Last updated: 2026-03-31

This feature area is implemented.

---

## Endpoints

- `GET /api/admin/reports`
- `PUT /api/admin/needs/{id}`
- `DELETE /api/admin/needs/{id}`
- `GET /api/admin/stats`

---

## Current moderation behavior

### Reports

- admins can review donor-submitted NGO reports
- reports are returned in admin summary DTO form

### Need editing

- admin can edit any need

### Need deletion

- active pledges on that need are cancelled
- affected donors are notified
- the need is removed

### Dashboard stats

stats currently include:

- user counts
- NGO counts by status
- total needs
- active needs
- pledge totals
- pledges today
- fulfillments this month
- report totals

---

## Files of interest

- `controller/AdminController.java`
- `service/AdminService.java`
