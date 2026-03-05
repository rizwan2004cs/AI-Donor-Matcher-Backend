# AI DONATION MATCHER

## Complete Final Project Documentation

**A Location-Based Platform Connecting Donors with Verified NGOs**

**Team Members:** Dinesh | Gowtham | Sriram | Rizwan

**Final Year Engineering Project | Academic Year 2025–26**

**Stack:** React.js + Spring Boot 3.2.5 + Java 21 + PostgreSQL | **Timeline:** 14 Weeks

**All Tools: 100% Free — No Credit Card Required**

---

## 1. Project Overview

### 1.1 What It Is

The AI Donation Matcher is a location-based web platform that connects individual donors with nearby, verified NGOs based on real-time needs. NGOs post specific, current resource requirements; donors discover and respond to those needs by proximity and deliver goods directly. The platform removes guesswork from local charitable giving by combining location awareness, a trust-verified NGO directory, and a need-specific pledge system with in-app navigation.

### 1.2 The Problem It Solves

Despite genuine societal willingness to donate goods, local charitable ecosystems remain fragmented. Donors cannot easily verify which nearby organisations are legitimate or what they actually need right now. NGOs receive the wrong items while their real needs go unmet. No unified, trust-filtered channel exists to coordinate donor supply with NGO demand at a local level. This fragmentation leads to wasted resources, unmet needs, and eroding donor confidence.

**Core Gap Identified**

There is no accessible, location-based platform that simultaneously allows verified NGOs to post current, specific resource needs and enables nearby donors to discover and respond with confidence in organisational legitimacy. Good intentions consistently fail to translate into efficient, targeted community impact.

### 1.3 Target Users

The platform is built around three user roles:

- **Donors:** Individuals with usable goods who want to give meaningfully without trust uncertainty. The donor is the primary actor — they discover needs, pledge items, and arrange delivery directly with the NGO.
- **NGOs:** Local charitable organisations that need a structured channel to communicate real-time resource requirements to a nearby, verified donor base.
- **Admins:** Platform gatekeepers responsible for NGO verification before any organisation becomes publicly visible, and for ongoing platform governance including moderation and suspension.

**Design Decision: Volunteer Role Removed from Prototype**

Volunteer coordination was removed after identifying that task abandonment — a volunteer claiming a delivery and not completing it — creates a state machine edge case that cannot be reliably resolved without real users and real usage data.

The core value of the platform is fully intact without volunteers: donors discover verified NGOs, pledge against real needs, and deliver directly. This produces a simpler, more reliable, and fully demonstrable coordination loop.

Volunteer coordination is documented as the highest-priority future scope item. The `@Scheduled` expiry mechanism and task board design are already fully specified and implementation-ready.

### 1.4 Project Scope Boundary

This is a matching and coordination prototype. The platform connects donors to verified NGOs with specific needs. Physical delivery is arranged directly between donor and NGO after a pledge is made. The trust score is an assistive credibility indicator, not a legal certification. Admin verification is manual — human-reviewed, not automated document analysis.

---

## 2. Problem Statement

### 2.1 The Donor's Dilemma

Individuals who possess usable goods frequently experience donor hesitation rooted in two uncertainties: they cannot easily verify whether a nearby organisation is legitimate, and they do not know whether their specific items are currently needed. Existing platforms either focus on monetary donations or function as static directories with no real-time need information, forcing donors into time-consuming manual research that creates enough friction to discourage giving altogether.

### 2.2 The NGO's Operational Challenge

Grassroots and local NGOs routinely face a resource mismatch — receiving surplus quantities of items they do not need while facing shortages of what they urgently require. Smaller organisations lack the digital infrastructure to broadcast current needs to a nearby donor base. Even when willing donors exist locally, NGOs have no structured channel to reach them. Genuine supply and genuine demand exist in the same geography but fail to connect efficiently.

### 2.3 Why Existing Solutions Fail

Donation platforms are built predominantly for monetary giving and do not support physical goods coordination. Directory listings are static and do not reflect real-time organisational needs. Verification, where it exists at all, is left entirely to the donor to perform manually. No existing platform combines donor discovery, NGO need-posting, location-based filtering, and a trust-scoring layer within a single, integrated interface.

---

## 3. Proposed Solution & Value Proposition

### 3.1 Solution Overview

The AI Donation Matcher addresses the identified gaps through a structured, location-aware web platform. The platform is a trust-filtered matching layer — it reduces friction at the precise point where donor supply and NGO demand currently fail to meet. It does not manage logistics end-to-end; it ensures the right donor reaches the right NGO with confidence, and guides them there with in-app navigation.

### 3.2 Services Per Stakeholder

**For Donors — Verified Discovery, Need-Specific Matching, and Guided Delivery**

Donors access a map-based interface showing only administrator-verified NGOs within their chosen radius. Pins are colour-coded by category. Each pin popup shows the top active need and remaining quantity. Full NGO profiles display all active needs with progress bars, trust scores, and fulfilled history. After pledging, an in-app Leaflet map with OSRM routing guides the donor directly to the NGO. The donor's dashboard tracks all active pledges and lifetime donation history.

**For NGOs — Real-Time Need Broadcasting and Pledge Management**

Verified NGOs post, update, and close specific resource requests from a managed dashboard. A two-stage go-live process ensures only complete, verified profiles appear on the donor map. NGOs see incoming pledges with donor contact details. Needs are automatically locked from editing once pledged. The public NGO profile displays active needs and fulfilled history as proof of activity.

### 3.3 How the Platform Bridges the Core Gap

- **Trust deficit:** Reduced through admin-gated NGO verification and a rule-based trust score with four transparent, auditable inputs.
- **Resource mismatch:** Addressed by making categorised NGO need-posts the primary unit of interaction. Donors respond to specific stated needs, not speculative giving.
- **Discovery friction:** Eliminated by location-based filtering with a radius slider, category filters, NGO name search, and auto-expand when no local results exist.
- **Delivery uncertainty:** Reduced by in-app OSRM navigation from donor's GPS to NGO address, persistent on the Active Pledges dashboard until fulfillment.

---

## 4. User Journeys

### 4.1 The Donor Journey

**Step 1 — Register and Verify Email:**
Donor registers with full name, email, location, and password and selects the Donor role. A verification email is sent immediately via JavaMail. Donor clicks the link to activate the account. Unverified donors can browse the map and view NGO profiles but the Pledge button is disabled with tooltip: 'Verify your email to pledge.'

**Step 2 — Discover NGOs on the Map:**
Donor opens the discovery page. Browser requests location permission. Map and list view populate with verified NGOs within the default 5 km radius. Each pin is colour-coded by need category. The list is sorted by distance first, with urgency used as a secondary sort within distance bands.

**Step 3 — Search and Filter:**
Donor uses the search bar to find a specific NGO by name (overrides radius filter), or uses the category dropdown and radius slider (1–50 km) to filter. Both filters apply simultaneously to the same backend query. If no NGOs are found within the set radius, the map automatically expands to show all verified NGOs nationwide with a clear banner: 'No NGOs found nearby. Showing all available NGOs.'

