# GroupBuy — Smart Group-Buying E-Commerce Platform

GroupBuy is a production-style backend e-commerce system that enables sellers to host time-limited group-buying campaigns alongside direct cart and checkout e-commerce. In a group-buying campaign, unit prices drop dynamically as more buyers reserve and pay for units, unlocking collective volume discounts.

---

## 1. Business & Architectural Overview

Traditional e-commerce platforms charge static prices regardless of customer demand concentration. GroupBuy aggregates buyer demand in real-time campaigns.

**Example Dynamic Tiering:**
- **1–4 units**: ₹2,000 / unit
- **5–9 units**: ₹1,800 / unit (10% discount)
- **10–19 units**: ₹1,600 / unit (20% discount)
- **20+ units**: ₹1,400 / unit (30% discount)

When a customer joins a campaign, they authorize payment at the current active tier price. When the campaign completes successfully, if demand reaches a deeper discount tier, the platform recalculates the final settlement price and issues an automatic differential refund. Customers are guaranteed never to pay more than their authorization.

---

## 2. Completed Platform Features

### A. Products & Categories
- Category hierarchy with hierarchical slugs and parent-child relations.
- Product catalog with SKU, stock quantity, regular price, active status, seller ownership, and category association.
- Public product search, filtering (category, price range, stock availability), sorting, and pagination.
- Admin & Seller authorized catalog management (create, update, soft delete / deactivate).
- Pessimistic locking on product inventory updates to prevent overselling.

### B. Group-Buy Campaigns & Pricing Engine
- Campaign lifecycle: `DRAFT` ➔ `SCHEDULED` ➔ `ACTIVE` ➔ `SUCCESSFUL` / `FAILED` / `EXPIRED` / `CANCELLED`.
- Full integration with the dynamic `PricingPolicyValidator` and `PricingService` for data-driven quantity tiers.
- Minimum participant threshold (`min_participants`) and target quantity (`target_quantity`).
- Concurrency-safe updates for `current_participants` and `current_quantity`.
- Background scheduler (`CampaignScheduler`) running every 60 seconds to activate scheduled campaigns, finalize expired campaigns, and release stale inventory reservations.

### C. Participation & Group Membership
- Authenticated user participation with active campaign checks, deadline checks, and stock verification.
- Concurrency-safe stock holds using `InventoryReservation` with automatic 15-minute expiration windows.
- Prevention of duplicate active participations per campaign.
- User-driven participation cancellation with immediate inventory hold release (if not yet locked into settlement).

### D. Cart & Checkout
- Authenticated user-specific persistent cart (`Cart` and `CartItem`).
- Add, update quantity, remove, and clear cart items.
- Server-side price recalculation: client-submitted totals are never trusted.
- Transactional atomic checkout: converts cart items into order line items, decrements product stock, clears the cart, and creates order snapshots.

### E. Orders & Lifecycle
- Order lifecycle: `CREATED` ➔ `PAID` ➔ `PROCESSING` ➔ `SHIPPED` ➔ `DELIVERED` (or `CANCELLED`).
- Historical immutability: `OrderItem` stores snapshots of unit price, product name, and SKU at checkout time.
- Authenticated order history and detail lookup by human-readable order number (e.g., `ORD-20261028-XXXXXX`).
- Access control: Users can only access their own orders; admins can manage all orders.
- Self-service cancellation with automatic inventory restoration for eligible orders (`CREATED` or `PAID`).

### F. Payments & Provider Abstraction
- Clean provider abstraction via `PaymentGateway` SPI.
- Out-of-the-box deterministic `MockPaymentGateway` for local and test environments (no real money or external credentials needed).
- HMAC-SHA256 signature verification for inbound payment webhooks (`POST /api/v1/payments/webhook`).
- Two-phase payment flow: `initiate` ➔ `verify` / `webhook`.
- Differential refund calculation: Automatically credits participants the difference when a campaign finalizes at a lower unit price tier than their authorization.
- Idempotent webhook event processing preventing replay attacks.

