# NGO Address Geocoding

Last updated: 2026-03-30

## Provider

The backend currently uses OpenStreetMap Nominatim for NGO address geocoding.

Config:
- `geocoding.provider=nominatim`
- `geocoding.nominatim.base-url`
- `geocoding.nominatim.user-agent`

Default base URL:
- `https://nominatim.openstreetmap.org`

## Trigger behavior

Geocoding is triggered whenever an NGO profile save includes the `address` field:
- `PUT /api/ngo/my/profile`

The backend resolves the address to coordinates and stores:
- `lat`
- `lng`

Resolution strategy:
- try the full submitted address first
- if that returns no result, progressively retry broader comma-separated area segments
- example: `Door No 12, Mulapet, Nellore` -> `Mulapet, Nellore` -> `Nellore`
- this allows approximate regional placement when the exact street-level address is not available

Those coordinates remain available in existing NGO responses used by the frontend, including:
- `GET /api/ngo/my/profile`
- `GET /api/ngos/{id}`
- discovery responses that expose NGO coordinates

## Current creation-flow note

The current NGO account registration flow does not collect the structured NGO profile address used for map geocoding.

That means the first geocoding event normally happens when the NGO saves its profile address for the first time through `PUT /api/ngo/my/profile`.

## Failure behavior

Geocoding is now fail-fast for non-blank addresses.

If the provider:
- returns no result for the full address and all broader fallback queries, or
- is temporarily unavailable, or
- the address cannot be parsed into coordinates

the backend rejects the profile update with a runtime error and does not persist the new address change.

Typical messages:
- `Unable to geocode the provided NGO address. Please enter a more specific address.`
- `Address geocoding is temporarily unavailable. Please try again.`

If the address is intentionally cleared to blank, the backend clears `lat` and `lng` as well.

## Retry and rate-limit behavior

The backend currently performs one external lookup per candidate query during a profile save.

Because broader fallbacks are now enabled, a single save may try multiple queries from most specific to least specific until one succeeds or all fail.

There is no repeated retry loop against the same candidate query. This is intentional to avoid aggressive retry behavior against Nominatim rate limits and to keep API responses predictable.

Frontend guidance:
- avoid rapid repeated profile-save attempts for the same invalid address
- preserve entered form values if geocoding fails so the user can refine the address and retry

## Swagger and contract impact

No new request fields were added.

Documented contract changes:
- `NgoProfileRequest.address` now explicitly notes automatic backend geocoding
- frontend/backend agreement documents:
  - provider used
  - failure behavior
  - no-retry behavior
  - continued availability of `lat` and `lng` in NGO responses