**Step 4 — View Pin Popup:**
Donor clicks an NGO map pin. A popup appears showing: NGO name, profile photo thumbnail, trust score tier label, distance from donor, category colour icon, and the top 1 active need with remaining quantity. Donor clicks 'View Full Profile' to proceed.

**Step 5 — Evaluate NGO Full Profile:**
Full profile page shows: profile photo, complete address, contact email, all active needs with quantity progress bars and urgency flags, trust score numeric value and tier label, and a 'Past Fulfilled Needs' section showing the NGO's track record. A 'Report this organisation' link is at the bottom.

**Step 6 — Pledge a Donation:**
Donor selects a specific need. A quantity selector appears showing total required, already pledged by others, and remaining. Donor inputs their quantity (capped at remaining). Clicks 'Confirm Pledge.' A confirmation email is immediately sent to the donor containing the NGO address and contact email.

**Step 7 — In-App Navigation to NGO:**
Immediately after pledging, the Delivery View opens. An embedded Leaflet map shows the road route from the donor's current GPS location to the NGO's address using OSRM routing. Estimated distance and travel time are displayed above the map. The NGO's contact email is shown with a prompt to arrange drop-off time before leaving.

**Step 8 — Active Pledges Dashboard:**
The donor's dashboard shows all active pledges simultaneously. Each pledge card shows NGO name, category icon, item pledged, quantity, pledge status, a countdown to the 48-hour expiry, a persistent 'Navigate' button (reopens the Delivery View), and a 'Cancel Pledge' button.

**Step 9 — Pledge Cancellation or Auto-Expiry:**
Donor can cancel a pledge anytime before fulfillment. Cancellation restores the quantity to the need immediately and notifies the NGO by email. If the donor does not deliver within 48 hours, the pledge auto-expires: quantity is restored, the need status recalculates, and the donor receives an expiry notification email.

**Step 10 — Fulfillment Confirmation and History:**
The NGO marks the request as Fulfilled from their dashboard. The donor receives a thank-you confirmation email. The pledge moves from Active Pledges to the Donation History tab, showing NGO name, category, item, quantity, and date. Clicking a history entry reopens the NGO profile, enabling easy re-donation.

### 4.2 The NGO Journey

**Step 1 — Register and Submit Documents:**
NGO representative creates an account, selects the NGO role, fills basic details, and uploads required credentials (registration certificates, contact details). Profile is set to PENDING and not publicly visible. Admin receives an email notification of the new application.

**Step 2 — Admin Verification:**
The platform administrator reviews submitted documents from the verification queue. Admin approves or rejects the application with an optional written reason. NGO receives an email notification with the outcome. If rejected, the NGO may reapply with corrected documentation.

**Step 3 — Profile Completion — Go-Live Gate:**
Approved NGO logs in and is directed to the profile completion screen. Required fields: organisation name, full address (used for map pin placement), contact email, contact phone, organisation description (minimum 50 characters), and category of work. Optional: profile photo (JPG/PNG, max 2MB, hosted on Cloudinary free tier). A progress bar tracks completion. The NGO becomes publicly visible on the donor map only when all required fields are complete.

**Step 4 — Post a Resource Need:**
From the dashboard, NGO clicks 'Add New Need.' Form fields: category dropdown (Food, Clothing, Medicine, Education Supplies, Household Items, Other), item name (free text), optional description, quantity required, urgency flag (Normal/Urgent), and optional expiry date. Maximum 5 active needs at a time. On reaching the cap, the Add button is disabled with an explanatory message.

**Step 5 — Manage Active Needs:**
NGO views all active needs with real-time pledged and remaining quantities displayed as progress bars. Needs with at least one active pledge show a lock icon — edit and delete buttons are hidden. Only the 'Mark as Fulfilled' action is available on locked needs. Needs approaching their expiry date show a countdown warning.

**Step 6 — Receive Pledge Notifications:**
NGO receives an email when a donor pledges. The incoming pledges view shows donor name, donor email, item pledged, quantity pledged, and timestamp. NGO contacts the donor via email to confirm a drop-off time and date.

**Step 7 — Mark as Fulfilled:**
Once physical goods are received, NGO marks the request as Fulfilled. This triggers: need status updates to FULFILLED, donor receives a thank-you confirmation email, transaction is logged in the NGO's fulfilled history, and trust score is recalculated.

**Step 8 — Monitor Trust Score:**
NGO dashboard shows the current trust score with numeric value and tier label. Score reflects verification status, profile completeness, total fulfilled count, and activity recency. Recalculated automatically on fulfillment and profile update events.

### 4.3 The Admin Journey

**Step 1 — Verification Queue:**
Admin dashboard shows all NGO applications in PENDING status with submitted documents available inline. Admin reviews completeness and authenticity of credentials.

**Step 2 — Approve or Reject:**
Admin approves the NGO (proceeds to profile completion stage) or rejects with a written reason. All decisions are logged with timestamp and admin ID.

**Step 3 — Need Moderation:**
Admin can view, edit, or remove any NGO's posted need. Removing a need with active pledges triggers automatic cascade: pledges cancelled, affected donors notified by email, quantity released. All moderation actions are logged.

**Step 4 — Report Queue:**
Donor-submitted reports appear in the report queue with reporter details, reason, and timestamp. If 3 or more reports exist against the same NGO, it is automatically elevated with a warning banner. Admin decides: dismiss, warn, or suspend.

**Step 5 — Suspend an NGO:**
Admin clicks Suspend. The suspension cascade runs as a single atomic `@Transactional` operation: NGO status set to SUSPENDED, all active needs closed, all active pledges cancelled, all affected donors notified by email immediately, NGO disappears from donor map. Operation is all-or-nothing — no partial suspension states.

**Step 6 — Platform Overview:**
Admin sees aggregate counts: total verified NGOs, total active needs, total pledges today, total fulfillments this month, and any flagged accounts awaiting attention.

---

## 5. UI Flow — Screen by Screen

This section describes the layout and behaviour of every screen in the platform. These descriptions serve as the reference specification for all frontend implementation work.

### 5.1 Registration Screen

```
┌────────────────────────────────────────────┐
│         AI DONATION MATCHER                │
│  ─────────────────────────────────────     │
│  I am a:  [ Donor ]  [ NGO ]              │
│                                            │
│  Full Name:  [                         ]   │
│  Email:      [                         ]   │
│  Password:   [                         ]   │
│  Location:   [                         ]   │
│                                            │
│  -- NGO only: Upload Documents ----------  │
│  [ Choose File ]  reg_certificate.pdf      │
│  ----------------------------------------- │
│                                            │
│         [ Create Account ]                 │
│  Already have an account?  [ Log In ]      │
└────────────────────────────────────────────┘
```

### 5.2 Email Verification Screen

```
┌────────────────────────────────────────────┐
│  ✉ Verify Your Email                       │
│  ─────────────────────────────────────     │
│  A verification link has been sent to:     │
│  rahul@gmail.com                           │
│                                            │
│  Click the link in your email to activate  │
│  your account before pledging.             │
│                                            │
│  [ Resend Verification Email ]             │
│                                            │
│  You can browse the map while you wait.    │
└────────────────────────────────────────────┘
```

