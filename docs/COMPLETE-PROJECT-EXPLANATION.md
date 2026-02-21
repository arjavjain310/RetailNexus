# RetailNexus — Complete Project Explanation

This document is the single reference for what the project is, what technologies it uses, how it works end-to-end, and how to run and deploy it.

---

## 1. What Is RetailNexus?

**RetailNexus** is a **retail management system** for small shops. It is browser-based: you log in and use the web app to manage products, stock, billing, and reports.

### What you can do

| Feature | Description |
|--------|-------------|
| **Login** | Sign in as Admin or Cashier (default users: admin/admin123, cashier/cashier123). |
| **Dashboard** | See today’s sales, monthly revenue, profit, low stock count, near expiry, dead stock; view charts (monthly trend, category sales, profit); see restock suggestions. |
| **Products** | Add, edit, delete products. Fields: name, barcode, category, cost price, selling price, GST %, unit (kg/L/pcs). Duplicate barcode is blocked. |
| **Inventory** | See total stock per product; set stock (inline); add stock (product + quantity only). Stock goes down automatically when you complete a sale. |
| **Billing** | Add products to cart with quantity (e.g. 0.7 kg, 2 L); optional price and GST % per line; complete sale; view invoice in browser. No PDF download. |
| **Reports** | Daily sales, monthly sales, low stock, dead stock. Daily report can be exported as PDF. |

There are **no product images** and **no file uploads**. Focus is on **inventory, billing, and analytics**.

---

## 2. Tech Stack (Complete)

### Backend

| Technology | Version / Note | Purpose |
|------------|----------------|---------|
| **Java** | 17 | Language |
| **Spring Boot** | 3.2.5 | Main framework |
| **Spring MVC** | (part of Spring Boot) | HTTP and request handling |
| **Spring Data JPA** | (part of Spring Boot) | Database access via repositories |
| **Spring Security** | (part of Spring Boot) | Login, session, roles, CSRF |
| **Bean Validation (Jakarta)** | (part of Spring Boot) | Form validation |
| **Maven** | 3.x | Build and dependencies |

### Database

| Technology | Purpose |
|------------|--------|
| **H2** | Embedded database. File-based locally (`./data/`), in-memory on Render. |
| **JPA / Hibernate** | Maps Java entities to tables. Schema created/updated with `ddl-auto=update`. |
| **SchemaMigrationRunner** | Runs after startup: adds/alters columns (e.g. `products.unit`, `sale_items.quantity`), drops batches quantity check constraint. |

### Frontend

| Technology | Purpose |
|------------|--------|
| **Thymeleaf** | Server-rendered HTML (layout, forms, tables, conditionals). |
| **Bootstrap 5** | CSS framework (grid, cards, buttons, forms, tables). |
| **Chart.js** | Dashboard charts (line, bar, pie). |
| **Vanilla JavaScript** | Billing page: cart updates, totals, form submission. |

### Other libraries

| Technology | Purpose |
|------------|--------|
| **OpenHTMLToPDF (openhtmltopdf-pdfbox)** | Converts HTML to PDF (used for **Reports → Daily → Export PDF** only). |
| **thymeleaf-extras-springsecurity6** | Thymeleaf + Spring Security integration (e.g. show username, secure URLs). |
| **Docker** | Builds the app image for deployment on Render. |

### High-level architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  Browser                                                         │
│  (Thymeleaf HTML + Bootstrap + Chart.js + JS)                    │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP (GET/POST, session cookie)
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  Spring Boot                                                     │
│  • Spring Security (login, session, CSRF)                        │
│  • Controllers → Services → Repositories                         │
│  • SchemaMigrationRunner (after startup)                        │
└────────────────────────────┬────────────────────────────────────┘
                             │ JDBC (JPA/Hibernate)
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  H2 Database                                                     │
│  users, products, batches, sales, sale_items,                   │
│  inventory_transactions                                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Project structure (files and roles)

