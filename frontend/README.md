# GroupBuy Frontend — Smart Group-Buying E-Commerce Platform

A production-quality React + TypeScript + Vite + Tailwind CSS frontend application engineered for the GroupBuy platform.

---

## 🚀 Features & Highlights

- **Visual Identity & Design**: Tailored modern e-commerce UI featuring responsive layouts, live deal countdown clocks, pricing tier discount ladders, visual progress bars, accessible badges, skeleton/spinners, empty states, and toast notifications.
- **Authentication & Security**:
  - Centralized Axios client (`apiClient`) with Bearer token injection.
  - Automatic JWT refresh token rotation with failed request queue retry on `401 Unauthorized`.
  - Roles supported: `ROLE_CUSTOMER`, `ROLE_SELLER`, `ROLE_ADMIN`.
  - Safe configuration: no secrets or credentials exposed.
- **Group-Buy Engine**:
  - Live campaign progress visualization (participating units vs minimum target).
  - Multi-tier volume discount ladder component (`PricingTiersVisualizer`).
  - One-click campaign participation with immediate pricing recalculation.
  - User's active and historical group deal commitments view (`/participations`).
- **Standard E-Commerce Flow**:
  - Product catalog browsing with live debounced search, category filters, and sorting.
  - Shopping cart with persistent backend synchronization (`/cart`).
  - Multi-step checkout with mock/sandbox payment processing (`/checkout`).
  - Order history and line-item breakdown (`/orders`, `/orders/:orderNumber`).
- **Admin & Operations Hub**:
  - Administrative management of categories (Create, Edit, Toggle Active, Delete).
  - Product catalog management (Create, Edit price/stock/status, Archive).
  - Campaign lifecycle control (Inspect live progress, manual Finalize, Cancel).
  - Order fulfillment workflow (Update status: `CREATED` → `PAID` → `PROCESSING` → `SHIPPED` → `DELIVERED`).

---

## 🛠️ Tech Stack

- **Runtime & Build Tool**: Node.js & Vite 8
- **Core Framework**: React 19 + TypeScript
- **Styling**: Tailwind CSS v4 (`@tailwindcss/vite`)
- **Routing**: React Router DOM v7
- **HTTP Client**: Axios with interceptors
- **Icons**: Lucide React

---

## 📦 Getting Started

### 1. Prerequisites
- Node.js 18+ (tested on Node 26.3.0 and npm 11.16.0)
- GroupBuy backend running at `http://localhost:8080`

### 2. Configuration
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```
Default `.env`:
```env
VITE_API_BASE_URL=http://localhost:8080
```

### 3. Installation
Navigate into the `frontend/` directory and install dependencies:
```bash
cd frontend
npm install
```

### 4. Running Development Server
```bash
npm run dev
```
The application will launch at `http://localhost:5173`. Requests made to `/api/*` are either routed directly to `VITE_API_BASE_URL` or forwarded via Vite proxy to `http://localhost:8080`.

### 5. Production Build & Verification
```bash
npm run build
```
Creates an optimized static production bundle in `dist/`.

---

## 🗺️ Route Sitemap

### Public Routes
| Route | Page | Description |
|---|---|---|
| `/` | `HomePage` | Hero banner, active group deals, featured products, categories, features |
| `/products` | `ProductsPage` | Search, category filtering, sort by price/date, grid listing |
| `/products/:id` | `ProductDetailPage` | Product specs, active campaign banner, add to cart |
| `/campaigns` | `CampaignsPage` | Live group-buy campaigns with tier discounts and countdowns |
| `/campaigns/:id` | `CampaignDetailPage` | Tier ladder visualizer, current quote, commit/join action |
| `/login` | `LoginPage` | User login with JWT access/refresh token storage |
| `/register` | `RegisterPage` | Customer / Seller account registration |
| `/cart` | `CartPage` | Backend cart items, quantity updates, order summary |

### Customer Protected Routes (`ProtectedRoute`)
| Route | Page | Description |
|---|---|---|
| `/checkout` | `CheckoutPage` | Shipping address, order creation, payment simulation |
| `/orders/confirmed` | `OrderConfirmationPage` | Success invoice and link to order details |
| `/orders` | `OrdersPage` | Full customer order history with status tracking |
| `/orders/:orderNumber` | `OrderDetailPage` | Detailed invoice, line items, shipping address |
| `/participations` | `MyParticipationsPage` | User's joined campaigns, target progress, units committed |
| `/profile` | `ProfilePage` | User account details and assigned roles |