### 5.3 Donor Discovery Screen

```
┌─────────────────────────────────────────────────────┐
│  [ 🔍 Search NGO by name...                      ]  │
│  [ All Categories ▼ ]   Radius: 1km ━━●━━ 50km     │
│                                  5km [ Apply ]      │
├──────────────────────────┬──────────────────────────┤
│                          │  LIST VIEW (sorted)      │
│   LEAFLET MAP            │  ───────────────────────  │
│                          │  🔴 City Care  0.8km      │
│                          │     🔴 Urgent             │
│  🔴 pin  🔵 pin  🟢 pin  │     Needs: Coats (10 left)│
│       🟡 pin             │  ───────────────────────  │
│                          │  🔵 Hope Fdn   1.2km      │
│   (colour = category)   │     Normal                │
│                          │     Needs: Rice (50 left) │
└──────────────────────────┴──────────────────────────┘

Pin Key: 🔴Food  🔵Clothing  🟢Medicine  🟡Education
         🟠Household  🟣Other
```

### 5.4 Map Pin Popup

```
┌────────────────────────────────┐
│ [photo] City Care Foundation   │
│         ★ Trusted  84          │
│         📍 0.8 km away         │
│  ────────────────────────────  │
│  Top Need:                     │
│  🔴 Winter Coats               │
│  10 remaining of 30  🔴 Urgent │
│  ────────────────────────────  │
│    [ View Full Profile ]       │
└────────────────────────────────┘
```

### 5.5 NGO Full Profile Screen

```
┌──────────────────────────────────────────────┐
│ [Photo]  City Care Foundation                │
│          ★ Trusted  84                       │
│          📍 123 Main St, Tambaram, Chennai    │
│          ✉ citycare@gmail.com                │
│          Verified since: Jan 2024            │
│  ──────────────────────────────────────────  │
│  About: Supporting underprivileged...        │
│  ──────────────────────────────────────────  │
│  ACTIVE NEEDS                                │
│  🔴 Winter Coats        🔴 Urgent  ⏰ 3 days  │
│  [██████████████░░░░░░] 67% pledged          │
│  10 remaining of 30                          │
│  [ Pledge This Item ]                        │
│  ──────────────────────────────────────────  │
│  🔵 Rice (50 kg)         ● Normal             │
│  [░░░░░░░░░░░░░░░░░░░░] 0% pledged          │
│  50 remaining of 50                          │
│  [ Pledge This Item ]                        │
│  ──────────────────────────────────────────  │
│  PAST FULFILLED NEEDS                        │
│  ✓ Blankets (20)          Feb 2025           │
│  ✓ School Bags (15)       Jan 2025           │
│  ✓ Food Packets (100)     Dec 2024           │
│  ──────────────────────────────────────────  │
│               ⚑ Report this organisation     │
└──────────────────────────────────────────────┘
```

### 5.6 Pledge Screen

```
┌────────────────────────────────────────┐
│  Pledge to City Care Foundation        │
│  ────────────────────────────────────  │
│  Item:       Winter Coats              │
│  Category:   🔴 Clothing               │
│  Urgency:    🔴 Urgent                 │
│  ────────────────────────────────────  │
│  Total needed:     30                  │
│  Pledged by others: 20                 │
│  Still remaining:   10                 │
│  ────────────────────────────────────  │
│  How many can you donate?              │
│  [ − ]   [ 5 ]   [ + ]   (max: 10)    │
│                                        │
│       [ Confirm Pledge ]               │
│                                        │
│  A confirmation email will be sent to  │
│  you with NGO address and contact.     │
└────────────────────────────────────────┘
```

### 5.7 Post-Pledge Delivery View (In-App Navigation)

```
┌─────────────────────────────────────────────┐
│  Navigate to City Care Foundation           │
│  📍 2.3 km away  |  ~8 mins by car          │
│  ─────────────────────────────────────────  │
│                                             │
│   [ LEAFLET MAP — OSRM ROUTE DRAWN ]       │
│   [ 📍 You are here                    ]    │
│   [    ≈≈≈≈≈ road route ≈≈≈≈≈          ]   │
│   [                 🏢 Drop-off here   ]    │
│                                             │
│  ─────────────────────────────────────────  │
│  NGO Address: 123 Main St, Tambaram        │
│  Contact:     citycare@gmail.com           │
│  📧 Email NGO to confirm drop-off time      │
│                                             │
│  Your Pledge: 5 Winter Coats               │
│  Status: ● Active  |  Expires in: 47h 12m  │
│                                             │
│             [ Cancel Pledge ]               │
└─────────────────────────────────────────────┘
```

### 5.8 Donor Dashboard

```
┌──────────────────────────────────────────────┐
│  My Dashboard — Rahul Sharma                 │
│  [ Active Pledges (2) ]  [ History (14) ]    │
│  ──────────────────────────────────────────  │
│  ACTIVE PLEDGES TAB:                         │
│  City Care Foundation       0.8km            │
│  🔴 5 Winter Coats  |  ● Active              │
│  Expires in: 47h 12m                         │
│  [ Navigate ]   [ Cancel Pledge ]            │
│  ──────────────────────────────────────────  │
│  Hope Foundation            1.2km            │
│  🔵 10 kg Rice  |  ● Active                  │
│  Expires in: 38h 45m                         │
│  [ Navigate ]   [ Cancel Pledge ]            │
│  ──────────────────────────────────────────  │
│  HISTORY TAB:                                │
│  ✓ City Care Fdn  🔴 5 Coats      Dec 2025   │
│  ✓ Hope Fdn       🔵 10kg Rice    Nov 2025   │
│  ✓ Child Aid      🟡 3 School Bags Oct 2025   │
│  (click any row to reopen NGO profile)       │
└──────────────────────────────────────────────┘
```

### 5.9 NGO Dashboard

```
┌────────────────────────────────────────────┐
│  City Care Foundation Dashboard            │
│  Trust Score: 84  ★ Trusted                │
│  Active Needs: 2/5  |  Fulfilled: 47       │
│  ────────────────────────────────────────  │
│  ACTIVE NEEDS                              │
│  🔴 Winter Coats  20/30 pledged  🔴 Urgent  │
│  🔒 Locked (has active pledges)            │
│  [ Mark as Fulfilled ]                     │
│                                            │
│  🔵 Rice (50kg)    0/50 pledged  ● Normal   │
│  [ Edit ]  [ Delete ]                      │
│                                            │
│  [ + Add New Need ] (3 slots remaining)    │
│  ────────────────────────────────────────  │
│  INCOMING PLEDGES                          │
│  Rahul Sharma  rahul@gmail.com             │
│  5 Winter Coats  |  2 hours ago            │
│  ────────────────────────────────────────  │
│  Priya Mehta   priya@gmail.com             │
│  15 Winter Coats  |  5 hours ago           │
└────────────────────────────────────────────┘
```

### 5.10 NGO Profile Completion Screen

