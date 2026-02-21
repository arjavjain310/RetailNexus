# Why Does My Data Disappear? (Data Not Persisting)

## Short answer

**On Render (retailnexus.onrender.com):**  
Data is stored only **in memory**, not on disk. Every time the app **restarts** or **redeploys** (e.g. after a git push, or when the free tier spins down after inactivity), the process starts fresh and all data is **lost**. This is by design with the current setup.

**When running locally:**  
Data **does** persist in the **`data/`** folder (file `retailnexus.mv.db`) **unless** you run with the `render` profile or delete that folder.

---

## 1. Why data disappears on Render

On Render the app runs with the **`render`** profile. In **`application-render.properties`** you have:

```properties
# In-memory database (no file on disk)
spring.datasource.url=jdbc:h2:mem:retailnexus;...

# Schema is DROPPED when the app shuts down
spring.jpa.hibernate.ddl-auto=create-drop
```

So:

| Setting | Effect |
|--------|--------|
| **`jdbc:h2:mem:retailnexus`** | Database lives only in **RAM**. Nothing is written to disk. When the process stops, all data is gone. |
| **`ddl-auto=create-drop`** | When the app **shuts down**, Hibernate **drops** all tables. When it starts again, it creates empty tables. So even if the process kept running, a normal shutdown would wipe the schema. |

When does the process restart on Render?

- **Redeploy** – e.g. after you push to GitHub (if auto-deploy is on).
- **Free tier spin-down** – after some time with no traffic, Render stops the service; the next request starts it again with a **new** process and a **new** empty in-memory database.

So **every time** one of these happens, you get a **new, empty** database. That’s why “previous entered data gets removed every time” on Render.

---

## 2. Why data might disappear when running locally

When you run **locally** with the **default** profile (no `SPRING_PROFILES_ACTIVE=render`):

- **Database:** `jdbc:h2:file:./data/retailnexus` → data is stored in **`data/retailnexus.mv.db`** in your project.
- **Schema:** `ddl-auto=update` → tables are created/updated but **not** dropped on shutdown.

So data **should** persist between restarts.

Data can still “disappear” locally if:

1. **You run with the `render` profile**  
   Example: `SPRING_PROFILES_ACTIVE=render ./mvnw spring-boot:run`  
   Then the app uses **in-memory** H2 and **create-drop**, so data is lost on every restart.

2. **You delete the `data/` folder or `retailnexus.mv.db`**  
   That file **is** the database; deleting it wipes all data.

3. **You run from a different directory**  
   `./data/` is relative to the **current working directory**. If you start the app from another folder, a **new** `data/` (and new DB file) may be created there, and your previous data stays in the old project’s `data/` folder.

So: **locally, data persists only if you use the default profile and don’t delete `data/` and don’t run from a different directory.**

---

## 3. How to keep data

### When running **locally**

- Use the **default** profile (do **not** set `SPRING_PROFILES_ACTIVE=render`).
- Run from the **project root** (e.g. `RetailNexus/`).
- Do **not** delete the **`data/`** folder or **`retailnexus.mv.db`**.

Then your data stays in **`RetailNexus/data/retailnexus.mv.db`** and survives restarts.

### When running on **Render** (so data survives redeploys and spin-downs)

The app **supports persistent data on Render**. When the **DATABASE_URL** environment variable is set (e.g. by connecting a Render PostgreSQL database to your web service), the app uses **PostgreSQL** instead of in-memory H2 and data **persists**.

**Steps to store data every time on Render:**

1. In Render Dashboard → **New** → **PostgreSQL**. Create the database.
2. Open your **RetailNexus** web service → **Environment**.
3. **Connect** the PostgreSQL instance to the web service (from the Postgres **Info** → **Connect** → select your service). Render will add **DATABASE_URL** automatically. Or add **DATABASE_URL** manually and set it to the **Internal Database URL** from the Postgres instance (starts with `postgres://`).
4. **Redeploy** the web service.

The app detects **DATABASE_URL** and uses PostgreSQL with `ddl-auto=update`, so all data is stored in the database and **remains** across redeploys and spin-downs.

---

## 4. Summary

| Where you run | Why data disappears | How to keep data |
|---------------|---------------------|-------------------|
| **Render** | In-memory H2 + process restarts (redeploy/spin-down) | Use a **persistent DB** (e.g. Render PostgreSQL) and configure the app to use it when `render` profile is active. |
| **Locally** | Only if you use `render` profile, or delete `data/`, or run from another directory | Use **default** profile, run from project root, keep **`data/`** and **`retailnexus.mv.db`**. |

So: **“Previous entered data gets removed every time”** on **Render** because the app is using an **in-memory** database and the process restarts. To keep data on Render, you need to switch to a persistent database (e.g. PostgreSQL) and wire it in for the `render` profile.