```
RetailNexus/
├── pom.xml                          # Maven: dependencies, Java 17, Spring Boot 3.2
├── Dockerfile                       # Multi-stage: Maven build → JRE + jar (for Render)
├── render.yaml                     # Render Blueprint: web service, Docker, SPRING_PROFILES_ACTIVE=render
├── .gitignore
├── README.md
├── DEPLOY.md                       # Steps: push to GitHub, deploy on Render
├── docs/
│   ├── COMPLETE-PROJECT-EXPLANATION.md   # This file
│   ├── PROJECT-OVERVIEW.md
│   └── PUSH-TO-GITHUB.md
│
└── src/main/
    ├── java/com/retailnexus/
    │   ├── RetailNexusApplication.java    # Entry point
    │   │
    │   ├── config/
    │   │   ├── DataLoader.java             # Seeds admin/cashier + products + batches (if empty)
    │   │   ├── SecurityConfig.java         # Login, logout, public paths, CSRF
    │   │   ├── SchemaMigrationRunner.java  # DB migrations (unit, quantity, batches constraint)
    │   │   └── GlobalControllerAdvice.java # Shared model (e.g. current user)
    │   │
    │   ├── controller/
    │   │   ├── LoginController.java        # GET /login
    │   │   ├── RegisterController.java    # Register new user
    │   │   ├── DashboardController.java   # GET /dashboard (metrics + charts)
    │   │   ├── ProductController.java     # Products CRUD, list, search
    │   │   ├── InventoryController.java   # List stock, set-stock, add-stock
    │   │   ├── BillingController.java     # Billing page, complete sale, invoice (no PDF)
    │   │   ├── ReportsController.java     # Daily/monthly/low stock/dead stock, daily PDF
    │   │   └── UserController.java        # User profile
    │   │
    │   ├── entity/
    │   │   ├── User.java
    │   │   ├── Product.java               # name, barcode, category, cost, selling, gst%, unit
    │   │   ├── Batch.java                 # product, batchNumber, expiryDate, quantity
    │   │   ├── Sale.java                  # saleDate, totals, soldBy, paymentMethod, items
    │   │   ├── SaleItem.java              # product, batch, quantity, unitPrice, gst, totalPrice
    │   │   └── InventoryTransaction.java  # batch, type, quantityChange, reference
    │   │
    │   ├── repository/
    │   │   ├── UserRepository.java
    │   │   ├── ProductRepository.java
    │   │   ├── BatchRepository.java
    │   │   ├── SaleRepository.java
    │   │   ├── SaleItemRepository.java
    │   │   └── InventoryTransactionRepository.java
    │   │
    │   └── service/
    │       ├── UserDetailsServiceImpl.java   # Load user for Spring Security
    │       ├── ProductService.java
    │       ├── BatchService.java             # Stock: getTotalStock, setProductStock, addProductStock, deductStock
    │       ├── SaleService.java              # createSale, findById, findByIdWithItems
    │       ├── DashboardService.java        # All dashboard metrics and chart data
    │       ├── ReportService.java           # Daily/monthly/low stock/dead stock data
    │       ├── PdfReportService.java        # generateSalesReportPdf (daily report only)
    │       └── RestockSuggestionService.java # Suggestions based on last 30 days sales
    │
    └── resources/
        ├── application.properties          # Server, H2 file DB, JPA, Thymeleaf
        ├── application-render.properties   # Render: H2 in-memory, create-drop
        ├── static/
        │   ├── css/style.css
        │   └── images/logo.png
        └── templates/
            ├── layout.html                 # Common layout (nav, scripts)
            ├── login.html
            ├── register.html
            ├── dashboard.html              # Cards + Chart.js
            ├── products/list.html, form.html
            ├── inventory/list.html, add-stock.html
            ├── billing/index.html, invoice.html   # No PDF button
            ├── reports/index.html, daily.html, monthly.html, low-stock.html, dead-stock.html
            └── user/profile.html
```

---

## 4. Data model (database)

| Entity | Table | Main fields |
|--------|--------|-------------|
| **User** | `users` | id, username, password (BCrypt), role (ADMIN, CASHIER) |
| **Product** | `products` | id, name, barcode, category, cost_price, selling_price, gst_percent, unit (KG, LITRE, PIECES) |
| **Batch** | `batches` | id, product_id, batch_number, expiry_date, quantity |
| **Sale** | `sales` | id, sale_date, total_amount, total_gst, total_profit, user_id, payment_method |
| **SaleItem** | `sale_items` | id, sale_id, product_id, batch_id, quantity (decimal), unit_price, gst_percent, gst_amount, total_price, profit |
| **InventoryTransaction** | `inventory_transactions` | id, batch_id, type (RESTOCK/SALE), quantity_change, reference |

Relations:

- **Sale** → many **SaleItem**s. Each **SaleItem** → one **Product**, one **Batch**.
- **Batch** → one **Product**. Total stock for a product = sum of all batch quantities (including negative for oversell).
- **InventoryTransaction** records each restock and each sale deduction per batch.

---

## 5. End-to-end flows (how it works)

### 5.1 First run

1. App starts; Hibernate creates/updates tables from entities.
2. **DataLoader** runs:
   - If no users: creates **admin** (admin123) and **cashier** (cashier123).
   - If no products: seeds 55+ grocery products and creates one batch per product with default stock (e.g. 100).