```
┌──────────────────────────────────────────┐
│  Complete Your Profile to Go Live        │
│  [████████████████░░░░]  80% Complete    │
│  ──────────────────────────────────────  │
│  ✅ Organisation Name                    │
│  ✅ Full Address                         │
│  ✅ Contact Email                        │
│  ✅ Contact Phone                        │
│  □ Organisation Description (min 50 chars)│
│  ✅ Category of Work                     │
│  □ Profile Photo (optional)              │
│  ──────────────────────────────────────  │
│  Missing: Organisation Description       │
│  Profile goes live on the map once       │
│  all required fields are complete.       │
│        [ Save & Complete Profile ]       │
└──────────────────────────────────────────┘
```

### 5.11 Admin Dashboard

```
┌────────────────────────────────────────────┐
│  Admin Dashboard                           │
│  ────────────────────────────────────────  │
│  OVERVIEW                                  │
│  Verified NGOs: 24  |  Active Needs: 67    │
│  Pledges Today: 12  |  Fulfilled/Month: 89 │
│  ────────────────────────────────────────  │
│  VERIFICATION QUEUE (3 pending)            │
│  Hope Foundation — submitted 2h ago        │
│  [ Review ]  [ Approve ]  [ Reject ]       │
│  ────────────────────────────────────────  │
│  REPORT QUEUE                              │
│  ⚠️ City Care Fdn — 3 reports  [ View ]   │
│     Green Earth   — 1 report   [ View ]    │
│  ────────────────────────────────────────  │
│  NGO MANAGEMENT                            │
│  City Care Foundation  ★ Trusted           │
│  [ View Needs ]  [ Edit ]  [ Suspend ]     │
└────────────────────────────────────────────┘
```

---

## 6. Platform Features

### 6.1 Prototype Scope — Tier 1 (Core Loop) + Tier 2 (Extended)

Tier 1 is the non-negotiable core. Tier 2 is prototype scope built after Tier 1 is fully live. Both tiers will be demonstrated at the panel review.

| Feature | Description | Tier | Scope |
|---------|-------------|------|-------|
| Role-Based Registration | Donor/NGO/Admin roles with distinct dashboards. Email verification before pledging. NGOs upload documents at registration. | 1 | Prototype |
| Admin Verification Gateway | Pending queue, manual document review, approve/reject with reason. Human-in-the-loop entirely. | 1 | Prototype |
| Two-Stage NGO Go-Live | Admin approval + profile completion required. Progress bar. NGO visible only after all required fields filled. | 1 | Prototype |
| NGO Needs Management | Category dropdown + free text + optional description + quantity + urgency + optional expiry date. Max 5 active needs. Locked once pledged. | 1 | Prototype |
| Location-Based Discovery | Haversine JPA query. Radius slider 1–50 km. Auto-expand if no results. Category + radius filters together. | 1 | Prototype |
| Colour-Coded Map Pins | Red=Food, Blue=Clothing, Green=Medicine, Yellow=Education, Orange=Household, Purple=Other. | 1 | Prototype |
| NGO Search Bar | Search by name on discovery page. Overrides radius filter. | 1 | Prototype |
| Pin Popup with Top Need | Name, photo, trust tier, distance, category icon, top need + remaining quantity. | 1 | Prototype |
| NGO Full Profile | Address, all needs with progress bars, trust score, photo, fulfilled history. Report button. | 1 | Prototype |
| Split Pledge System | Multiple donors split a need. Quantity selector capped at remaining. @Transactional. 4 need states. | 1 | Prototype |
| Rule-Based Trust Score | 4 inputs: verification, completeness, fulfilled count, recency. Tiers: New/Established/Trusted. Event-driven recalculation. | 1 | Prototype |
| Post-Pledge Delivery View | Leaflet + OSRM road route. Distance + time. NGO contact. Persistent until fulfilled. | 2 | Prototype |
| Donor Dashboard | Active Pledges tab (Navigate + Cancel) + Donation History tab (click to re-donate). | 2 | Prototype |
| Pledge Auto-Expiry | @Scheduled hourly. 48hr threshold. Quantity restored. Donor notified. | 2 | Prototype |
| Donor Pledge Cancellation | Cancel anytime before fulfillment. Quantity restored. NGO notified. | 2 | Prototype |
| Multi-Pledge Support | Donor holds multiple active pledges simultaneously across different NGOs. | 2 | Prototype |
| NGO Need Expiry | Optional expiry date. @Scheduled auto-close. Warning at 3 days. Active pledges cancelled. | 2 | Prototype |
| NGO Fulfilled History | Past fulfilled needs on public profile. Category, item, quantity, date. No donor names. | 2 | Prototype |
| Email Notifications | 12 triggers via JavaMail + Gmail SMTP. All free. Full table in Section 7.5. | 2 | Prototype |
| Donor Report NGO | Report button on NGO profile. 4 reasons. 3 reports auto-flag admin queue. | 2 | Prototype |
| Admin Need Moderation | Admin edits/removes any NGO need. Cascade cancels pledges, notifies donors. | 2 | Prototype |
| Suspension Cascade | Atomic @Transactional: NGO hidden, needs closed, pledges cancelled, donors notified. | 2 | Prototype |
| Default List Sort | Distance ASC primary. Urgency secondary within 2km distance bands. | 2 | Prototype |

### 6.2 Future Scope — Tier 3

| Feature | Description | Tier | Scope |
|---------|-------------|------|-------|
| Volunteer Coordination | Task board, claim workflow, @Scheduled expiry for abandoned tasks. Design fully specified. | 3 | Future |
| OTP Handover Confirmation | One-time code at drop-off to verify physical handover. | 3 | Future |
| In-App Messaging | Transaction-scoped chat between donor and NGO. Closes at fulfillment. | 3 | Future |
| Smart Notifications | Category-based alerts matching donor history to new NGO requests. | 3 | Future |
| Admin Analytics | Fulfillment rates, category trends, pledge-to-fulfillment timing. | 3 | Future |
| NGO Analytics | Profile views and need impression counts for NGO dashboard. | 3 | Future |
| Multilingual Support | Tamil and Hindi via React i18next. | 3 | Future |
| Mobile Application (React Native) | Full native Android and iOS app with push notifications. Separate codebase from PWA. App Store and Play Store submission required. | 3 | Future |

---

## 7. Technical Blueprint

### 7.1 Technology Stack

Every tool listed below is completely free. No credit card, billing account, or usage limits required for prototype scale.

