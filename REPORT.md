# Operation Ninja T Frog — Final Engineering Report

**Project:** Sound Marketplace Backend Modernization  
**Repository:** `sound-marketplace`  
**Execution Period:** Phases 0 through 8 Complete  
**Target Branches:** `ntf/phase-1-observability` through `ntf/phase-8-proof-package`  

---

## 1. Executive Summary & Architecture Overview

Operation Ninja T Frog systematically resolved 30 critical architectural, security, payment, data integrity, and performance vulnerabilities across the Sound Marketplace Spring Boot application. 

The application was transitioned from an insecure, unindexed, N+1 query-laden architecture with hardcoded secrets, raw file uploads, and client-controlled pricing into a hardened, high-throughput, observable, and fully gated digital marketplace.

### Key Architectural Upgrades
1. **End-to-End Observability:** Distributed request tracing via SLF4J MDC (`X-Request-Id`, `userId`), AOP execution timing (`TRACE_ENTER`, `TRACE_EXIT`), and structured external call logging (`EXT_CALL system=...`).
2. **Hardened Security & RBAC:** SHA-256 hashed one-time tokens, strict registration role whitelisting (`consumer`, `music_artist`, `producer`), sensitive data redaction in logging, and ownership verification on all resource mutations.
3. **Financial Integrity:** Server-side pricing enforcement in integer cents, pessimistic inventory locking, automated compensation/refund triggers on oversell, and idempotent Stripe webhook ingestion.
4. **Relational Integrity & Migrations:** Flyway schema evolution (`V1` through `V4`) introducing composite, foreign key, and full-text indexes, eliminating duplicate relationships and enforcing referential integrity.
5. **High-Performance Catalog:** Lightweight constructor JPQL projections (`TrackSummaryDTO`, `AlbumSummaryDTO`, `MerchandiseSummaryDTO`) that strip BLOBs from list responses, reducing payload sizes by >99.8%. Capped pagination (default 20, max 100) and HTTP 304 ETag caching.
6. **Media Storage Abstraction & Gated Playback:** Pluggable `StorageService` (`LocalStorageService` and `S3StorageService`), Apache Tika magic-bytes validation, ffprobe binary duration probing, ffmpeg AAC transcoding, and HMAC-SHA256 signed URL gated playback supporting HTTP 206 Partial Content.

---

## 2. Traceability Matrix (NTF-01 through NTF-30)

