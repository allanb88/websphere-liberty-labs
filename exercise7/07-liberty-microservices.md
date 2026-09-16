# Exercise 7 — Liberty Microservices: Products CRUD

## Objective

Build a 3-tier CRUD application split across three independent services:

1. **PostgreSQL** — runs in a Docker container; holds the `products` table
2. **products-api** — a Jakarta Servlet REST API on Liberty (port 9081); handles CRUD over JDBC
3. **products-ui** — a JSP frontend on Liberty (port 9080); calls the API and renders an HTML table

By the end of this exercise you will have:
- A running PostgreSQL container seeded with sample data
- Two Liberty application images (`products-api`, `products-ui`) built and run entirely in Docker
- A browser UI where you can create, read, update, and delete products

**Everything runs in Docker.** You do not need a JDK, Maven, or a Liberty install on your
machine — each service's `Dockerfile` uses a multi-stage build: a `maven` build image
compiles the WAR, and a second stage bakes it into the same `icr.io/appcafe/websphere-liberty`
runtime image used since exercise 2. This is the same pattern as exercise 4 (application
images), applied to two services instead of one. A single `docker-compose.yml` at the root
of `exercise7/` builds both images, creates the shared network, and starts all three
containers in the right order with one command.

This exercise is Phase 1 of a migration learning path. Because containerization already
happens here, Phase 2 is simply pointing these same three images at a Minikube cluster
instead of your local Docker daemon — the images and `server.xml` files do not change.

---

## Concepts

### Microservice boundaries
Each service owns its own responsibility. The UI never talks to the database directly —
all data access flows through the REST API. This mirrors how a real WebSphere monolith
is broken apart during a migration to EKS.

### Servlet-based REST API
There is no JAX-RS or Spring Boot here. A single `@WebServlet` routes by HTTP method and
path info to implement GET / POST / PUT / DELETE — the same pattern used in exercises 2–5,
scaled up to a real data API.

### JNDI DataSource
Liberty provisions the JDBC connection pool and makes it available to the application via
JNDI (`java:comp/env/jdbc/products`). The application code never hard-codes a JDBC URL or
credentials — it always asks Liberty for the DataSource. This is the J2EE/Jakarta EE standard.

### Post-Redirect-Get in the UI
HTML forms can only `GET` or `POST`. The UI servlet intercepts every form POST, calls the
appropriate API method, then redirects back to `GET /ui/products`. This prevents duplicate
submissions on browser refresh.

### Container-to-container networking
`products-db`, `products-api`, and `products-ui` run as three separate containers joined to
one Docker network, `products-net`, that `docker-compose.yml` creates and names explicitly.
Docker's embedded DNS resolves each service's name as a hostname *for other containers on
that network* — so `products-api/server.xml` connects to the database at `products-db:5432`,
and `products-ui`'s `ApiClient` calls `http://products-api:9081/...`, never `localhost`.
Inside a container, `localhost` always means "this container," never a sibling container.
This is the same mental model as a Kubernetes `Service` DNS name — the exact problem
`products-net` solves here is what a K8s `Service` solves in Phase 2/3.

### Startup ordering with a healthcheck
`products-api` cannot serve requests until PostgreSQL accepts connections. `docker-compose.yml`
gives `products-db` a `healthcheck` (`pg_isready`) and makes `products-api` `depends_on` it with
`condition: service_healthy` — Compose blocks starting `products-api` until Postgres reports
healthy, instead of racing a fixed sleep or a manual "wait for the log line" step.

---

## Prerequisites

- Docker running locally, with the Compose v2 plugin (`docker compose ...`) — bundled with
  Docker Desktop; on Linux, install the `docker-compose-plugin` package if it's missing
- `curl` (for verifying the API) — `jq` is optional, for pretty-printing JSON

No Java, Maven, or Liberty installation is required — the Dockerfiles build the WAR files
using a `maven` build image and run them on the official Liberty image.

---

## Steps

### Step 1: Start the full stack

`exercise7/docker-compose.yml` defines all three services — `products-db`, `products-api`,
`products-ui` — the network they share, and the startup order between them.

From `exercise7/`:

~~~bash
cd exercise7
docker compose up --build -d
~~~

What this one command does, mapped to the individual pieces described above:

| Compose does this... | ...instead of you running |
|---|---|
| Creates the `products-net` network | `docker network create products-net` |
| Builds `products-api` and `products-ui` from their `Dockerfile`s | two `docker build` commands |
| Starts `products-db`, waits for its healthcheck, then starts `products-api` and `products-ui` | three ordered `docker run` commands |

**1.1 — Watch it come up**

~~~bash
docker compose ps
~~~

`products-db` should show `(healthy)` before `products-api` and `products-ui` show `Up` —
that ordering is the `depends_on: condition: service_healthy` rule in
`docker-compose.yml`, not luck.

### Step 2: Verify the database

~~~bash
docker compose exec products-db \
  psql -U liberty -d productsdb -c "SELECT * FROM products;"
~~~

Expected output:

~~~
 id |       name        | price  | stock