| Layer | Technology | Justification / Cost |
|-------|-----------|---------------------|
| Frontend | React.js + Tailwind CSS | Component SPA. Three role dashboards. Tailwind responsive styling. React Context API for auth state. 100% free. |
| Maps | Leaflet.js + OpenStreetMap | Free, open-source. No API key. Colour-coded NGO pins, popups, route polylines. Geolocation API with manual fallback. |
| Routing | OSRM Public API | Free open-source routing. Returns GeoJSON road route for Leaflet polyline. Suitable for prototype. Self-host or upgrade for production. |
| Backend | Java 21 + Spring Boot 3.2.5 | MVC: Controller/Service/Repository per domain. 11 services, 5 entities, 5 repositories, 9 DTOs fully implemented. Spring Data JPA. Monolithic — deliberate, right-sized choice. 100% free. |
| Database | PostgreSQL | Relational data model. DOUBLE lat/lng columns. Haversine native JPA query (@Query). PostGIS not needed at prototype scale. 100% free. |
| Auth | Spring Security + JWT (jjwt 0.12.3) + BCrypt | Role-based endpoint protection. JWT tokens with userId, role, emailVerified claims. BCrypt password hashing. No third-party auth service. 100% free. |
| Geocoding | Nominatim (OpenStreetMap) | Converts NGO address text to lat/lng coordinates on profile update. Free, no API key required. Used inside NgoService.updateProfile(). |
| Images | Cloudinary Free Tier | NGO profile photos. 10 GB storage. 25 GB bandwidth/month. No credit card. URL stored in NGO entity. |
| Email | JavaMail + Gmail SMTP | 500 emails/day free. No credit card. All 12 platform triggers. Replaces SendGrid/AWS SES entirely. |
| Hosting (FE) | Vercel | Permanent free tier. GitHub auto-deploy. Zero config. |
| Hosting (BE) | Render | Free tier. Spins down after 15min inactivity — expected for prototype. |
| Hosting (DB) | Supabase or Neon | Supabase: 500MB free. Neon: 3GB free. Web dashboards. Railway excluded — free tier discontinued 2023. |
| PWA | manifest.json + Service Worker | Makes the React web app installable on Android and iOS home screens. Android: Chrome auto-prompts install banner. iOS: Safari Share → Add to Home Screen. Offline mode caches last loaded map and visited NGO profiles via service worker. Same React codebase — no separate app development needed. Zero additional cost. |

### 7.2 System Architecture

Monolithic three-tier client-server. All roles share the same core entities (NGO, Need, Pledge). Microservices would require distributed transactions and API gateways with no user-facing benefit at this scope.

**Architecture Tiers:**

- **Presentation:** React frontend in browser. Captures input, renders Leaflet maps, calls REST APIs, displays role dashboards.
- **Application:** Spring Boot backend. Validates JWT, enforces RBAC, executes business logic, runs `@Scheduled` jobs, sends JavaMail.
- **Data:** PostgreSQL. All entities accessed exclusively via Spring Data JPA. No direct frontend database access.

#### 7.2.1 Integrated System Architecture & User Flow Diagram

The diagram shows all four user flows (Presentation Layer, NGO Flow, Admin Flow, Donor Flow, Automated System Flow) connected through the Application Layer (Spring Boot) and Data Layer (PostgreSQL).

### 7.3 Progressive Web App (PWA) Implementation

The platform is implemented as a Progressive Web App, making it installable on Android and iOS devices directly from the browser without any app store submission. The same React codebase serves both the web and mobile experience.

**PWA vs React Native — Why PWA Was Chosen**

React Native would require a completely separate codebase, Android Studio or Xcode setup, and app store submission adding 4–6 weeks to the timeline with no additional core functionality. PWA delivers an installable mobile experience from the same React codebase in approximately half a day of implementation. It was the correct engineering decision for a 14-week project timeline.

**How Installation Works**

- **Android (Chrome):** Chrome automatically displays an 'Add AI Donation Matcher to Home Screen' banner after the user visits the site. One tap installs it. The app opens without a browser address bar and behaves like a native app.
- **iOS (Safari):** The user taps the Share button in Safari and selects 'Add to Home Screen.' Once installed, the app opens in standalone mode with its own home screen icon.

**What Gets Added to the Codebase**

```json
// manifest.json — tells the phone how to install the app
{
  "name": "AI Donation Matcher",
  "short_name": "DonationApp",
  "start_url": "/",
  "display": "standalone",
  "background_color": "#1F4E79",
  "theme_color": "#2E75B6",
  "icons": [
    { "src": "/icon-192.png", "sizes": "192x192", "type": "image/png" },
    { "src": "/icon-512.png", "sizes": "512x512", "type": "image/png" }
  ]
}
```

```javascript
// service-worker.js — handles offline caching
self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open('adm-cache-v1').then((cache) =>
      cache.addAll(['/', '/index.html', '/static/css/main.css'])
    )
  );
});

// Serve from cache when offline, fetch from network when online
self.addEventListener('fetch', (e) => {
  e.respondWith(
    fetch(e.request).catch(() => caches.match(e.request))
  );
});
```

**Offline Mode Behaviour**

When the donor has no internet connection, the service worker serves the last cached version of the app. The discovery map shows the last loaded NGO data. Previously visited NGO profiles are accessible. Any action requiring server interaction — pledging, marking fulfilled — shows a clear message: 'You are offline. This action requires an internet connection.' When connectivity returns, the app resumes normally.

**PWA Implementation Owner**

Rizwan owns the PWA implementation as an extension of his frontend responsibilities. The manifest.json and service worker are added to the existing React project — no changes to backend, database, or any other team member's code are required. Estimated implementation time: half a day after Tier 1 is complete.

### 7.4 Key Data Models & State Machines

**Need Entity**

```java
public class Need {
    private NeedCategory category;    // FOOD, CLOTHING, MEDICINE, EDUCATION,
                                      // HOUSEHOLD, OTHER
    private String itemName;          // free text
    private String description;       // optional
    private int quantityRequired;     // total needed
    private int quantityPledged;      // sum of all active pledges
    private UrgencyLevel urgency;     // NORMAL, URGENT
    private LocalDate expiryDate;     // optional
    private NeedStatus status;        // OPEN, PARTIALLY_PLEDGED,
                                      // FULLY_PLEDGED, FULFILLED, EXPIRED
    // Derived: quantityRemaining = quantityRequired - quantityPledged
}
```

**Need Status State Machine**

```
OPEN
  |-- donor pledges partial quantity --> PARTIALLY_PLEDGED
  |-- donor pledges full quantity    --> FULLY_PLEDGED
  |-- expiry date reached            --> EXPIRED

PARTIALLY_PLEDGED
  |-- more donors pledge to fill all --> FULLY_PLEDGED
  |-- all pledges expire/cancelled   --> OPEN
  |-- NGO marks fulfilled            --> FULFILLED
  |-- expiry date reached            --> EXPIRED

FULLY_PLEDGED
  |-- all pledges expire/cancelled   --> OPEN
  |-- NGO marks fulfilled            --> FULFILLED

// EXPIRED: all active pledges auto-cancelled, donors notified
// FULFILLED: trust score recalculates, donor receives thank-you email
```

**Trust Score Formula**

```java
public int calculateScore(NGO ngo) {
    int score = 0;
    if (ngo.isVerified())                         score += 40; // weight: 40
    score += (int)(ngo.getProfileCompleteness() * 20);         // weight: 20
    score += Math.min(30, ngo.getFulfilledCount() * 2);        // weight: 30
    if (ngo.getDaysSinceLastActivity() > 60)      score -= 10; // penalty: 10
    return Math.max(0, Math.min(100, score));
}
// Tiers: New = 0-39  |  Established = 40-69  |  Trusted = 70-100
// Recalculates on: admin verification, profile update, fulfillment
```

**Pledge Auto-Expiry Scheduled Job**

