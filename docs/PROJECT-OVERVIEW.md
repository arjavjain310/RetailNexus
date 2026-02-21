# RetailNexus — End-to-End Project Overview

## 1. What Is RetailNexus?

**RetailNexus** is a lightweight **retail management system** for small shops or stores. It runs in the browser and lets you:

- Log in as **Admin** or **Cashier**
- Manage **products** (name, barcode, category, cost, selling price, GST %, unit)
- Manage **inventory** (set stock per product; add stock by product + quantity)
- Create **bills** (add items to cart, apply GST, complete sale, view/print invoice and PDF)
- View a **dashboard** (today’s sales, monthly revenue, profit, low stock, restock suggestions, charts)
- Run **reports** (daily/monthly sales, low stock, dead stock; export to PDF)

There are no product images or file uploads; the app is focused on **inventory, billing, and analytics**.

---

## 2. Tech Stack

### Backend

| Technology | Purpose |
|------------|--------|
| **Java 17** | Language |
| **Spring Boot 3.2** | Application framework (web, security, data) |
| **Spring MVC** | HTTP endpoints and request handling |
| **Spring Data JPA** | Database access (repositories, entities) |
| **Spring Security** | Login, roles (Admin/Cashier), session, CSRF |
| **Bean Validation (Jakarta)** | Form validation (e.g. product, registration) |
| **Maven** | Build, dependencies, packaging |

### Database

| Technology | Purpose |
|------------|--------|
| **H2** | Embedded database (file-based locally, in-memory on Render) |
| **JPA / Hibernate** | ORM: entities → tables, migrations via `ddl-auto=update` |

### Frontend

| Technology | Purpose |
|------------|--------|
| **Thymeleaf** | Server-side HTML templates (layout, forms, tables) |
| **Bootstrap 5** | Layout, components, responsive UI |
| **Chart.js** | Dashboard charts (line, bar, pie) |
| **Vanilla JS** | Billing page (cart, totals, form submit) |

### Other

| Technology | Purpose |
|------------|--------|
| **OpenHTMLToPDF** | Generate invoice PDF from HTML |
| **Docker** | Container image for deployment (e.g. Render) |

### Summary Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│  Browser (user)                                                  │
│  → HTML/CSS/JS (Thymeleaf + Bootstrap + Chart.js)                │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP (forms, redirects, JSON-like)
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  Spring Boot App                                                 │
│  ├── Spring Security (login, session, roles)                     │
│  ├── Controllers (MVC) → Services → Repositories (JPA)          │
│  └── Schema migrations (SchemaMigrationRunner)                  │
└────────────────────────────┬────────────────────────────────────┘
                             │ JDBC
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  H2 Database                                                     │
│  (users, products, batches, sales, sale_items,                  │
│   inventory_transactions)                                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Architecture (Layers)

The app follows a **layered** structure:

1. **Controllers** — Handle HTTP: show pages, accept form posts, redirect.
2. **Services** — Business logic: create sale, deduct stock, compute dashboard stats, generate PDF.
3. **Repositories** — Data access: JPA repositories for each entity.
4. **Entities** — Domain model mapped to database tables.

Example flow for **completing a sale**:

- **BillingController** receives POST with cart data → parses it into `CartItem` list.
- **SaleService.createSale()** builds a `Sale` and `SaleItem`s, computes totals and GST, then calls **BatchService** to deduct stock from batches (FIFO; can go negative for oversell).
- **SaleRepository** and **BatchRepository** persist the sale and updated batches.
- User is redirected to the invoice page; **PdfReportService** can generate a PDF from the same sale.

---

## 4. Data Model (Entities & Tables)

| Entity | Table | Main fields |
|--------|--------|-------------|
| **User** | `users` | id, username, password (hashed), role (ADMIN/CASHIER) |
| **Product** | `products` | id, name, barcode, category, costPrice, sellingPrice, gstPercent, unit (KG/LITRE/PIECES) |
| **Batch** | `batches` | id, product_id, batchNumber, expiryDate, quantity (internal; no user-facing batch form) |
| **Sale** | `sales` | id, saleDate, totalAmount, totalGst, totalProfit, user_id, payment_method |
| **SaleItem** | `sale_items` | id, sale_id, product_id, batch_id, quantity (BigDecimal), unitPrice, gstPercent, gstAmount, totalPrice, profit |
| **InventoryTransaction** | `inventory_transactions` | id, batch_id, type (RESTOCK/SALE), quantityChange, reference |

Relations:

- A **Sale** has many **SaleItem**s; each **SaleItem** references one **Product** and one **Batch**.
- **Batch** belongs to one **Product**; stock is summed per product across batches.
- **InventoryTransaction** records restock and sale deductions per batch.

---

## 5. End-to-End User Flows

### 5.1 First Run (Setup)

1. App starts → **DataLoader** runs:
   - Creates users: **admin** / **admin123**, **cashier** / **cashier123**.
   - If no products exist, seeds 55+ grocery products and creates initial batches with default stock (e.g. 100 per product).
2. **SchemaMigrationRunner** runs after JPA:
   - Adds `products.unit` if missing, alters `sale_items.quantity` to decimal if needed, drops `batches` quantity check constraint so stock can go negative on oversell.