----+-------------------+--------+-------
  1 | Wireless Keyboard |  49.99 |   120
  2 | USB-C Hub         |  34.95 |    85
  3 | Mechanical Mouse  |  59.00 |    60
  4 | Monitor Stand     |  29.99 |    40
  5 | Laptop Sleeve 15" |  19.50 |   200
(5 rows)
~~~

> **Note:** If the init script did not run (e.g. a volume from a previous run was reused),
> run `docker compose down -v` to remove the containers and any volumes, then
> `docker compose up --build -d` again.

### Step 3: Verify products-api

The `products-api` project contains four Java classes:

| Class | Package | Role |
|-------|---------|------|
| `Product` | `com.example.api.model` | POJO — maps to a `products` table row |
| `ProductDAO` | `com.example.api.db` | All JDBC operations (list, find, create, update, delete) |
| `JsonUtil` | `com.example.api.util` | Serialize/deserialize `Product` ↔ JSON using `jakarta.json` |
| `ProductsServlet` | `com.example.api` | `@WebServlet("/products/*")` — routes by HTTP method and path |

`server.xml` provisions a `dataSource` bound to JNDI name `jdbc/products`, pointing at
`products-db:5432` (the database service's name on `products-net`, not `localhost`).

**3.1 — Check the startup logs**

~~~bash
docker compose logs -f products-api
~~~

Wait for:

~~~
[AUDIT] CWWKZ0001I: Application products-api started in ... seconds.
[AUDIT] CWWKF0011I: The defaultServer server is ready to run a smarter planet.
~~~

Press Ctrl-C to stop following the logs. If Liberty logs a `DSRA*` or connection-refused
error instead, check `docker compose ps` — `products-db` may not have reported healthy yet.

**3.2 — Exercise the API**

~~~bash
curl -s http://localhost:9081/api/products | jq
~~~

Expected: a JSON array of the 5 seeded products. This confirms the full chain —
Liberty → JNDI → JDBC connection pool → the `products-db` service — works end to end.

Try the other verbs:

~~~bash
# Create
curl -s -X POST http://localhost:9081/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Desk Mat","price":15.00,"stock":75}'

# Update (replace 6 with the id returned above)
curl -s -X PUT http://localhost:9081/api/products/6 \
  -H "Content-Type: application/json" \
  -d '{"name":"Desk Mat XL","price":18.00,"stock":50}'

# Delete
curl -s -X DELETE http://localhost:9081/api/products/6 -o /dev/null -w "%{http_code}\n"
~~~

### Step 4: Run the full stack through products-ui

The `products-ui` project contains three files:

| File | Package / Path | Role |
|------|-----------------|------|
| `ApiClient` | `com.example.ui` | Thin `HttpURLConnection` wrapper — GET/POST/PUT/DELETE against `products-api` |
| `ProductsController` | `com.example.ui` | `@WebServlet("/products/*")` — forwards GET to `products.jsp`; routes POST by a hidden `_method` field (Post-Redirect-Get) |
| `products.jsp` | `src/main/webapp` | Renders the product table and the create/edit/delete forms — no JavaScript fetch, plain HTML forms |

`ApiClient` is constructed with `http://products-api:9081/api/products` — the API
service's name on `products-net`, exactly like `products-db` is for the API's `server.xml`.
`products-ui` itself never touches PostgreSQL.

**4.1 — Open the UI**

~~~text
http://localhost:9080/ui/products
~~~

(`http://localhost:9080/ui/` also works — `index.jsp` redirects to `/ui/products`.)

You should see a table with the 5 seeded products (plus anything you created via `curl`
in Step 3.2).

**4.2 — Exercise the full CRUD round trip in the browser**

1. Fill in the **New product** form (name, price, stock) and click **Create**. The page
   redirects back to `GET /ui/products` and the new row appears — proving
   `products-ui → products-api → products-db` end to end.
2. Edit a row's fields and click **Save**. This sends `_method=PUT` to
   `ProductsController`, which calls `ApiClient.put(...)`.
3. Click **Delete** on a row (confirm the browser prompt). This sends
   `_method=DELETE`.
4. Refresh the page (`F5`) after a create — because of Post-Redirect-Get, the browser
   re-issues a `GET`, not a duplicate `POST`, so nothing is created twice.

**4.3 — Cross-check against the API directly**

~~~bash
curl -s http://localhost:9081/api/products | jq
~~~

Every change made through the UI should be visible here too — the UI holds no state of
its own; `products.jsp` only ever renders what the last `GET` from `products-api` returned.

---

## Exercises

> To be completed after the implementation steps are written.

---

## Questions

> To be completed after the implementation steps are written.

---

## Cleanup

~~~bash
docker compose down
~~~

This stops and removes all three containers and the `products-net` network in one step.
No named volume is defined for `products-db` in this exercise, so there is nothing extra
to prune — the next `docker compose up --build -d` starts from a fresh, freshly-seeded
database. (If you ever add a named volume for Postgres data, `docker compose down -v`
is what removes it too.)