```java
@Scheduled(fixedRate = 3600000) // every hour
@Transactional
public void expireOldPledges() {
    LocalDateTime cutoff = LocalDateTime.now().minusHours(48);
    List<Pledge> expired = pledgeRepo
        .findByStatusAndCreatedAtBefore(PledgeStatus.ACTIVE, cutoff);
    for (Pledge p : expired) {
        p.setStatus(PledgeStatus.EXPIRED);
        Need need = p.getNeed();
        need.setQuantityPledged(need.getQuantityPledged() - p.getQuantity());
        needService.recalculateStatus(need);
        emailService.sendPledgeExpiredEmail(p.getDonor());
    }
}
// Same job also checks need expiry dates in a separate loop
```

### 7.5 Data Flow Examples

**A: Donor Discovers NGOs with Filters**

1. Donor sets radius to 5km and selects Clothing category. React sends: `GET /api/ngos?lat=X&lng=Y&radius=5&category=CLOTHING&search=`
2. Spring Security validates JWT. NgoService runs Haversine JPA query with category and search filters.
3. Returns `NgoDiscoveryDTO` array: `{id, name, distanceKm, trustScore, trustTier, topNeedItem, topNeedQuantityRemaining, topNeedUrgency, topNeedCategory, lat, lng, photoUrl}`
4. If result is empty, React auto-calls same endpoint without radius to get nationwide results.
5. Leaflet renders colour-coded pins. List sorted by `distanceKm` ASC, then urgency within 2km bands.

**B: Donor Pledges and Gets Navigation**

6. Donor selects need, enters quantity 5. React sends: `POST /api/pledges {needId, quantity:5, JWT}`
7. `@Transactional`: validate ROLE_DONOR, check email verified, lock need row with `@Lock(PESSIMISTIC_WRITE)`, check remaining >= quantity, create Pledge (expiresAt = now+48h), update `need.quantityPledged += quantity`, recalculate need status, update NGO `lastActivityAt`.
8. Response includes `{pledgeId, ngoLat, ngoLng, ngoAddress, ngoEmail, expiresAt}`
9. React opens Delivery View. Fetches OSRM: `GET https://router.project-osrm.org/route/v1/driving/{donorLng},{donorLat};{ngoLng},{ngoLat}?overview=full&geometries=geojson`
10. Leaflet draws GeoJSON polyline. Distance and duration displayed. Pledge expiry countdown shown.
11. EmailService sends pledge confirmation email to donor via JavaMail.

**C: NGO Marks Fulfilled**

12. NGO clicks Mark as Fulfilled on a need. `PATCH /api/needs/{id}/fulfill` with NGO JWT.
13. NeedService sets status FULFILLED. All active pledges on this need set to FULFILLED.
14. For each fulfilled pledge: EmailService sends thank-you email to donor.
15. `TrustScoreService.calculateScore(ngo)` runs. New score stored on NGO entity.
16. Need moves to fulfilled history. No longer shown in active needs on public profile.

### 7.6 Complete Email Trigger Table

| Trigger Event | Recipient | Email Content |
|--------------|-----------|---------------|
| Registration | Donor | Email verification link. Account locked until clicked. |
| NGO Application Submitted | Admin | New application alert. Link to verification queue. |
| NGO Approved | NGO | Approval with link to complete profile and go live. |
| NGO Rejected | NGO | Rejection with written reason from admin. |
| Pledge Created | Donor | Confirmation with NGO name, address, contact email, 48hr expiry reminder. |
| Pledge Cancelled by Donor | NGO | Donor cancelled. Item, quantity, donor name. |
| Pledge Auto-Expired (48hr) | Donor | Expiry notification. Item still available to re-pledge. |
| Need Expiry Warning (3 days) | NGO | Expiry approaching. Item name and date. |
| Need Auto-Expired | NGO | Need closed. Active pledges on this need cancelled. |
| NGO Marks Fulfilled | Donor | Thank you confirmation. Item, NGO name, date. |
| Report Submitted | Donor | Report received. Review timeline noted. |
| 3 Reports on Same NGO | Admin | Urgent review flag. NGO name and link to report queue. |

### 7.7 Backend Implementation Status (as of March 2026)

The Spring Boot backend service layer is **fully implemented and tested**. All unit tests pass.

**Complete (no changes needed):**

| Layer | Count | Components |
|-------|-------|-----------|
| Entities | 5 | `User`, `Ngo`, `Need`, `Pledge`, `Report` |
| Enums | 7 | `Role`, `NgoStatus`, `NeedCategory`, `NeedStatus`, `PledgeStatus`, `TrustTier`, `UrgencyLevel` |
| Repositories | 5 | `UserRepository`, `NgoRepository`, `NeedRepository`, `PledgeRepository`, `ReportRepository` |
| DTOs (records) | 9 | `RegisterRequest`, `LoginRequest`, `LoginResponse`, `NgoProfileRequest`, `NgoDiscoveryDTO`, `NeedRequest`, `PledgeRequest`, `PledgeResponse`, `ReportRequest` |
| Services | 11 | `AuthService`, `NgoService`, `NeedService`, `PledgeService`, `AdminService`, `ReportService`, `TrustScoreService`, `JwtService`, `EmailService`, `CloudinaryService`, `ScheduledJobService` |
| Config | 4 | `ApplicationConfig`, `SecurityConfig`, `JwtFilter`, `CloudinaryConfig` |
| Unit Tests | 69 | All passing — 11 test classes, one per service |

**Remaining backend work:**

| Item | Description |
|------|-------------|
| `AuthController` | POST /api/auth/register, POST /api/auth/login, GET /api/auth/verify-email, POST /api/auth/resend-verification |
| `NgoController` | GET/PATCH /api/ngo/profile, GET /api/ngos (discovery), GET /api/ngos/{id} |
| `NeedController` | POST/PUT/DELETE /api/needs, PATCH /api/needs/{id}/fulfill |
| `PledgeController` | POST /api/pledges, DELETE /api/pledges/{id}, GET /api/pledges/active, GET /api/pledges/history |
| `AdminController` | GET/POST /api/admin/ngos/pending, PATCH approve/reject/suspend, GET/PUT/DELETE /api/admin/needs, GET /api/admin/reports, GET /api/admin/stats |
| `ReportController` | POST /api/ngos/{id}/report |
| `GlobalExceptionHandler` | `@RestControllerAdvice` — catches `RuntimeException`, `BadCredentialsException`, validation errors |

**Endpoint security (SecurityConfig):**
- `GET /api/auth/**` — public
- `GET /api/ngos/**` — public
- `/api/admin/**` — ROLE_ADMIN only
- `/api/ngo/**` — ROLE_NGO only
- `/api/pledges/**` — ROLE_DONOR only
- Everything else — authenticated
- CORS origin: `http://localhost:5173` (Vite dev) + Vercel production URL

---

## 8. Team Task Division

Vertical ownership model. Each member owns a feature domain end-to-end from Spring Boot service layer to React frontend. Individual contributions are clearly demonstrable during panel review.

