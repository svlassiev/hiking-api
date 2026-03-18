# hiking-api

Kotlin/Ktor backend for the hiking photo gallery at [serg.vlassiev.info/hiking](https://serg.vlassiev.info/hiking).

## Architecture

```
Browser → nginx (hiking-ui) → hiking-api (Ktor/JVM) → MongoDB
                                      ↕
                                Google Cloud Storage
                              gs://colorless-days-children/
```

- **Runtime:** Kotlin 1.3 / Ktor 1.2 / JDK 8
- **Database:** MongoDB 7 (standalone, 1Gi PVC)
- **Storage:** GCS bucket for photos, MongoDB for metadata (albums, image URLs, EXIF data)
- **Auth:** Firebase Admin SDK (token verification for edit endpoints)
- **Docker image:** `svlassiev/hiking-api`
- **Cluster:** GKE `sixty-years-to-death` (europe-north1-a, project `thematic-acumen-225120`)

## CI/CD

Automated via GitHub Actions. Every push to `master`:

1. Builds Docker image and pushes to Docker Hub (tagged with commit SHA + `latest`)
2. Authenticates to GCP and gets GKE credentials
3. Applies K8s manifests (`k8s/hiking-api.yml`)
4. Deploys new image to the cluster

**Required GitHub secrets:** `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `GCP_SA_KEY`

## API Endpoints

**Public:**
- `GET /hiking-api/` — health check
- `GET /hiking-api/folders` — all albums sorted by date
- `POST /hiking-api/images` — paginated images by IDs
- `GET /hiking-api/timeline/data` — full timeline
- `GET /hiking-api/timeline/data/head` — first album
- `GET /hiking-api/timeline/data/tail` — remaining albums

**Share previews (Open Graph):** serves both colorless and hiking share links
- `GET /share/{folder}` — colorless album preview (OG tags + redirect)
- `GET /share/{folder}/{n}` — colorless photo preview
- `GET /share/hiking/album/{listId}` — hiking album preview
- `GET /share/hiking/image/{imageId}` — hiking photo preview

Colorless share routes resolve album metadata from the colorless-days-children nginx service (no DB needed). Hiking share routes use MongoDB.

**Admin (requires Firebase ID token):**
- `POST /hiking-api/edit/images-lists` — create album
- `POST /hiking-api/edit/images` — add image (uploads to GCS, resizes to 4 variants)
- `PUT /hiking-api/edit/images-lists/{id}/name` — rename album
- `DELETE /hiking-api/edit/images-lists/{id}` — delete album
- `DELETE /hiking-api/edit/images-lists/{id}/images/{imageId}` — delete image

## Local Development

```bash
docker build -t hiking-api .
docker run -p 9090:9090 hiking-api
```

## K8s Manifests

- `k8s/hiking-api.yml` — Service + Deployment (managed by CI/CD)
- `k8s/secrets.yml` — Secret templates (apply manually, not via CI/CD)
- `k8s/mongo.yml` — Legacy MongoDB StatefulSet (no longer used; MongoDB is now a standalone Deployment managed separately)