### G. Security & Administrative Operations
- Stateless Spring Security filter chain with JWT Bearer token authentication and refresh token rotation.
- Role-based authorization (`ROLE_ADMIN`, `ROLE_SELLER`, `ROLE_CUSTOMER`).
- Centralized `@RestControllerAdvice` exception handling with RFC-compliant sanitized error responses.
- Spring Boot Actuator health checks and OpenAPI 3.0 / Swagger UI documentation.

---

## 3. Technology Stack

- **Java**: 21 (LTS) / 23 compatible
- **Framework**: Spring Boot 3.3.5
- **Build Tool**: Apache Maven 3.9+
- **Persistence**: Spring Data JPA, Hibernate ORM 6
- **Database**: PostgreSQL 16 (H2 compatibility mode for unit/slice testing)
- **Database Migrations**: Flyway Core (`V1`, `V2`, `V3`)
- **Security**: Spring Security 6, JJWT (io.jsonwebtoken 0.12.6), BCrypt
- **API Documentation**: SpringDoc OpenAPI Starter WebMVC UI 2.6.0
- **Testing**: JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers PostgreSQL

---

## 4. Database Migrations

Database schema versioning is managed via Flyway:
- `V1__initial_schema.sql`: Base tables for users, products, campaigns, pricing tiers, participations, reservations, orders, payments, refunds, and audit records.
- `V2__auth_tokens.sql`: Refresh tokens table for JWT session management.
- `V3__categories_and_cart.sql`: Added `categories`, `carts`, `cart_items`, `order_items` tables, `min_participants` / `current_participants` campaign tracking, and foreign key adjustments.

---

## 5. Configuration & Environment Variables

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8080` | Application HTTP port |
| `SPRING_PROFILES_ACTIVE` | `prod` (or `test`) | Active Spring Boot profile |
| `DB_URL` | `jdbc:postgresql://localhost:5432/groupbuy_db?sslmode=require` | Neon Cloud / PostgreSQL JDBC connection URL |
| `DB_USERNAME` | `groupbuy_user` | Database user (Neon / PostgreSQL) |
| `DB_PASSWORD` | `groupbuy_secure_password_replace_in_prod` | Database password (Neon / PostgreSQL) |
| `SPRING_DATASOURCE_URL` | *(fallback to DB_URL)* | Legacy Spring JDBC URL fallback |
| `APP_JWT_SECRET` | *(64-byte hex string in config)* | HMAC-SHA secret for JWT signing |
| `APP_JWT_EXPIRATION_MS` | `3600000` (1 hour) | JWT access token time-to-live |
| `APP_JWT_REFRESH_EXPIRATION_MS` | `604800000` (7 days) | Refresh token time-to-live |
| `APP_PAYMENT_WEBHOOK_SECRET` | *(HMAC secret in config)* | Secret for HMAC-SHA256 payment webhook verification |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Allowed CORS origins |

---

## 6. How to Build & Run

### 6.1 Prerequisites
- JDK 21 or JDK 23 installed and configured on `PATH`.
- PostgreSQL running locally or in Docker.

### 6.2 Running the Application
```powershell
mvn spring-boot:run
```

### 6.3 Executing Tests
```powershell
mvn test "-Dspring.profiles.active=test"
```
> **Note on Testcontainers**: `PostgresSchemaMigrationTest` automatically detects whether Docker daemon is reachable. If Docker is not running, the 3 container tests are gracefully skipped, while all remaining unit and integration tests run against H2 in PostgreSQL compatibility mode.

---

## 7. Major API Endpoints & Examples

### 7.1 Authentication (`/api/v1/auth`)
- `POST /api/v1/auth/register` — Register a customer or seller account.
- `POST /api/v1/auth/login` — Authenticate and receive access + refresh tokens.
- `POST /api/v1/auth/refresh` — Rotate refresh token.