### Admin Protected Routes (`AdminRoute` - `ROLE_ADMIN` / `ROLE_SELLER`)
| Route | Page | Description |
|---|---|---|
| `/admin` | `AdminDashboardPage` | Overview cards, quick links to operational portals |
| `/admin/products` | `AdminProductsPage` | Product inventory table, Add/Edit modal, stock counters |
| `/admin/categories` | `AdminCategoriesPage` | Category table, Add/Edit modal, toggle status |
| `/admin/campaigns` | `AdminCampaignsPage` | Monitor campaign progress, Finalize and Cancel actions |
| `/admin/orders` | `AdminOrdersPage` | Update order fulfillment status dropdown |

---

## 🔌 Integrated Backend Endpoints

All integrated endpoints strictly correspond to existing Spring Boot REST controllers:

- **Authentication**:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/refresh`
  - `POST /api/v1/auth/logout`
- **User Profile**:
  - `GET /api/v1/users/me`
- **Categories**:
  - `GET /api/v1/categories` (public)
  - `GET /api/v1/categories/{id}`
  - `GET /api/v1/categories/slug/{slug}`
  - `GET /api/v1/admin/categories` (admin)
  - `POST /api/v1/admin/categories` (admin)
  - `PUT /api/v1/admin/categories/{id}` (admin)
  - `DELETE /api/v1/admin/categories/{id}` (admin)
- **Products**:
  - `GET /api/v1/products` (public catalog)
  - `GET /api/v1/products/{id}`
  - `GET /api/v1/products/seller/me`
  - `POST /api/v1/products` (seller/admin)
  - `PUT /api/v1/products/{id}` (seller/admin)
  - `DELETE /api/v1/products/{id}` (seller/admin)
- **Campaigns & Pricing**:
  - `GET /api/v1/campaigns` (public active list)
  - `GET /api/v1/campaigns/{id}` (detail with tiered quotes)
  - `POST /api/v1/campaigns` (create)
  - `PUT /api/v1/campaigns/{id}` (update)
  - `POST /api/v1/campaigns/{id}/cancel` (cancel)
  - `GET /api/v1/admin/campaigns` (admin)
  - `POST /api/v1/admin/campaigns/{id}/finalize` (admin finalize)
  - `POST /api/v1/pricing/quote` (pricing engine calculation)
- **Campaign Participations**:
  - `POST /api/v1/campaigns/{id}/participate` (join campaign)
  - `DELETE /api/v1/campaigns/{id}/participate` (leave campaign)
  - `GET /api/v1/participations/me` (my joined campaigns)
  - `GET /api/v1/campaigns/{id}/participations` (campaign participants)
- **Cart**:
  - `GET /api/v1/cart`
  - `POST /api/v1/cart/items`
  - `PUT /api/v1/cart/items/{id}`
  - `DELETE /api/v1/cart/items/{id}`
  - `DELETE /api/v1/cart`
  - `POST /api/v1/cart/checkout`
- **Orders**:
  - `GET /api/v1/orders` (my orders)
  - `GET /api/v1/orders/{orderNumber}` (order details)
  - `POST /api/v1/orders/{orderNumber}/cancel` (cancel order)
  - `GET /api/v1/admin/orders` (admin order list)
  - `PATCH /api/v1/admin/orders/{id}/status` (admin update status)
- **Payments**:
  - `POST /api/v1/payments/initiate`
  - `POST /api/v1/payments/verify`

---

## ℹ️ Backend Compatibility & Unavailable Operations

In accordance with strict verification rules, no fake API endpoints were created:
1. **User Management Listing**: The backend does not expose a global admin user listing endpoint (`GET /api/v1/admin/users`). The Admin Dashboard contains a clear informational notice explaining this limitation.
2. **Payment Gateways**: The frontend integrates with the backend's `/api/v1/payments/initiate` and `/api/v1/payments/verify` simulation endpoints, providing instant order settlement feedback in dev/sandbox mode without hardcoded credentials.