| Vulnerability ID | Vulnerability Summary | Fix Summary | Commits | Test Suite(s) | Trace Scenario / Log Line |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **NTF-01** | Missing ownership verification on track & merchandise edit/delete | Enforced caller ownership matching entity owner ID; returns 403 Forbidden | `ntf/phase-3-ownership` | `OwnershipIT` | `forbidden-edit.log`: `CatalogServiceImpl - EVENT ownership.denied` |
| **NTF-02** | Registration allowed elevation to `admin` / `super_admin` | Whitelisted roles to `consumer`, `music_artist`, `producer`; rejects elevated roles with 400 | `ntf/phase-2-security` | `RegistrationRolesIT` | `register-verify-login.log`: `EVENT auth.register.rejected reason=invalid_role` |
| **NTF-03** | Password reset leaked raw token in API response and plaintext DB | Returned generic 202; stored only SHA-256 hash in DB; sent token via email | `ntf/phase-2-security` | `PasswordResetIT` | `forgot-password.log`: `EVENT auth.password_reset.requested genericResponse=true` |
| **NTF-04** | Email verification token stored in plaintext in DB | Generated secure random token and stored SHA-256 hash in DB | `ntf/phase-2-security` | `AuthLoginIT` | `register-verify-login.log`: `EVENT auth.register.token_hashed` |
| **NTF-05** | Server-side file path injection in `POST /api/tracks` | Removed `POST /api/tracks` JSON endpoint; purged `filePath` from DTOs; required multipart upload | `ntf/phase-7-media-storage` | `ServerFileIngestionRemovedIT` | `file-ingest-blocked.log`: `HTTP_OUT 405` |
| **NTF-06** | Checkout allowed client to dictate price / total amount | Server ignores client amount; recalculates total directly from DB item prices | `ntf/phase-5-payments` | `CheckoutPricingIT` | `checkout-webhook.log`: `EVENT order.created clientAmountIgnored=true` |
| **NTF-07** | Currency and price handled as float/double prone to rounding errors | Converted all order, price, and payment representations to integer cents (`long`) | `ntf/phase-5-payments` | `CheckoutPricingIT` | `checkout-webhook.log`: `totalCents=1500` |
| **NTF-08** | Payment status updated without verifying webhook signature | Enforced Stripe HMAC-SHA256 signature verification on all webhook payloads | `ntf/phase-5-payments` | `WebhookSignatureIT` | `checkout-webhook.log`: `OrderService - EVENT order.paid` |
| **NTF-09** | Duplicate webhook events could re-credit orders or grant double entitlements | Created `stripe_events` idempotency table; duplicate event IDs return 200 immediately | `ntf/phase-5-payments` | `WebhookIdempotencyIT` | `checkout-webhook.log`: `OrderService - EVENT webhook.duplicate` |
| **NTF-10** | Any user could delete or modify other users' comments | Implemented author ownership checks in `updateComment` and `deleteComment` | `ntf/phase-3-ownership` | `CommentEditDeleteIT` | `comment-edit.log`: `EngagementServiceImpl - EVENT comment.updated` |
| **NTF-11** | Cart item duplicate entries caused unexpected quantities | Added unique composite database constraints `uk_cart_merch` and `uk_cart_track` | `ntf/phase-4-data-integrity` | `StockConcurrencyIT` | `StockConcurrencyIT` DB constraint tests |
| **NTF-12** | Entitlements not granted upon successful payment | Implemented automatic entitlement creation for all tracks in paid orders | `ntf/phase-5-payments` | `AlbumEntitlementIT` | `checkout-webhook.log`: `OrderService - EVENT entitlement.granted` |
| **NTF-13** | Race conditions allowed merchandise stock overselling | Added `PESSIMISTIC_WRITE` locks on merchandise; triggers automatic refund when stock is 0 | `ntf/phase-5-payments` | `StockConcurrencyIT` | `oversell.log`: `EVENT order.oversold ... refund.initiated` |
| **NTF-14** | Unbounded database queries on track and merchandise list endpoints | Enforced `PageResponse` with default size 20 and max ceiling 100 | `ntf/phase-6-performance` | `PaginationIT` | `tracks-page.log`: `PageRequest{page=0, size=20}` |
| **NTF-15** | Play count and like count computed via expensive `COUNT(*)` queries | Added denormalized columns updated atomically via JPQL `UPDATE` | `ntf/phase-6-performance` | `CountersIT` | `TrackListPerformanceIT`: `sqlCount=2` |
| **NTF-16** | Track and album list endpoints returned full binary blobs (MBs per page) | Created slim `TrackSummaryDTO` and `AlbumSummaryDTO` via JPQL constructor expressions | `ntf/phase-6-performance` | `NoBlobInResponsesIT` | `tracks-page.log`: `bytes=5842 sqlCount=2` |
| **NTF-17** | Cover images and avatars served without caching headers | Added `/cover`, `/avatar`, `/image` endpoints with MD5 ETag and 7-day Cache-Control | `ntf/phase-6-performance` | `ImageCachingIT` | `ImageCachingIT`: `304 Not Modified` |
| **NTF-18** | Streaming endpoints exposed raw internal byte arrays | Removed `/{id}/stream` and `/audio` byte[] routes; replaced with gated Range stream | `ntf/phase-7-media-storage` | `StreamSignatureIT` | `stream-gated.log`: `HTTP_OUT 206 bytes=1024` |
| **NTF-19** | Audio playback lacked authorization gating | Created `/api/tracks/{id}/play` yielding HMAC-SHA256 signed URLs (full vs preview) | `ntf/phase-7-media-storage` | `PlayDecisionTest` | `stream-gated.log`: `EVENT stream.gate.decision` |
| **NTF-20** | Comment update/delete parameter order caused target ID mixups | Reordered arguments to `(userId, commentId, commentText)` in service layer | `ntf/phase-3-ownership` | `CommentEditDeleteIT` | `comment-edit.log`: `args=[101, 12, ...]` |
| **NTF-21** | Play queue state lost across client sessions | Created `user_queue_items` and `user_player_state` schema and entity models | `ntf/phase-4-data-integrity` | `PlaylistControllerTest` | `PlaylistControllerTest` queue persistence |
| **NTF-22** | Unindexed full table scans on comments, history, and search | Added composite and full-text indexes (`ft_tracks_search`, `idx_comments_track_created`) | `ntf/phase-4-data-integrity` | `FlywayMigrationIT` | Schema verification tests |
| **NTF-23** | User relationship table allowed duplicate follower/following pairs | Added `uk_follow` unique constraint on `(follower_id, followed_id)` | `ntf/phase-4-data-integrity` | `FollowIT` | `FollowIT` duplicate follow rejection |
| **NTF-24** | Play counts could be artificially inflated via rapid repeat requests | Added Caffeine in-memory cache deduplicating play events within 30 minutes | `ntf/phase-6-performance` | `CountersIT` | `CountersIT`: play count increment ignored within window |
| **NTF-25** | Hardcoded secrets and JWT signing keys in source code | Replaced all secrets with externalized configuration and environment variable defaults | `ntf/phase-2-security` | `ConfigSafetyTest` | `ConfigSafetyTest`: clean secret scan |
| **NTF-26** | Logs leaked sensitive user passwords and verification tokens | Implemented Logback masking and safe `ArgumentSummarizer` | `ntf/phase-2-security` | `LogRedactionIT` | `LogRedactionIT`: verification token masked in logs |
| **NTF-27** | Media stored directly as database byte blobs | Migrated storage to `StorageService` and created admin blob export job | `ntf/phase-7-media-storage` | `BlobExportIT` | `BlobExportIT`: DB blob null, storage key populated |
| **NTF-28** | Lack of centralized API error envelope | Standardized on `ApiResponse<T>` with `requestId` and HTTP status mapping | `ntf/phase-1-observability` | `ErrorHandlingIT` | `ErrorHandlingIT`: unified JSON error responses |
| **NTF-29** | Insecure default CORS policy allowing arbitrary origins | Restricted CORS configuration with configurable whitelist (`cors.origins`) | `ntf/phase-2-security` | `CorsIT` | `CorsIT`: unauthorized origins rejected |
| **NTF-30** | Arbitrary file upload vulnerabilities (executable spoofing, oversize files) | Apache Tika magic bytes MIME detection, size caps, and uploader role checks | `ntf/phase-7-media-storage` | `UploadValidationIT` | `UploadValidationIT`: `.exe` as `.mp3` returns 415 |

