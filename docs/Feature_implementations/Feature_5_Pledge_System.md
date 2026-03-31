# Feature 5: Pledge System

Last updated: 2026-03-31

This feature area is implemented and is the core donor transaction path.

---

## Endpoints

- `POST /api/pledges`
- `GET /api/pledges/{id}`
- `DELETE /api/pledges/{id}`
- `GET /api/pledges/active`
- `GET /api/pledges/history`

---

## Current rules

- only `DONOR` users can pledge
- pledge creation no longer checks `emailVerified`
- need rows are locked pessimistically during pledge creation
- requested quantity cannot exceed the remaining quantity
- each new pledge expires 48 hours after creation

---

## Current response behavior

`POST /api/pledges` returns a route-friendly payload containing:

- `pledgeId`
- NGO latitude and longitude
- NGO address
- NGO contact email
- pledge expiry time

That is what powers the delivery screen after a donor pledges.

---

## Receipt and lifecycle

- donor can cancel an active pledge
- NGO can mark an incoming pledge as received from the NGO dashboard
- scheduled job can expire old active pledges
- cancellation or expiry restores pledged quantity to the need

---

## Files of interest

- `controller/PledgeController.java`
- `service/PledgeService.java`
- `repository/PledgeRepository.java`
- `repository/NeedRepository.java`
- `service/ScheduledJobService.java`
