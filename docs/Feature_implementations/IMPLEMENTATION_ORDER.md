# Implementation Order

Last updated: 2026-03-31

The original implementation-order plan is now mostly historical. The current backend feature set is wired and available. The remaining prioritization is about cleanup and polish rather than missing controllers.

---

## 1. Already implemented end-to-end

1. Firebase registration and login
2. NGO approval and moderation
3. NGO profile completion and geocoding
4. NGO need creation and management
5. Donor discovery and NGO detail
6. Donor pledge lifecycle
7. NGO incoming pledge receipt
8. Admin reports and dashboard stats
9. Scheduled expiry jobs

---

## 2. Recommended next cleanup order

1. remove or formally deprecate dormant OTP endpoints from the backend if they are no longer needed
2. decide whether legacy password login should remain supported
3. wire fulfilled NGO history into the public NGO detail contract if the frontend needs it
4. harden production deployment across Render and Vercel
5. add deeper deployment smoke checks and observability

---

## 3. Deployment-critical priorities

1. keep Firebase admin configuration stable in Render
2. keep Vercel Firebase web envs aligned with the backend
3. verify CORS against the deployed frontend domain
4. verify `/api/ngos`, Firebase login, and pledge flow after every deployment
