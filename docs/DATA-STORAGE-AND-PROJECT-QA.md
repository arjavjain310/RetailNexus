# Where Data Is Stored & Project Q&A

## Part 1: Where is the database data stored?

### When you run **locally** (on your machine)

- **Database:** H2 in **file** mode.
- **Connection URL (from `application.properties`):**  
  `jdbc:h2:file:./data/retailnexus;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE`
- **Physical location:** Inside your project folder, under the **`data/`** directory.
  - Full path example: `RetailNexus/data/`
  - H2 creates one or more files there, for example:
    - **`retailnexus.mv.db`** – main database file (tables, rows, indexes).
    - **`retailnexus.trace.db`** – optional trace/log file if H2 tracing is used.
  - `./data/` is relative to the **working directory** when you start the app (usually the project root, e.g. `RetailNexus`).
- **Persistence:** Data **persists** between restarts. It is lost only if you delete the `data/` folder or the `.mv.db` file.
- **`.gitignore`:** The project ignores `data/*.db` and `data/*.mv.db`, so database files are **not** committed to Git.

### When you run on **Render** (cloud)

- **Database:** H2 in **in-memory** mode (profile: `render`).
- **Connection URL (from `application-render.properties`):**  
  `jdbc:h2:mem:retailnexus;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- **Physical location:** **Nowhere on disk.** Data lives only in the **RAM** of the Render service.
- **Persistence:** Data is **not** stored permanently. It is **lost** when:
  - The service is redeployed (e.g. after a new push to GitHub),
  - The service restarts or scales down (e.g. free tier spin-down).

### Summary table

| Environment | Mode    | Where data is stored              | Persists? |
|-------------|---------|-----------------------------------|-----------|
| Local       | File    | Project folder: `data/` (e.g. `retailnexus.mv.db`) | Yes       |
| Render      | In-memory | RAM only                         | No        |

---

## Part 2: List of questions and answers (project Q&A)

### Project overview

**Q1. What is RetailNexus?**  
A lightweight retail management system for small shops. It provides login, product management, inventory (stock), billing (cart + invoice), dashboard with charts, and reports. It is browser-based and does not use product images or file uploads.

**Q2. Who are the users?**  
Admin and Cashier. Default logins: admin/admin123 and cashier/cashier123. Both roles can use dashboard, products, inventory, billing, and reports.

**Q3. Is there a mobile app?**  
No. It is a web application only, usable from any browser (including on mobile).

---

### Tech stack

**Q4. What is the backend technology?**  
Java 17 with Spring Boot 3.2: Spring MVC, Spring Data JPA, Spring Security, Maven.

**Q5. What database is used?**  
H2: embedded, file-based when run locally and in-memory when run on Render (render profile).

**Q6. What is used for the frontend?**  
Thymeleaf (server-rendered HTML), Bootstrap 5 (CSS/components), Chart.js (dashboard charts), and vanilla JavaScript (e.g. billing cart).

**Q7. How is authentication done?**  
Spring Security with form-based login. Passwords are stored hashed (BCrypt). Session-based; no JWT.

**Q8. How is the database schema created or updated?**  
JPA/Hibernate with `ddl-auto=update` (locally). A `SchemaMigrationRunner` runs after startup to add/alter columns (e.g. `products.unit`, `sale_items.quantity`) and drop the batches quantity check constraint. On Render, `ddl-auto=create-drop` with in-memory H2 recreates schema on each start.

---

### Data and database

**Q9. Where is the data stored when I run the app on my computer?**  
In the project’s **`data/`** folder. H2 creates files such as **`retailnexus.mv.db`** there. Path is relative to the working directory (usually the project root).

**Q10. Where is the data stored when the app runs on Render?**  
Nowhere on disk. H2 runs in **in-memory** mode; data exists only in RAM and is lost on redeploy or restart.

**Q11. What tables exist in the database?**  
`users`, `products`, `batches`, `sales`, `sale_items`, `inventory_transactions`.

**Q12. How is stock (inventory) stored?**  
Stock is stored in the **`batches`** table: each row has a product, batch number, expiry date, and quantity. Total stock for a product is the sum of quantities of all its batches. The UI does not show batch number or expiry; you only set or add “stock” per product.

**Q13. Can stock go negative?**  
Yes. When a sale exceeds available stock (oversell), the app still completes the sale and deducts from batches; batch quantity can become negative. A migration drops the DB check constraint that would have blocked this.

**Q14. How is a sale stored?**  
A **Sale** row holds date, totals, payment method, and sold-by user. Each line item is a **SaleItem**: product, batch, quantity (decimal), unit price, GST, total price, profit. Sale and SaleItems are created in one transaction; then batch quantities are updated (deducted).

---

### Features and behaviour

**Q15. Can I download the invoice as PDF?**  
No. The invoice PDF download feature was removed. You only view the invoice in the browser; there is a “New Sale” button to go back to billing.

**Q16. Is there any PDF export?**  
Yes. Only **Reports → Daily sales** has an “Export PDF” option (daily sales report PDF).

**Q17. How does billing work?**  
User adds products to a cart with quantity (e.g. 0.7 kg, 2 L). Optionally edits unit price and GST % per line. On “Complete”, the server creates a Sale and SaleItems, deducts stock from batches (FIFO), and redirects to the invoice page.

**Q18. What units are supported for products?**  
KG, LITRE, and PIECES. Quantity in billing can be decimal for KG/L (e.g. 0.7 kg). Display uses labels like “0.7 kg”, “2 L”, or plain number for pieces.

**Q19. What happens if two products have the same barcode?**  
The app blocks saving a product whose barcode already exists for another product and shows an error message (e.g. “Another product with this barcode already exists”).

**Q20. How does the dashboard get its numbers?**  
DashboardService uses repositories: SaleRepository (e.g. today’s total, monthly revenue, profit), BatchRepository (stock sums, near expiry), SaleItemRepository (category-wise sales), ProductRepository, and RestockSuggestionService (last 30 days sales) for suggestions.

**Q21. What are “restock suggestions”?**  
Based on sales per product over the last 30 days, the app suggests products whose current stock is below about a week’s worth of average daily sales (and optionally an order quantity).

**Q22. What are “low stock” and “dead stock”?**  
Low stock: products whose total batch quantity is between 1 and a threshold (e.g. 10). Dead stock: products that have stock but had no sales in the last 30 days.

---

### Security and configuration

**Q23. How are passwords stored?**  
Hashed with BCrypt. Plain passwords are never stored.

**Q24. Which URLs are public (no login)?**  
/login, /register, /css/**, /js/**, /images/**, /h2-console/**. All other paths require an authenticated user.

**Q25. Is CSRF protection enabled?**  
Yes. Spring Security enables CSRF; Thymeleaf includes the CSRF token in forms.

**Q26. Where is the H2 console and when is it available?**  
At **/h2-console** when `spring.h2.console.enabled=true` (local). It is disabled in the Render profile. JDBC URL for local file DB is the same as in `application.properties` (e.g. `jdbc:h2:file:./data/retailnexus`); username `sa`, password blank unless set.

---

### Running and deploying

**Q27. How do I run the app locally?**  
From the project root: `./mvnw spring-boot:run`. Open http://localhost:8080. Java 17+ is required.

**Q28. How do I push the code to GitHub?**  
Use Git: `git add .`, `git commit -m "..."`, `git push origin main`. For HTTPS, use a Personal Access Token as the password. See `docs/PUSH-TO-GITHUB.md` for details.

**Q29. How do I deploy on Render?**  
Connect the GitHub repo to Render, use the repo’s Dockerfile and render.yaml (Blueprint). Set env `SPRING_PROFILES_ACTIVE=render`. Render builds the image and runs the jar; the app uses in-memory H2 unless you switch to an external DB.

**Q30. Why is Vercel not used for this app?**  
Vercel is aimed at static sites and serverless functions. This app is a long-running Java/Spring Boot server, so it is deployed on Render (or similar) instead. Vercel could only host a static landing page, not the actual application.

**Q31. Does the app need an external database in production?**  
No for a demo or short-lived data: Render can run it with in-memory H2.

---

### Architecture and code

**Q32. What is the overall architecture?**  
Layered: Controllers handle HTTP, Services contain business logic, Repositories (JPA) access the database, Entities map to tables. Spring Security handles authentication and authorization.

**Q33. How is the invoice page rendered?**  
BillingController’s GET `/billing/invoice/{id}` loads the Sale (with items) via SaleService and returns the Thymeleaf template `billing/invoice.html`, which displays sale details and line items. No PDF is generated for the invoice.

**Q34. Why was the invoice PDF removed?**  
The PDF download was causing a 500 error (e.g. lazy-load or environment issues). It was removed so the invoice remains view-only in the browser without errors.

**Q35. What is SchemaMigrationRunner?**  
An ApplicationRunner that runs after the app starts. It applies DB changes: add `products.unit` if missing, alter `sale_items.quantity` to decimal, and drop the check constraint on `batches.quantity` so quantity can go negative.

**Q36. What is DataLoader?**  
A CommandLineRunner that seeds initial data: admin and cashier users (if none exist), and a set of grocery products with default stock in batches (if no products exist).

---

### Common “how” and “where” questions

**Q37. Where are products defined at startup?**  
In **DataLoader**: it builds a list of Product entities (name, barcode, category, cost, selling price, GST, unit) and saves them; then it creates batches (e.g. INIT-0001) with default quantity per product.

**Q38. Where is the cart stored during billing?**  
The cart is not stored in the database until the user clicks Complete. It is held in the browser (e.g. form fields or JavaScript) and submitted as a single string (e.g. `id:qty:price:gst;...`) when the form is posted.

**Q39. Where are session and login state stored?**  
In the server’s HTTP session (in-memory by default). Session timeout is configured in `application.properties` (e.g. 30 minutes).

**Q40. Where is the main application entry point?**  
`RetailNexusApplication.java` with the `main` method; it runs Spring Boot.

---

This file answers **where database data is stored** (local file vs Render in-memory) and provides a **list of questions and answers** that can be asked about the project (overview, tech stack, data, features, security, deployment, and architecture).