| Member | Domain | Backend Deliverables | Frontend Deliverables |
|--------|--------|---------------------|----------------------|
| **Dinesh** | Security & Admin | ~~Spring Security config, JWT generation/validation, RBAC 3 roles~~ ✅ Complete. ~~AdminService: NGO approval/rejection, suspension cascade, need moderation, report queue, stats~~ ✅ Complete. **Remaining:** AuthController, AdminController, GlobalExceptionHandler | Login/Registration forms (role selector), email verification screen, admin dashboard: verification queue, report queue, NGO management with suspend/edit |
| **Gowtham** | Geospatial & Maps | ~~Haversine native JPA query with category/search/radius params~~ ✅ Complete. ~~NgoDiscoveryDTO with distanceKm + topNeed + trustTier + photoUrl~~ ✅ Complete. **Remaining:** NgoController (discovery endpoint) | Leaflet map, colour-coded pins, pin popups, radius slider, category dropdown filter, NGO search bar, Delivery View with OSRM route polyline |
| **Sriram** | NGO & Trust Engine | ~~NgoService, NeedService, TrustScoreService, CloudinaryService~~ ✅ Complete. ~~Profile completion gate, Nominatim geocoding, ScheduledJobService (need expiry)~~ ✅ Complete. **Remaining:** NeedController, NgoController (profile endpoints) | NGO dashboard (needs management + incoming pledges), NGO profile completion screen with progress bar, public NGO profile with active needs + fulfilled history |
| **Rizwan** | Donor, Pledge & PWA | ~~PledgeService (@Transactional + PESSIMISTIC_WRITE lock), ScheduledJobService (pledge auto-expiry), EmailService (all 12 triggers via JavaMail)~~ ✅ Complete. **Remaining:** PledgeController, ReportController | Donor pledge screen with quantity selector, Delivery View navigation screen, donor dashboard (active pledges + history tabs), shared Tailwind CSS design system, PWA manifest.json + service worker + offline cache |

**Shared Responsibilities:**
PostgreSQL ER schema design — Week 1, before any feature work | REST API contract documentation agreed before parallel development | Seed data setup: 2 verified NGOs, 3 donor accounts, sample needs and pledges for demo | Final deployment: Vercel + Render + Supabase/Neon | End-to-end demo run-through and panel preparation

**Critical Path Dependency:**
Dinesh's authentication module must be complete before any other member can test against a secured live API. Mitigated by each member using mock authentication tokens locally during the early phases. Real JWT integration is wired up during the full integration week (Week 11).

### 8.1 Note on AI-Assisted Development

AI coding tools were used to accelerate boilerplate generation and debug specific implementations including the Haversine JPA query, JWT filter chain configuration, and JavaMail SMTP setup. All architecture decisions, feature design, data model design, and core business logic were made by the team and are fully understood. Every component can be walked through line by line during the panel review.

---

## 9. Build Priority Order

This section defines the exact sequence in which the platform must be built. Follow this order strictly. The rule is absolute and non-negotiable.

### The Non-Negotiable Rule

**NEVER start Tier 2 until every Tier 1 task runs clean end-to-end on the live deployed URL.** Not locally. Not with mock data. On the live URL with real JWT authentication.

**NEVER start Tier 3 until Tier 2 is complete and tested with seed data.**

If the panel day arrives and only Tier 1 is complete — you still have a working, demonstrable, fully defensible prototype that covers the core value proposition.

### Tier 1 — Core Loop (Must Ship)

| # | Task | Owner | Priority | Status |
|---|------|-------|----------|--------|
| 1 | User registration + email verification (Donor and NGO flows) | Dinesh | P0 | ✅ Service done — AuthController needed |
| 2 | JWT login + role-based access control for 3 roles | Dinesh | P0 | ✅ Service + SecurityConfig done — AuthController needed |
| 3 | Admin verifies NGO: approve/reject from pending queue | Dinesh | P0 | ✅ Service done — AdminController needed |
| 4 | NGO profile completion gate + go-live when complete | Sriram | P0 | ✅ Service done — NgoController needed |
| 5 | NGO posts, edits, and closes a need (category + quantity) | Sriram | P0 | ✅ Service done — NeedController needed |
| 6 | Donor discovers NGOs on map via Haversine JPA query | Gowtham | P0 | ✅ Service done — NgoController discovery endpoint needed |
| 7 | Donor pledges against a need (single donor, full quantity) | Rizwan | P0 | ✅ Service done — PledgeController needed |
| 8 | NGO marks fulfilled + trust score recalculates | Sriram | P0 | ✅ Service done — NeedController fulfill endpoint needed |

### Tier 2 — Extended Prototype (Build After Tier 1 is Live)

| # | Task | Owner | Priority | Status |
|---|------|-------|----------|--------|
| 9 | Pledge auto-expiry @Scheduled job (48hr threshold) | Rizwan | P1 | ✅ ScheduledJobService done |
| 10 | Donor can cancel their own pledge anytime | Rizwan | P1 | ✅ PledgeService done — PledgeController needed |
| 11 | Split pledging: quantity tracking + @Transactional | Rizwan | P1 | ✅ PledgeService + PESSIMISTIC_WRITE lock done |
| 12 | Email notifications — pledge confirmation + fulfillment (JavaMail) | Rizwan | P1 | ✅ All 12 triggers in EmailService done |
| 13 | Category filter + radius slider on discovery map | Gowtham | P1 | ✅ NgoRepository.findNearby() supports params — frontend needed |
| 14 | Colour-coded pins by category on Leaflet map | Gowtham | P1 | ✅ topNeedCategory in NgoDiscoveryDTO — frontend needed |
| 15 | NGO search bar — name search overrides radius | Gowtham | P1 | ✅ search param in NgoService.discoverNgos() done |
| 16 | Pin popup with top need + remaining quantity | Gowtham | P1 | ✅ NgoDiscoveryDTO includes top need fields — frontend needed |
| 17 | Post-pledge delivery view: Leaflet + OSRM road route | Gowtham | P1 | ✅ PledgeResponse includes ngoLat, ngoLng, ngoAddress — frontend needed |
| 18 | Donor dashboard: active pledges tab + history tab | Rizwan | P1 | ✅ PledgeService.getActivePledges() + getPledgeHistory() done — PledgeController needed |
| 19 | NGO fulfilled history on public profile | Sriram | P1 | ✅ NeedRepository queries support FULFILLED status filter |
| 20 | NGO profile photo upload via Cloudinary free tier | Sriram | P1 | ✅ CloudinaryService + NgoService.updatePhotoUrl() done |
| 21 | Need expiry date + auto-close @Scheduled job | Sriram | P1 | ✅ ScheduledJobService.processNeedExpiry() done |
| 22 | Donor report NGO + report queue in admin dashboard | Dinesh | P1 | ✅ ReportService done — ReportController + AdminController needed |
| 23 | Admin moderation: edit or remove any NGO need | Dinesh | P1 | ✅ AdminService.editNeed() + removeNeed() done — AdminController needed |
| 24 | Suspension cascade — atomic @Transactional operation | Dinesh | P1 | ✅ AdminService.suspendNgo() done — AdminController needed |
| 25 | Default list sort: distance ASC + urgency secondary | Gowtham | P1 | ✅ NgoRepository native query orders by distance_km — frontend list sort needed |
| 26 | PWA manifest.json + app icons (makes app installable on Android and iOS) | Rizwan | P1 | Frontend task |
| 27 | Service worker + basic offline cache (last loaded map + visited NGO profiles) | Rizwan | P1 | Frontend task |

