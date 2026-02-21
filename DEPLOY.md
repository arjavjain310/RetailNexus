# Deploy RetailNexus: GitHub → Render

## 1. Push to GitHub

Run in your terminal (you’ll be asked to sign in):

```bash
cd /Users/arjavjain/RetailNexus
git push -u origin main
```

- **Username:** `arjavjain310`
- **Password:** use a [Personal Access Token](https://github.com/settings/tokens) (not your GitHub password)

Repo: **https://github.com/arjavjain310/RetailNexus**

---

## 2. Deploy on Render

1. Go to **[dashboard.render.com](https://dashboard.render.com)** → **New** → **Blueprint**.
2. Connect GitHub and select **arjavjain310/RetailNexus**.
3. Render reads `render.yaml` and `Dockerfile` and creates a **Web Service**.
4. Click **Apply**. After the first deploy the app is live at **https://retailnexus.onrender.com** (or the URL shown in the dashboard).

---

## 3. (Optional) Store data on Render so it persists

By default the app uses an **in-memory** database on Render, so data is lost on redeploy or spin-down. To **keep data every time**:

1. In the Render Dashboard, go to **New** → **PostgreSQL** (or add a Postgres instance in the same project).
2. Create the database (name, region, plan). Note the **Internal Database URL** (or **External** if your app is in another region).
3. Open your **RetailNexus Web Service** → **Environment**.
4. Click **Add Environment Variable**. Render can **link** the database to the service:
   - Go to the **PostgreSQL** instance → **Info** → **Connections**.
   - Under **Connect**, select your **RetailNexus** web service and click **Connect**. Render will add **DATABASE_URL** to the web service automatically.
5. If you added it manually instead: add **Key** `DATABASE_URL`, **Value** = the **Internal Database URL** from the Postgres instance (starts with `postgres://`).
6. **Redeploy** the web service (Manual Deploy → Deploy latest commit, or push to GitHub if auto-deploy is on).

After that, the app uses **PostgreSQL** when `DATABASE_URL` is set: data is stored in the database and **persists** across redeploys and spin-downs.
