# RetailNexus

Lightweight, browser-based Retail Management System built with Java (Spring Boot), Thymeleaf, Bootstrap 5, and Chart.js. No product images or file storage—analytics-focused and easy to deploy.

## Tech Stack

- **Backend:** Spring Boot 3.2, Spring MVC, Spring Data JPA, Spring Security, Maven
- **Database:** Embedded H2 (file-based locally, in-memory on Render)
- **Frontend:** Thymeleaf, Bootstrap 5, Chart.js

## Features

- **Authentication:** Role-based login (Admin, Cashier)
- **Dashboard:** Total sales today, monthly revenue, profit, low stock, near expiry, dead stock counts; Monthly sales trend (line), category-wise sales (bar), profit distribution (pie); Restock suggestions
- **Product Management:** Add/Edit/Delete products (Name, Barcode, Category, Cost, Selling Price, GST %); search and filter
- **Inventory & Batches:** Batch number, expiry date, quantity; FIFO; auto stock deduction on sale
- **Billing:** Add to cart, GST calculation, block sale if no stock, invoice page, PDF download
- **Reports:** Daily/Monthly sales, Low stock, Dead stock; export to PDF

## Run Locally

**Requirements:** Java 17+, Maven 3.6+

```bash
cd RetailNexus
mvn spring-boot:run
```

Open: **http://localhost:8080**

**Default logins:**

| Role   | Username | Password   |
|--------|----------|------------|
| Admin  | admin    | admin123   |
| Cashier| cashier  | cashier123 |

**First run:** H2 creates the schema and seeds 55+ grocery products. Add stock from **Inventory → Add Stock** before making sales.

## Deploy to Render.com

The repo includes a **Blueprint** (`render.yaml`) and **Dockerfile** for one-click deploy:

1. Push the repo to GitHub (see [DEPLOY.md](DEPLOY.md)).
2. Go to **[dashboard.render.com](https://dashboard.render.com)** → **New** → **Blueprint**.
3. Connect GitHub and select this repo. Render will create a Web Service from the Dockerfile.
4. Click **Apply**. The app will be at `https://<your-service>.onrender.com`.

**Note:** With the `render` profile, H2 uses an in-memory database. Data resets on each deploy. For persistent data, use Render PostgreSQL and update `application-render.properties`.

## Project Structure

```
src/main/java/com/retailnexus/
├── RetailNexusApplication.java
├── config/          (Security, DataLoader)
├── controller/      (Login, Dashboard, Product, Inventory, Billing, Reports)
├── entity/          (User, Product, Batch, Sale, SaleItem, InventoryTransaction)
├── repository/
└── service/
src/main/resources/
├── application.properties
├── static/css/
└── templates/       (Thymeleaf: login, dashboard, products, inventory, billing, reports)
```

## Database (H2)

Tables: `users`, `products`, `batches`, `sales`, `sale_items`, `inventory_transactions`.  
No image columns or file storage. Schema is created/updated by JPA on startup.

## License

MIT.