---

## 10. Limitations & Future Scope

### 10.1 Honest Limitations of the Prototype

- Physical delivery is not verified by the system. Fulfillment is recorded when the NGO marks receipt. The platform cannot independently confirm goods arrived.
- Trust score is an assistive indicator from structured inputs. It is not a fraud-detection system or legal certification of legitimacy.
- Admin verification is manual. Document authenticity is assessed by a human reviewer, not automated analysis.
- OSRM public demo server is used for routing. Suitable for prototype and demonstration. A self-hosted OSRM instance is required for production reliability.
- Donor-NGO handover coordination happens via shared email outside the platform. No in-app messaging in prototype.
- Prototype operates at seed-data scale. Production-volume performance has not been tested.
- English only. No multilingual support in prototype.

### 10.2 Future Scope (Tier 3)

- **Volunteer Coordination Layer:** Community members optionally assist with delivery. Task board, claim workflow, @Scheduled expiry for abandoned claims. Design fully specified.
- **OTP-Based Handover Confirmation:** One-time code at drop-off to confirm physical handover.
- **In-App Coordination Messaging:** Transaction-scoped temporary chat between donor and NGO, closing at fulfillment.
- **Smart Notification Matching:** Category-based donor alerts matching past giving history to new NGO requests.
- **Admin Analytics Dashboard:** Fulfillment rates, category trends, pledge-to-fulfillment timing.
- **NGO Analytics:** Profile view counts and need impression counts.
- **Multilingual Support:** Tamil and Hindi via React i18next.
- **Mobile Application (React Native):** Full native Android and iOS app with push notifications. Separate codebase from the PWA. App Store and Google Play Store submission required. Appropriate after the PWA has been validated with real users.

---

## 11. Panel Preparation — Questions & Answers

These are anticipated panel questions and the team's prepared responses, grounded in specific decisions made during the design process. Start every answer directly — never begin with 'Good question.'

| Panel Question | Prepared Response |
|---------------|-------------------|
| **How is this different from a basic NGO directory?** | A directory is static, unverified, and shows no real-time needs. This platform adds: admin-gated NGO verification, real-time categorised need-posting with quantities, split pledging across multiple donors, a rule-based trust score, in-app OSRM navigation to the NGO, and a pledge lifecycle with auto-expiry. A directory tells you an NGO exists. This platform tells you which verified nearby NGO needs exactly what right now, and guides you there. |
| **What exactly is the AI component?** | AI is not synonymous with machine learning. The Trust Scoring Engine makes automated multi-factor credibility assessments that would otherwise require a human to review documents, activity patterns, and fulfillment records. We chose rule-based AI over ML for three reasons: insufficient training data at prototype scale; the necessity for full auditability in a trust-sensitive domain; and honesty about the system's capabilities. We can explain exactly why any NGO received its specific score. |
| **Why only two active user roles? No volunteers?** | Volunteer coordination was removed after identifying that task abandonment creates a state machine edge case we cannot reliably resolve without real users. The core value is fully intact: donors discover verified NGOs, pledge against real needs, and deliver directly. Volunteer coordination is documented as the highest-priority future scope item with the @Scheduled expiry mechanism already fully designed and specified. |
| **What if a donor pledges but never delivers?** | A Spring @Scheduled job runs every hour and checks all ACTIVE pledges. Any pledge older than 48 hours is automatically expired: pledged quantity is restored to the need, need status recalculates, and the donor receives an email notification. This is the same pattern used in e-commerce cart reservation systems. |
| **What prevents two donors from over-pledging simultaneously?** | Pledge creation uses `@Lock(PESSIMISTIC_WRITE)` on the need row inside a `@Transactional` Spring operation. When a donor submits a pledge, the service acquires a database-level lock on that need row before checking and subtracting quantity. If two donors submit at the same moment, one waits while the other holds the lock — preventing over-pledging at the database row level. |
| **How do you verify physical delivery?** | We do not — and we are transparent about this. Fulfillment is recorded when the NGO marks receipt. The trust score penalises NGOs with low fulfillment rates. Admins can investigate and suspend fraudulent accounts. OTP handover confirmation in future scope would add lightweight physical verification without new infrastructure. |
| **Why not use PostGIS for location queries?** | For prototype-scale data, a Haversine formula in a native Spring Data JPA query achieves identical results with zero additional database configuration. PostGIS is appropriate when spatial indexing is needed for performance at scale — a decision that should be driven by real usage data, not made speculatively. |
| **Isn't a monolith just a shortcut?** | No — it is a deliberate, right-sized decision. All three roles share the same core entities: NGO, Need, and Pledge. Splitting into microservices would require distributed transactions and an API gateway with no user-facing benefit at this scale. Spring Boot's Controller-Service-Repository layers provide equivalent logical separation. Horizontal scaling behind a load balancer is available when real usage data justifies it. |
| **Why cap NGOs at 5 active needs?** | The cap is a quality control mechanism. Uncapped posting leads to stale, unmanaged listings that degrade donor trust and map quality. Five needs covers all realistic simultaneous categories for a grassroots organisation and forces NGOs to actively maintain their listings. |
| **What happens when admin suspends an NGO?** | Suspension runs as a single atomic @Transactional operation: NGO status set to SUSPENDED, all active needs closed, all active pledges cancelled, all affected donors notified by email immediately, NGO removed from donor map. No partial suspension states exist — the operation is all-or-nothing. |
| **The OSRM routing — is it reliable enough?** | The OSRM public demo server is appropriate for prototype and demonstration purposes. For production, we would self-host an OSRM instance on a VPS or migrate to a commercial routing API. This is a known, documented limitation with a clear upgrade path — not an architectural flaw. |
| **Is there a mobile app?** | Yes. The platform is implemented as a Progressive Web App. On Android, Chrome automatically prompts the user to install it to their home screen. On iOS, users add it via Safari's Share menu. Once installed it opens without a browser address bar and behaves like a native app. The same React codebase serves both web and mobile — no duplicate development was needed. |
| **Why PWA instead of React Native?** | React Native would require a completely separate codebase, Android Studio or Xcode setup, and app store submission — adding 4 to 6 weeks to our timeline with no additional core functionality. PWA delivers an installable mobile experience from the same React codebase in approximately half a day of implementation. For our 14-week timeline it was the correct engineering decision. A dedicated React Native app is documented in future scope. |
| **Does the app work offline?** | Basic offline support is implemented via a service worker. The last loaded map and previously visited NGO profiles are served from cache when connectivity drops. Actions that require server interaction — pledging, marking fulfilled — display a clear offline message. Full offline sync with background data refresh is documented in future scope. |
| **How did AI tools contribute to development?** | AI tools accelerated boilerplate generation and helped debug specific implementations including the Haversine query, JWT filter chain, and JavaMail configuration. All architecture decisions, feature design, data modelling, and business logic were designed by the team. We can walk through every component in detail. |

---

**End of Document | AI Donation Matcher | Complete Final Project Documentation**
