#!/usr/bin/env bash
# ==============================================================================
# Operation Ninja T Frog - Trace Demo Script
# Runs scenarios against a running stack, setting X-Request-Id: ntf-demo-<scenario>
# and saving matching trace logs to reports/trace/<scenario>.log
#
# Usage: ./scripts/trace-demo.sh [scenario|all]
# Scenarios:
#   1. register-verify-login
#   2. forbidden-edit
#   3. forgot-password
#   4. checkout-webhook
#   5. oversell
#   6. tracks-page
#   7. stream-gated
#   8. comment-edit
#   9. file-ingest-blocked
# ==============================================================================

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
OUTPUT_DIR="${OUTPUT_DIR:-reports/trace}"
APP_LOG="${APP_LOG:-logs/app.log}"

mkdir -p "$OUTPUT_DIR"

SCENARIO="${1:-all}"

run_scenario() {
  local sc="$1"
  echo ">>> Running trace scenario: $sc"

  case "$sc" in
    "register-verify-login")
      local req_id="ntf-demo-register-verify-login"
      # Attempt registration with forbidden role
      curl -s -X POST "$BASE_URL/api/auth/register" \
        -H "Content-Type: application/json" \
        -H "X-Request-Id: $req_id-role-reject" \
        -d '{"email":"badrole@test.com","password":"Password123!","roles":["SUPER_ADMIN"]}' || true

      # Successful registration
      local reg_resp
      reg_resp=$(curl -s -X POST "$BASE_URL/api/auth/register" \
        -H "Content-Type: application/json" \
        -H "X-Request-Id: $req_id-reg" \
        -d '{"email":"traceuser@test.com","password":"Password123!","roles":["CONSUMER"]}')

      # Login failure (wrong password)
      curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -H "X-Request-Id: $req_id-bad-login" \
        -d '{"email":"traceuser@test.com","password":"WrongPassword!"}' || true

      # Login success
      curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -H "X-Request-Id: $req_id-login" \
        -d '{"email":"traceuser@test.com","password":"Password123!"}' || true
      ;;

    "forbidden-edit")
      local req_id="ntf-demo-forbidden-edit"
      # User B attempts to edit User A's track -> 403 Forbidden
      curl -s -X PUT "$BASE_URL/api/tracks/1" \
        -H "Content-Type: application/json" \
        -H "X-Request-Id: $req_id" \
        -H "Authorization: Bearer mock-user-b-token" \
        -d '{"title":"Hacked Title"}' || true
      ;;

    "forgot-password")
      local req_id="ntf-demo-forgot-password"
      # Forgot password returns 202 Accepted, no token leaked in body or log
      curl -s -X POST "$BASE_URL/api/users/forgot-password" \
        -H "Content-Type: application/json" \
        -H "X-Request-Id: $req_id" \
        -d '{"email":"traceuser@test.com"}' || true
      ;;

    "checkout-webhook")
      local req_id="ntf-demo-checkout"
      # 1. Checkout (server computes total, client amount ignored)
      curl -s -X POST "$BASE_URL/api/checkout" \
        -H "Content-Type: application/json" \
        -H "X-Request-Id: $req_id" \
        -H "Authorization: Bearer mock-buyer-token" \
        -d '{"items":[{"type":"TRACK","id":1,"quantity":1,"unitPriceCents":999999}]}' || true

      # 2. Webhook delivery -> order paid, entitlement granted
      curl -s -X POST "$BASE_URL/api/payments/webhook" \
        -H "Content-Type: application/json" \
        -H "X-Request-Id: ntf-demo-webhook-1" \
        -H "Stripe-Signature: t=1729160000,v1=mock_signature" \
        -d '{"id":"evt_demo_checkout_1","type":"payment_intent.succeeded","data":{"object":{"id":"pi_demo_checkout_1","amount":1500,"currency":"usd"}}}' || true

      # 3. Duplicate webhook ignored
      curl -s -X POST "$BASE_URL/api/payments/webhook" \
        -H "Content-Type: application/json" \
        -H "X-Request-Id: ntf-demo-webhook-2" \
        -H "Stripe-Signature: t=1729160000,v1=mock_signature" \
        -d '{"id":"evt_demo_checkout_1","type":"payment_intent.succeeded","data":{"object":{"id":"pi_demo_checkout_1","amount":1500,"currency":"usd"}}}' || true
      ;;

    "oversell")
      local req_id="ntf-demo-oversell"
      # Two paid orders for limited stock item -> one fulfills, second marks order.oversold
      curl -s -X POST "$BASE_URL/api/payments/webhook" \
        -H "Content-Type: application/json" \
        -H "X-Request-Id: $req_id-order-1" \
        -H "Stripe-Signature: t=1729160000,v1=mock_sig" \
        -d '{"id":"evt_os_1","type":"payment_intent.succeeded","data":{"object":{"id":"pi_os_1","amount":2500,"currency":"usd"}}}' || true

      curl -s -X POST "$BASE_URL/api/payments/webhook" \
        -H "Content-Type: application/json" \
        -H "X-Request-Id: $req_id-order-2" \
        -H "Stripe-Signature: t=1729160000,v1=mock_sig" \
        -d '{"id":"evt_os_2","type":"payment_intent.succeeded","data":{"object":{"id":"pi_os_2","amount":2500,"currency":"usd"}}}' || true
      ;;

    "tracks-page")
      local req_id="ntf-demo-tracks-page"
      # Browse paged tracks: page=0, size=20
      curl -s -X GET "$BASE_URL/api/tracks?page=0&size=20" \
        -H "X-Request-Id: $req_id" || true
      ;;

    "stream-gated")
      local req_id="ntf-demo-stream-gated"
      # 1. Fetch play url (preview or full)
      curl -s -X GET "$BASE_URL/api/tracks/1/play" \
        -H "X-Request-Id: $req_id-play" || true

      # 2. Stream Range request: bytes=0-1023 -> 206 Partial Content
      curl -s -X GET "$BASE_URL/api/stream/1?v=preview&exp=1800000000&sig=dummy_sig" \
        -H "Range: bytes=0-1023" \
        -H "X-Request-Id: $req_id-range" || true
      ;;

    "comment-edit")
      local req_id="ntf-demo-comment-edit"
      # Author edits comment -> 200 OK
      curl -s -X PUT "$BASE_URL/api/tracks/comments/1" \
        -H "Content-Type: application/json" \
        -H "X-Request-Id: $req_id" \
        -H "Authorization: Bearer mock-author-token" \
        -d '{"commentText":"Updated insightful comment"}' || true
      ;;

    "file-ingest-blocked")
      local req_id="ntf-demo-file-ingest-blocked"
      # Server file ingestion JSON POST /api/tracks rejected -> 405 Method Not Allowed
      curl -s -X POST "$BASE_URL/api/tracks" \
        -H "Content-Type: application/json" \
        -H "X-Request-Id: $req_id" \
        -H "Authorization: Bearer mock-artist-token" \
        -d '{"title":"Malicious Track","filePath":"/etc/passwd"}' || true
      ;;

    *)
      echo "Unknown scenario: $sc"
      exit 1
      ;;
  esac

  # Extract log lines if app log exists
  if [[ -f "$APP_LOG" ]]; then
    grep -E "\[ntf-demo-${sc}" "$APP_LOG" > "$OUTPUT_DIR/${sc}.log" || true
    echo "Saved trace log to $OUTPUT_DIR/${sc}.log"
  fi
}

ALL_SCENARIOS=(
  "register-verify-login"
  "forbidden-edit"
  "forgot-password"
  "checkout-webhook"
  "oversell"
  "tracks-page"
  "stream-gated"
  "comment-edit"
  "file-ingest-blocked"
)

if [[ "$SCENARIO" == "all" ]]; then
  for s in "${ALL_SCENARIOS[@]}"; do
    run_scenario "$s"
  done
else
  run_scenario "$SCENARIO"
fi

echo "Trace demo execution complete."