---

## 3. Performance Benchmarks: Before vs. After (from `docs/perf.md`)

| Benchmark Metric | Baseline (Pre-Phase 6) | Phase 6 & 7 Optimized | Net Improvement |
| :--- | :--- | :--- | :--- |
| **Track List DTO Payload** | Full `TrackDTO` with embedded BLOBs & paths | `TrackSummaryDTO` (IDs, names, counters) | **Zero BLOBs in JSON** |
| **Single Item Payload** | ~200 KB – 500 KB | ~250 bytes | **> 99.8% reduction** |
| **Page Payload (20 tracks)** | **4 MB – 10 MB** (unpaged, full BLOBs) | **~5.8 KB** (`PageResponse<TrackSummaryDTO>`) | **~99.9% bandwidth saving** |
| **Max Response Size Cap** | Unbounded (scales linearly with DB size) | Strictly capped at `< 50 KB` (max 100 tracks) | **Guaranteed bounds** |
| **Average Latency (50 VUs)** | ~340 ms | **18 ms** | **18.8x faster** |
| **p95 Latency** | **~850 ms** | **48 ms** | **17.7x faster** |
| **Throughput (RPS)** | ~120 req/s | **1,850+ req/s** | **15.4x throughput** |
| **SQL Queries per Page** | N+1 queries (20+ queries per page) | **2 queries** (1 paged projection + 1 count) | **90% fewer DB hits** |
| **Image Cache Hit Latency** | Re-downloaded full MBs every request | **< 8 ms** (`304 Not Modified`, 0 payload bytes) | **Instant response** |

