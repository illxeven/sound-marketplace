# Payments & Checkout Architecture (Operation Ninja T Frog Phase 5)

## Overview
Phase 5 establishes a secure, server-authoritative checkout and payment processing flow.
Prices are derived exclusively from the database, eliminating client-side price tampering. Orders and entitlements are strictly synchronized using Stripe webhooks with cryptographic signature verification and idempotency controls.

---

## Architecture & Event Flow

```mermaid
sequenceDiagram
    autonumber
    actor Buyer as Client / Mobile App
    participant Backend as CheckoutController / CheckoutService
    participant DB as MySQL Database
    participant Stripe as Stripe API
    participant Webhook as PaymentController (/api/payments/webhook)
    participant OrderSvc as OrderServiceImpl

    %% Step 1: Checkout Initiation
    Buyer->>Backend: POST /api/checkout {items: [{type, id, quantity}]}
    Backend->>DB: Fetch item prices from DB (tracks, albums, merch)
    Backend->>Backend: Validate items (not free, not self-owned, in stock)
    Backend->>DB: INSERT orders (status=PENDING), order_items
    Backend->>Stripe: paymentIntents.create(amount, customer, metadata={orderId})
    Backend-->>Buyer: Return {orderId, clientSecret, customerId, ephemeralKey}

    %% Step 2: Payment Execution
    Buyer->>Stripe: Confirm payment using clientSecret / PaymentSheet
    Stripe-->>Buyer: Payment Succeeded

    %% Step 3: Webhook Fulfillment
    Stripe->>Webhook: POST /api/payments/webhook (Stripe-Signature, payload)
    Webhook->>OrderSvc: handleWebhook(payload, signature)
    OrderSvc->>OrderSvc: Verify Stripe-Signature with STRIPE_WEBHOOK_SECRET
    OrderSvc->>DB: INSERT IGNORE INTO stripe_events (id, type)
    alt Duplicate Event (0 rows inserted)
        OrderSvc-->>Webhook: Log webhook.duplicate & return 200 OK
    else New Event
        alt payment_intent.succeeded / checkout.session.completed
            OrderSvc->>DB: Fetch order by intent ID / metadata orderId
            OrderSvc->>OrderSvc: Verify amount_received == total_cents and currency matches
            alt Amount Mismatch
                OrderSvc->>DB: Log order.amount_mismatch, leave order PENDING
            else Amount Matches
                OrderSvc->>DB: UPDATE orders SET status='PAID', paid_at=NOW()
                OrderSvc->>DB: Grant entitlements (INSERT IGNORE into entitlements)
                OrderSvc->>DB: Decrement merch stock (UPDATE stock_quantity - q WHERE stock >= q)
                alt Merchandise Oversold
                    OrderSvc->>DB: UPDATE orders SET status='NEEDS_REFUND' & log order.oversold
                end
                OrderSvc->>DB: Clear buyer's cart items
            end
        else charge.refunded
            OrderSvc->>DB: UPDATE orders SET status='REFUNDED'
            OrderSvc->>DB: Revoke entitlements (DELETE FROM entitlements)
        else payment_intent.payment_failed
            OrderSvc->>DB: UPDATE orders SET status='FAILED'
        end
        OrderSvc-->>Webhook: Return 200 OK
    end
    Webhook-->>Stripe: 200 OK
```

---

## Local Testing with Stripe CLI

To test Stripe checkout and webhook events locally:

### 1. Start Webhook Forwarding
Forward webhook events from Stripe to your local running application:
```bash
stripe listen --forward-to localhost:8080/api/payments/webhook
```
When started, the CLI will output a webhook signing secret:
```
> Ready! Your webhook signing secret is whsec_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```
Ensure your application has `STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx` set in its environment or `application.properties`.

### 2. Triggering Webhook Events
In another terminal, trigger test events:
```bash
# Trigger a payment_intent.succeeded event
stripe trigger payment_intent.succeeded

# Trigger a checkout.session.completed event
stripe trigger checkout.session.completed

# Trigger a charge.refunded event
stripe trigger charge.refunded
```

### 3. Simulating Webhooks with Fixtures
To test end-to-end order fulfillment with a real order ID:
```bash
stripe trigger payment_intent.succeeded \
  --override payment_intent:metadata.orderId="1" \
  --override payment_intent:amount=1500 \
  --override payment_intent:currency="usd"
```

---

## Key Endpoints

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/checkout` | Authenticated | Create order & PaymentIntent for mobile/in-app checkout |
| `POST` | `/api/checkout/session` | Authenticated | Create order & Stripe Checkout Session for web checkout |
| `POST` | `/api/payments/webhook` | Public | Ingest and process Stripe webhook events |
| `GET` | `/api/orders` | Authenticated | Get current user's order history (paged) |
| `GET` | `/api/orders/{id}` | Authenticated | Get specific order details (owner or admin only) |
| `GET` | `/api/library` | Authenticated | Get user's entitled tracks from paid orders (paged) |
| `GET` | `/api/payments/history` | Authenticated | View legacy payment history |
| `GET` | `/api/payments/saved-cards` | Authenticated | View saved customer payment methods |

---

## Deprecated & Removed Endpoints
The following insecure or mock endpoints have been removed:
- `POST /api/payments`
- `POST /api/payments/buy/track/{trackId}`
- `POST /api/payments/buy/merchandise/{merchandiseId}`
- `POST /api/payments/buy/album/{albumId}`
- `POST /api/payments/checkout-session`
- `POST /api/payments/create-intent`
- `POST /api/payments/confirm-intent`