### 7.2 Categories (`/api/v1/categories`)
- `GET /api/v1/categories` — List active categories.
- `POST /api/v1/admin/categories` — (Admin) Create category.
```json
{
  "name": "Electronics",
  "slug": "electronics",
  "description": "Gadgets, audio, and accessories"
}
```

### 7.3 Products (`/api/v1/products`)
- `GET /api/v1/products?search=headphones&page=0&size=10` — Public catalog search.
- `POST /api/v1/products` — (Seller/Admin) Create a product.
```json
{
  "name": "Noise Cancelling Headphones",
  "sku": "NCH-PRO-01",
  "description": "Premium wireless over-ear headphones",
  "regularPrice": 2499.00,
  "stockQuantity": 150,
  "categoryId": 1
}
```

### 7.4 Group-Buy Campaigns (`/api/v1/campaigns`)
- `GET /api/v1/campaigns?status=ACTIVE` — List active group-buy campaigns.
- `POST /api/v1/campaigns` — (Seller/Admin) Create a campaign with quantity discount tiers.
```json
{
  "productId": 1,
  "title": "Launch Campaign: Noise Cancelling Headphones",
  "description": "Unlock up to 30% discount with volume orders",
  "startTime": "2026-10-28T00:00:00Z",
  "endTime": "2026-11-15T23:59:59Z",
  "minParticipants": 5,
  "targetQuantity": 50,
  "maxQuantityPerUser": 2,
  "pricingTiers": [
    { "minQuantity": 1, "maxQuantity": 4, "unitPrice": 2000.00 },
    { "minQuantity": 5, "maxQuantity": 9, "unitPrice": 1800.00 },
    { "minQuantity": 10, "maxQuantity": 19, "unitPrice": 1600.00 },
    { "minQuantity": 20, "maxQuantity": null, "unitPrice": 1400.00 }
  ]
}
```

### 7.5 Participation (`/api/v1/campaigns/{id}/participate`)
- `POST /api/v1/campaigns/{id}/participate` — (Customer) Join campaign and hold inventory.
```json
{
  "quantity": 2
}
```
- `DELETE /api/v1/campaigns/{id}/participate` — Cancel participation and release inventory hold.
- `GET /api/v1/participations/me` — View current user's campaign participations.

### 7.6 Cart & Checkout (`/api/v1/cart`)
- `GET /api/v1/cart` — View current user's cart with recalculation.
- `POST /api/v1/cart/items` — Add item to cart.
```json
{
  "productId": 1,
  "quantity": 2
}
```
- `POST /api/v1/cart/checkout` — Checkout cart into an immutable Order.
```json
{
  "shippingAddress": "123 Tech Park, Bangalore, KA, India",
  "idempotencyKey": "chk-9812401824"
}
```

### 7.7 Orders (`/api/v1/orders`)
- `GET /api/v1/orders` — List authenticated user's orders with pagination.
- `GET /api/v1/orders/{orderNumber}` — Get order details and item snapshots.
- `POST /api/v1/orders/{orderNumber}/cancel` — Cancel order and restore product inventory.
- `PATCH /api/v1/admin/orders/{id}/status?status=SHIPPED` — (Admin) Update shipment status.

### 7.8 Payments (`/api/v1/payments`)
- `POST /api/v1/payments/initiate` — Initiate payment for an order.
```json
{
  "orderId": 1
}
```
- `POST /api/v1/payments/verify` — Complete client verification.
```json
{
  "paymentId": 1,
  "gatewayTransactionId": "txn_mock_12345",
  "signature": "simulated_or_gateway_signature"
}
```
- `POST /api/v1/payments/webhook` — HMAC-SHA256 verified webhook for asynchronous gateway callbacks.

---

## 8. Swagger UI & OpenAPI Specification

When running the application, explore interactive API endpoints and schemas:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI 3.0 JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