---

## 4. API Migration Guide for the Client (Flutter)

### 4.1 Removed Routes & Replacements
- **Removed:** `POST /api/tracks` (JSON creation with `filePath`).  
  **Replacement:** `POST /api/tracks/upload` (Multipart form-data: `audioFile`, `coverArt`, `title`, etc.).
- **Removed:** `GET /api/tracks/{id}/stream` and `GET /api/tracks/{id}/audio` (raw byte arrays).  
  **Replacement:**  
  1. Call `GET /api/tracks/{id}/play` (or batch `POST /api/tracks/play-urls`) to receive `{ url, expiresAt, isPreview }`.  
  2. Stream via `GET /api/stream/{id}?v=full|preview&exp=...&sig=...` supporting HTTP 206 Partial Content.
- **Removed:** `GET /api/tracks/{id}/video` (raw byte array).  
  **Replacement:** Use signed URL from `/api/tracks/{id}/play` pointing to `/api/stream/{id}`.
- **Removed:** Unpaged `GET /api/tracks`, `GET /api/albums`, `GET /api/merchandise`.  
  **Replacement:** Use `page` (0-indexed) and `size` query params; responses wrapped in `PageResponse<T>`.

### 4.2 Changed Request & Response Shapes
- **List Endpoints:** Return `PageResponse<TrackSummaryDTO>` instead of flat `List<TrackDTO>`.
- **Checkout:** `POST /api/checkout` accepts `items: [{type: 'TRACK'|'ALBUM'|'MERCHANDISE', id, quantity}]`. Client amounts are ignored.
- **Images:** Track/Album/Merchandise cover images are served from dedicated endpoints (`/cover`, `/image`) and should be rendered using `Image.network` (with native caching) rather than `Image.memory`.

---

## 5. Deviations from Specification & Technical Rationale

1. **Dual Storage Implementation:** In Phase 7, `LocalStorageService` was configured as the default implementation for local and development profiles, with `S3StorageService` available for AWS deployments. This enables development, testing, and CI pipelines to execute cleanly without external cloud credentials.
2. **Graceful FFmpeg Fallback in Tests:** To support automated test runners and CI environments lacking local `ffmpeg`/`ffprobe` binaries, `FfmpegMediaTranscoder` and `FfprobeDurationProbe` implement test-friendly fallbacks that mark uploads as `READY` with master stream keys, while production environments continue to enforce transcoding pipelines.

---

## 6. Known Gaps & Follow-Up Recommendations

1. **Stripe Connect Payouts:** While buyer payments and stock reservation are fully implemented, artist revenue sharing and automated payouts via Stripe Connect should be scheduled for future milestone release.
2. **Server-Side Queue Sync Endpoints:** Database tables (`user_queue_items`, `user_player_state`) and entities are provisioned in Flyway migrations; client state synchronization controllers should be integrated alongside the Flutter Phase 9 rollout.
3. **Legacy Column Deprecation:** The `merchandise.quantity` column has been superseded by `stock_quantity`; `merchandise.quantity` should be formally dropped in a future Flyway migration once existing legacy queries are fully retired.

---

## 7. Verification & Run Commands

All verification commands must be run from the repository root:

```bash
# 1. Run all unit and slice tests
./sound-marketplace/mvnw test

# 2. Run full verification and integration test suite with JaCoCo check
./sound-marketplace/mvnw verify

# 3. Start local containerized production environment
docker compose up -d

# 4. Execute all 9 trace demo scenarios
bash ./scripts/trace-demo.sh all
```
