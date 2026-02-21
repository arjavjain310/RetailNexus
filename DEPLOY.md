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

**Note:** With the `render` profile, H2 uses an in-memory database. Data resets on each deploy. For persistent data, add a Render PostgreSQL database and update `application-render.properties`.