3. **SchemaMigrationRunner** runs:
   - Adds `products.unit` if missing.
   - Alters `sale_items.quantity` to DECIMAL(12,3) if needed.
   - Drops check constraint on `batches.quantity` so stock can go negative when overselling.

### 5.2 Login

1. User opens `/` or any protected URL → redirected to `/login`.
2. User submits username/password → **UserDetailsServiceImpl** loads user, Spring Security checks password (BCrypt).
3. Session created; user redirected to dashboard (or originally requested page).

### 5.3 Dashboard

1. **DashboardController** calls **DashboardService** for all metrics and chart data.
2. **DashboardService** uses repositories (Sale, SaleItem, Batch, Product) and **RestockSuggestionService**.
3. **dashboard.html** renders cards and Chart.js (monthly trend, category sales, profit by category).

### 5.4 Products

1. List: **ProductController** → **ProductService.findAll()** (optional search).
2. Add/Edit: Form → **ProductService.save()**. Duplicate barcode check in controller; error shown on form.
3. Delete: POST with product id; product removed.

### 5.5 Inventory

1. List: **InventoryController** loads products and **BatchService.getTotalStock(product)** for each.
2. Set stock: POST `/inventory/set-stock` (productId, quantity) → **BatchService.setProductStock()** (one batch per product).
3. Add stock: Form (product + quantity) → POST `/inventory/add-stock` → **BatchService.addProductStock()** (adds to current total).

### 5.6 Billing

1. **BillingController** shows product list and cart. User adds lines (product, quantity, optional price/GST).
2. Cart is built in the page (e.g. hidden input); on **Complete**, form POSTs a string like `productId:qty:unitPrice:gstPercent;...`.
3. **BillingController.parseCart()** turns it into **SaleService.CartItem** list (BigDecimal quantity).
4. **SaleService.createSale()**:
   - For each line: gets batches (FIFO), creates **SaleItem**s, computes line total and GST, records deductions.
   - If quantity exceeds stock: **BatchService.getOrCreateBatchForProduct()**, then deduct (batch quantity can go negative).
   - Saves **Sale** and **SaleItem**s; then **BatchService.deductStock()** for each batch.
5. Redirect to **invoice** page (`/billing/invoice/{id}`). Invoice is view-only in the browser; there is **no PDF download**.

### 5.7 Reports

1. **ReportsController** serves report index, daily, monthly, low stock, dead stock.
2. **ReportService** fetches data from Sale, SaleItem, Batch, Product.
3. **Daily** report has an **Export PDF** button → **PdfReportService.generateSalesReportPdf()** (OpenHTMLToPDF).

---

## 6. Security

- **Form login** at `/login`; **logout** at `/logout`.
- **Roles:** ADMIN, CASHIER (both can access all app pages in this project).
- **Passwords:** BCrypt (DataLoader, RegisterController).
- **CSRF:** Enabled; Thymeleaf adds `_csrf` to forms.
- **Public paths:** `/login`, `/register`, `/css/**`, `/js/**`, `/images/**`, `/h2-console/**`. All other paths require authentication.

---

## 7. Deployment

### Local

- **Run:** `./mvnw spring-boot:run`
- **URL:** http://localhost:8080
- **Database:** H2 file in `./data/` (persists between runs).

### Render

- **Repo:** GitHub (e.g. `arjavjain310/RetailNexus`).
- **Config:** `render.yaml` + `Dockerfile`. Service type: Web, runtime: Docker. Env: `SPRING_PROFILES_ACTIVE=render`.
- **Database:** H2 in-memory (data resets on each deploy). For persistent data, add Render PostgreSQL and update `application-render.properties`.
- **Flow:** Push to `main` → Render auto-deploys (if enabled). App URL: `https://<service-name>.onrender.com`.

---

## 8. Summary

| Topic | Summary |
|--------|---------|
| **What** | Retail management: login, products, inventory, billing (cart + invoice), dashboard, reports. No images; no invoice PDF. |
| **Backend** | Java 17, Spring Boot 3.2, Spring MVC, Spring Data JPA, Spring Security, Maven. |
| **Database** | H2 (file locally, in-memory on Render); JPA + SchemaMigrationRunner. |
| **Frontend** | Thymeleaf, Bootstrap 5, Chart.js, vanilla JS. |
| **PDF** | Only **Reports → Daily → Export PDF** uses PdfReportService. |
| **Deploy** | Local: `./mvnw spring-boot:run`. Cloud: push to GitHub, deploy on Render via Blueprint/Dockerfile. |

This file is the **complete explanation** of the RetailNexus project. For step-by-step deploy and push instructions, see **DEPLOY.md** and **docs/PUSH-TO-GITHUB.md**.