### 5.2 Login

1. User opens app → **SecurityConfig** sends unauthenticated users to `/login`.
2. **LoginController** serves the login page; user enters username/password.
3. **UserDetailsServiceImpl** loads user from DB; Spring Security checks password (BCrypt) and creates session.
4. Redirect to **Dashboard** (or previously requested page).

### 5.3 Dashboard

1. **DashboardController** asks **DashboardService** for:
   - totalSalesToday, monthlyRevenue, totalProfit, lowStockCount, nearExpiryCount, deadStockCount,
   - monthlySalesTrend, categoryWiseSales, profitDistribution, restockSuggestions.
2. **DashboardService** uses **SaleRepository**, **SaleItemRepository**, **BatchRepository**, **ProductRepository** (and **RestockSuggestionService**).
3. Thymeleaf template **dashboard.html** renders cards and Chart.js charts (monthly trend, category sales, profit by category).

### 5.4 Products

1. **ProductController** lists products (with optional search); **ProductService** / **ProductRepository** load from DB.
2. Add/Edit: form with name, barcode, category, cost, selling price, GST %, unit. On submit, **ProductService.save()**; duplicate barcode check returns error without saving.
3. Delete: POST to delete by id; product is removed (constraints may require handling batches/sale_items depending on schema).

### 5.5 Inventory

1. **InventoryController** lists products with current stock (**BatchService.getTotalStock(product)**).
2. User can **set stock** per product (form → POST `/inventory/set-stock`) → **BatchService.setProductStock(product, quantity)** (single batch per product, others zeroed).
3. **Add Stock**: form with product + quantity only → **BatchService.addProductStock(product, quantity)** (adds to existing total). No batch number or expiry in the UI.

### 5.6 Billing

1. **BillingController** shows product list (name, quantity in cart, cost price, selling price) and cart.
2. User adds items (product, quantity in product unit — e.g. 0.7 kg, 2 L, 3 pcs), optional unit price and GST % per line.
3. Cart is kept in the page (e.g. hidden input or JS); on **Complete**, form POSTs cart as a string (e.g. `id:qty:unitPrice:gstPercent;...`).
4. **BillingController.parseCart()** builds list of **SaleService.CartItem** (product, quantity as BigDecimal, unitPrice, gstPercent).
5. **SaleService.createSale()**:
   - For each cart line: finds batches (FIFO), creates **SaleItem**s with quantity, unit price, GST, totals, profit; records **BatchDeduction**s.
   - For remaining quantity (e.g. oversell): **BatchService.getOrCreateBatchForProduct()**, then deduct (quantity can go negative).
   - Saves **Sale** and all **SaleItem**s; then **BatchService.deductStock()** for each deduction.
6. Redirect to **invoice** page; user can open **PDF** via **PdfReportService** (HTML → PDF).

### 5.7 Reports

1. **ReportsController** serves report index, daily sales, monthly sales, low stock, dead stock.
2. **ReportService** uses **SaleRepository**, **SaleItemRepository**, **BatchRepository**, **ProductService** for data.
3. PDF export uses **PdfReportService** (HTML template → PDF).

---

## 6. Security

- **Spring Security**: form login at `/login`, logout at `/logout`, session-based auth.
- **Roles**: ADMIN, CASHIER (both can access dashboard, products, inventory, billing, reports in this app).
- **PasswordEncoder**: BCrypt for storing passwords (DataLoader, RegisterController).
- **CSRF**: enabled for forms; Thymeleaf includes `_csrf` in posts.
- **Paths**: `/login`, `/register`, `/css/**`, `/js/**`, `/h2-console/**` are public; rest require authentication.

---

## 7. Deployment

- **Local**: `./mvnw spring-boot:run` → app at `http://localhost:8080`; H2 file DB under `./data/`.
- **Render**: Repo has **Dockerfile** (multi-stage: Maven build → JRE + jar) and **render.yaml** (web service, Docker runtime, env `SPRING_PROFILES_ACTIVE=render`). Render builds the image and runs the jar; H2 in-memory (data resets on deploy).

---

## 8. Project Structure (Recap)

```
RetailNexus/
├── pom.xml
├── Dockerfile, render.yaml
├── src/main/java/com/retailnexus/
│   ├── RetailNexusApplication.java
│   ├── config/       (DataLoader, SecurityConfig, SchemaMigrationRunner, GlobalControllerAdvice)
│   ├── controller/   (Login, Register, Dashboard, Product, Inventory, Billing, Reports, User)
│   ├── entity/       (User, Product, Batch, Sale, SaleItem, InventoryTransaction)
│   ├── repository/   (JPA repos for each entity)
│   └── service/      (Product, Batch, Sale, Dashboard, Report, PdfReport, RestockSuggestion, UserDetailsImpl)
└── src/main/resources/
    ├── application.properties, application-render.properties
    ├── static/css/, static/images/
    └── templates/   (layout, login, register, dashboard, products, inventory, billing, reports, user)
```

---

This document gives an **end-to-end explanation** of the project and the **tech stack** used. For running and deploying, see the main **README.md** and **DEPLOY.md**.
