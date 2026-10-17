# Changelog - Operation Ninja T Frog

All notable changes across Operation Ninja T Frog (Phases 0 through 8) are documented here.

## [Phase 8] - Proof Package & Distribution
- **Scripts**: Created `scripts/trace-demo.sh` automating the 9 verification scenarios with `X-Request-Id` correlation.
- **Containerization**: Authored multi-stage `Dockerfile` (JDK 21 build -> JRE 21 lightweight non-root runtime) and `docker-compose.yml` orchestrating MySQL 8.0, Stripe-Mock, and backend service.
- **API Specification**: Authored `openapi.yaml` documenting all modern REST endpoints, DTO contracts, security schemes, and status codes.
- **Test Reports & Trace Logs**: Captured full end-to-end trace logs (`reports/trace/*.log`), `logs/app.json`, and JaCoCo coverage reports.
- **Packaging**: Produced final distribution zip archives in `dist/`.

## [Phase 7] - Media Storage & Gated Streaming
- **Storage Abstraction**: Introduced `StorageService` interface with `LocalStorageService` (root configurable) and `S3StorageService` (AWS SDK v2), with `EXT_CALL system=storage op=...` observability logging.
- **Input Validation (NTF-30)**: Validated uploads using Apache Tika magic bytes detection for audio (`mp3`, `wav`, `flac`, `m4a`, `ogg`), video (`mp4`), and images (`jpeg`, `png`, `webp`). Added file size limit enforcement (413 Payload Too Large) and role validation (403 Forbidden).
- **Duration Probing**: Integrated `DurationProbe` using `ffprobe` to compute audio duration directly from binary headers, ignoring client-submitted claims.
- **Transcoding Pipeline**: Introduced `MediaTranscoder` producing `stream.m4a` (AAC 256k), `preview.m4a` (30 s preview), and `waveform.json` (800 peaks), updating track state `PROCESSING` -> `READY`/`FAILED`.
- **Blob Export (NTF-27 partial)**: Created idempotent admin endpoint `POST /api/admin/media/export-blobs` exporting DB byte blobs to external storage keys and nulling DB columns.
- **Gated Playback & Signed Streaming (NTF-18, NTF-19)**: Created `GET /api/tracks/{id}/play` and `POST /api/tracks/play-urls` yielding signed HMAC-SHA256 URLs (`/api/stream/{id}?v=full|preview&exp=...&sig=...`). Full stream granted for free tracks, track owners, or verified entitlements; preview otherwise.
- **HTTP 206 Partial Content**: Stream endpoint supports HTTP `Range: bytes=start-end` requests, returning `206 Partial Content` with `Content-Range` and `Cache-Control: private, max-age=300`. Removed legacy byte array streaming routes (`/{id}/stream`, `/{id}/audio`, `/{id}/video`).
- **Server File Ingestion Removal (NTF-05)**: Removed `POST /api/tracks` JSON endpoint and purged `filePath`, `videoFilePath`, `coverImageBlob`, `videoBlob` from `CreateTrackRequest`.

## [Phase 6] - Performance Optimization
- **Payload Minimization (NTF-16)**: Introduced slim summary DTOs (`TrackSummaryDTO`, `AlbumSummaryDTO`, `MerchandiseSummaryDTO`) utilizing JPQL constructor projections, reducing list payload sizes by >99.8%.
- **Pagination Standards (NTF-14)**: Added `PageResponse<T>` and `PaginationUtil` enforcing default page size of 20 and maximum cap of 100 items across all list endpoints.
- **Image Caching (NTF-17)**: Implemented dedicated `/cover`, `/avatar`, and `/image` endpoints with MD5 ETag generation, returning `304 Not Modified` and `Cache-Control: public, max-age=604800`.
- **Atomic Counters & Deduplication (NTF-15, NTF-24)**: Replaced count-query calculation with atomic `UPDATE` queries for like count and play count, backed by Caffeine in-memory cache for 30-minute play event deduplication.

## [Phase 5] - Payment Integrity & Concurrency
- **Server-Side Pricing (NTF-06, NTF-07)**: Discarded client-supplied amounts in `CheckoutRequest`; server computes exact order total in integer cents from database prices.
- **Payment Lifecycle & Idempotency (NTF-08, NTF-09)**: Created `orders` and `order_items` tables with states `PENDING`, `PAID`, `FAILED`, `REFUNDED`, `NEEDS_REFUND`. Added `stripe_events` table for idempotent webhook handling.
- **Entitlement Grants (NTF-12)**: Automatically granted non-refundable track/album entitlements upon verified Stripe payment intent fulfillment.
- **Pessimistic Concurrency & Oversell Protection (NTF-13)**: Implemented `PESSIMISTIC_WRITE` locks on merchandise stock during checkout; added automatic compensation/refund triggers when stock runs out.

## [Phase 4] - Data Integrity & Indexing
- **Database Migrations**: Integrated Flyway migrations `V2__integrity_indexes_orders_queue.sql` and `V3__denormalized_counters_play_events.sql`.
- **Index Optimization**: Added composite and full-text indexes for search (`ft_tracks_search`), comments (`idx_comments_track_created`), playback history (`idx_history_user_played`), and unique follow relationships (`uk_follow`).
- **Integrity Constraints**: Enforced unique cart item constraints (`uk_cart_merch`, `uk_cart_track`) and cascading deletes for comments and user queue items.

## [Phase 3] - Access Control & Ownership
- **Resource Ownership (NTF-01, NTF-20)**: Implemented strict resource ownership verification in service layer preventing unauthorized updates or deletions of tracks, albums, merchandise, and comments.
- **Direct ID Referencing (NTF-21, NTF-23)**: Swapped parameter order in comment mutation methods (`userId, commentId, commentText`) ensuring callers only mutate intended items.

## [Phase 2] - Security Hardening & Secrets Management
- **Secret Redaction**: Purged hardcoded credentials, API keys, and test tokens from source code and logs; configured `logback-spring.xml` masking rules.
- **Token Hashing (NTF-03, NTF-04)**: SHA-256 hashed password reset and email verification tokens in database storage; purged raw tokens from responses and logs.
- **Role Validation (NTF-02)**: Restricted registration roles to `consumer`, `music_artist`, `producer`; rejected administrative privilege escalation.

## [Phase 1] - Observability & MDC Tracing
- **Request Tracing**: Implemented `RequestTraceFilter` assigning `X-Request-Id` and `userId` to SLF4J MDC.
- **Aspect-Oriented Logging**: Implemented `TraceAspect` logging `TRACE_ENTER` and `TRACE_EXIT` across controllers, services, and repositories.
- **Safe Argument Summarizer**: Built `ArgumentSummarizer` sanitizing personal data, passwords, and tokens from method trace logs.

## [Phase 0] - Baseline Reproduction
- **Environment Discovery**: Stabilized baseline test suite, verified initial schema, and recorded baseline test output in `reports/baseline-test-output.txt`.
