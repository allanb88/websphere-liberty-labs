# Exercise 7 — Liberty Microservices: Products CRUD

## Top-Level Overview

Build a 3-tier CRUD application split into independent microservices, running entirely in Docker
(no local JDK/Maven/Liberty install — each service's Dockerfile builds its own WAR via a Maven
build stage). This is Phase 1 of a broader migration learning path (Docker Compose-style local
containers → Minikube deployment in Phase 2/3). Originally scoped as "Liberty local install first,
containerize later," but pivoted mid-implementation per explicit user direction — see the
Implementation Notes under Sub-Task 5 for the full rationale.

**Domain**: `products` table — columns: `id`, `name`, `price`, `stock`.

**Three services:**

| Service | Runtime | Port | Role |
|---------|---------|------|------|
| `db` | PostgreSQL (Docker container) | 5432 | Data store |
| `products-api` | Liberty (local install or `wlp`) | 9081 | Jakarta Servlet REST API (JSON) |
| `products-ui` | Liberty (local install or `wlp`) | 9080 | JSP frontend — calls the API |

**Conventions followed from existing labs:**
- Java 11, Jakarta Servlet 6.0 / JSP 3.1, Maven WAR packaging
- `server.xml` explicit app registration (`<webApplication>`) pattern from exercise 3+
- `provided` scope for all Jakarta APIs
- Liberty base image: `icr.io/appcafe/websphere-liberty:latest` (used from exercise 4 onward; kept for reference)

---

## Sub-Tasks

---

### Sub-Task 1 — Project Skeleton and Directory Layout

**Status:** `[x] done`

**Intent:**
Create the `exercise7/` directory tree with all Maven project structures,
`server.xml` stubs, and a top-level `README.md` for the exercise.
No Java or SQL code yet — just the scaffolding every subsequent task fills in.

**Expected Outcomes:**
- `exercise7/` directory exists with the layout below
- Each Maven project has a valid `pom.xml` (compilable, no source yet)
- Each Liberty service has a `server.xml` stub with the correct port and feature set declared
- `exercise7/07-liberty-microservices.md` exists as the lab instruction file (skeleton, filled in later)

**Directory Layout to Create:**
```
exercise7/
├── 07-liberty-microservices.md          # Lab instruction file (skeleton)
├── db/
│   └── init.sql                         # DDL + seed data (filled in Sub-Task 2)
├── products-api/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/example/api/    # (empty, filled in Sub-Task 3)
│       │   ├── resources/
│       │   └── webapp/
│       │       └── WEB-INF/
│       │           └── web.xml          # minimal descriptor
│       └── test/
│   └── server.xml                       # Liberty config, port 9081
└── products-ui/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/example/ui/     # (empty, filled in Sub-Task 5)
        │   └── webapp/
        │       ├── WEB-INF/
        │       │   └── web.xml
        │       └── index.jsp            # placeholder (filled in Sub-Task 5)
        └── test/
    └── server.xml                       # Liberty config, port 9080
```

**Todo List:**
1. Create `exercise7/db/` directory and empty `init.sql` placeholder
2. Create `exercise7/products-api/pom.xml`
   - `groupId: com.example`, `artifactId: products-api`, `version: 1.0`, `packaging: war`
   - `maven.compiler.release: 11`
   - Dependencies: `jakarta.servlet-api:6.0.0` (provided), `jakarta.json-api:2.1.0` (provided), PostgreSQL JDBC driver `42.7.3` (compile scope)
   - `maven-war-plugin:3.4.0`, `finalName: products-api`
3. Create `exercise7/products-api/src/main/webapp/WEB-INF/web.xml` (minimal Jakarta EE 10 descriptor)
4. Create `exercise7/products-api/server.xml`
   - Features: `servlet-6.0`, `jdbc-4.2`, `jndi-1.0`
   - `httpEndpoint` on port 9081
   - `dataSource` JNDI name `jdbc/products` pointing to PostgreSQL on `localhost:5432`
   - `webApplication` element registering `products-api.war` at context root `/api`
5. Create `exercise7/products-ui/pom.xml`
   - Same structure; `artifactId: products-ui`; `finalName: products-ui`
   - Dependencies: `jakarta.servlet-api:6.0.0` (provided), `jakarta.pages-api:3.1.0` (provided)
   - No JDBC driver (UI talks to the API, not the DB directly)
6. Create `exercise7/products-ui/src/main/webapp/WEB-INF/web.xml`
7. Create `exercise7/products-ui/server.xml`
   - Features: `servlet-6.0`, `pages-3.1`
   - `httpEndpoint` on port 9080
   - `webApplication` at context root `/ui`
8. Create `exercise7/07-liberty-microservices.md` skeleton with Objective and placeholder Steps sections

**Relevant Context:**
- Pattern from [`exercise2/example/hello-app/pom.xml`](../exercise2/example/hello-app/pom.xml)
- Pattern from [`exercise2/example/hello-app/server.xml`](../exercise2/example/hello-app/server.xml)
- JNDI + JDBC feature usage documented in Liberty docs (`jdbc-4.2`, `jndi-1.0`)

---

### Sub-Task 2 — Database: PostgreSQL Container + Schema

**Status:** `[x] done`

**Intent:**
Define the `products` schema and seed data, and document how to start the PostgreSQL
container so developers can run it with a single `docker run` command. The API in
Sub-Task 3 depends on this being running first.

**Expected Outcomes:**
- `exercise7/db/init.sql` contains the DDL and seed INSERT statements
- Running the prescribed `docker run` command starts Postgres on port 5432 with DB `productsdb`, user `liberty`, password `liberty`
- Connecting with `psql` shows the `products` table with 3–5 seed rows
- `exercise7/07-liberty-microservices.md` Step 1 section documents the exact `docker run` command

**Todo List:**
1. Write `exercise7/db/init.sql`:
   ```sql
   CREATE TABLE products (
     id    SERIAL PRIMARY KEY,
     name  VARCHAR(100) NOT NULL,
     price NUMERIC(10,2) NOT NULL,
     stock INT NOT NULL DEFAULT 0
   );
   -- seed data (5 rows)
   INSERT INTO products (name, price, stock) VALUES ...
   ```
2. Document the `docker run` command in the lab file Step 1:
   ```bash
   docker run -d \
     --name products-db \
     -e POSTGRES_DB=productsdb \
     -e POSTGRES_USER=liberty \
     -e POSTGRES_PASSWORD=liberty \
     -p 5432:5432 \
     -v "$(pwd)/exercise7/db/init.sql:/docker-entrypoint-initdb.d/init.sql" \
     postgres:16
   ```
3. Document a verification step using `psql` or `docker exec` to confirm the table exists

**Relevant Context:**
- `products-api/server.xml` dataSource will reference `localhost:5432/productsdb`
- PostgreSQL JDBC URL: `jdbc:postgresql://localhost:5432/productsdb`
- Init script is auto-executed by the official `postgres` Docker image on first start

---

### Sub-Task 3 — products-api: REST Servlet (CRUD over JDBC)

**Status:** `[x] done`

**Intent:**
Implement the `products-api` WAR: a single Jakarta Servlet that handles
GET / POST / PUT / DELETE over `/api/products` and responds with JSON.
Uses JDBC (JNDI `jdbc/products`) to talk to PostgreSQL. No framework — plain servlet + JDBC,
consistent with the lab's existing servlet-first pattern.

**Expected Outcomes:**
- `mvn clean package` in `products-api/` produces `target/products-api.war` without errors
- Dropping the WAR into a Liberty instance with the configured `server.xml` starts cleanly
- `curl http://localhost:9081/api/products` returns a JSON array of all products
- `curl -X POST` creates a product; `PUT` updates; `DELETE` removes
- All responses use `Content-Type: application/json`

**Classes to Create:**

| Class | Path | Responsibility |
|-------|------|----------------|
| `ProductsServlet` | `com.example.api.ProductsServlet` | `@WebServlet("/products/*")` — routes by method + path info |
| `Product` | `com.example.api.model.Product` | Plain Java bean: id, name, price, stock |
| `ProductDAO` | `com.example.api.db.ProductDAO` | All JDBC operations (list, findById, create, update, delete) |
| `JsonUtil` | `com.example.api.util.JsonUtil` | Serialize/deserialize `Product` ↔ JSON string (manual or `jakarta.json`) |

**Todo List:**
1. Create `Product.java` — simple POJO with getters/setters for `id` (int), `name` (String), `price` (BigDecimal), `stock` (int)
2. Create `ProductDAO.java`
   - Constructor takes a `javax.sql.DataSource` (looked up from JNDI in the servlet)
   - Methods: `List<Product> findAll()`, `Product findById(int)`, `Product create(Product)`, `Product update(int, Product)`, `void delete(int)`
   - Uses `try-with-resources` for all Connection/PreparedStatement handling
3. Create `JsonUtil.java`
   - `String toJson(Product)` and `String toJson(List<Product>)` — use `jakarta.json.Json` builder
   - `Product fromJson(String)` — parse request body into a `Product`
4. Create `ProductsServlet.java`
   - `@WebServlet("/products/*")`
   - `init()`: look up `DataSource` via `InitialContext` → `jdbc/products`, instantiate `ProductDAO`
   - `doGet()`: path info null or `/` → list all; path `/123` → find by id
   - `doPost()`: read request body, parse JSON, call `dao.create()`, return 201
   - `doPut()`: parse path id + body, call `dao.update()`, return 200
   - `doDelete()`: parse path id, call `dao.delete()`, return 204
   - Shared helper: `writeJson(resp, int status, String json)`
5. Update `exercise7/07-liberty-microservices.md` Step 2 with build and run instructions:
   ```bash
   cd exercise7/products-api
   mvn clean package
   # copy WAR to Liberty dropins or use server.xml webApplication
   ```

**Relevant Context:**
- Pattern from [`exercise2/example/hello-app/src/main/java/example/HelloServlet.java`](../exercise2/example/hello-app/src/main/java/example/HelloServlet.java)
- `server.xml` already declares `jdbc/products` dataSource and `jdbc-4.2` feature
- JNDI lookup: `new InitialContext().lookup("java:comp/env/jdbc/products")`
- Liberty `jdbc-4.2` + `jndi-1.0` features must be active for JNDI DataSource resolution

---

### Sub-Task 4 — products-api: Liberty server.xml DataSource Wiring

**Status:** `[x] done`

**Intent:**
Complete the `products-api/server.xml` so Liberty can provision the PostgreSQL
DataSource via JNDI. This includes the `library` element referencing the JDBC driver JAR,
the `dataSource` element, and the `connectionManager`. The Liberty server must start
with zero warnings about unresolvable JNDI resources.

**Expected Outcomes:**
- Liberty starts with no FFDC or NullPointerException on DataSource lookup
- `curl http://localhost:9081/api/products` returns data from PostgreSQL (proves JNDI wiring works end-to-end)
- `server.xml` is self-documented with comments explaining each element (lab convention)

**Complete `server.xml` Elements Needed:**

```xml
<server>
  <featureManager>
    <feature>servlet-6.0</feature>
    <feature>jdbc-4.2</feature>
    <feature>jndi-1.0</feature>
  </featureManager>

  <httpEndpoint id="defaultHttpEndpoint" host="*"
                httpPort="9081" httpsPort="9444" />

  <!-- JDBC driver: PostgreSQL -->
  <library id="PostgreSQLLib">
    <fileset dir="${server.config.dir}/lib" includes="postgresql-*.jar" />
  </library>

  <!-- DataSource mapped to JNDI name jdbc/products -->
  <dataSource id="productsDS" jndiName="jdbc/products">
    <jdbcDriver libraryRef="PostgreSQLLib" />
    <properties.postgresql
        serverName="localhost" portNumber="5432"
        databaseName="productsdb"
        user="liberty" password="liberty" />
    <connectionManager maxPoolSize="10" />
  </dataSource>

  <webApplication location="products-api.war" contextRoot="/api">
    <classloader commonLibraryRef="PostgreSQLLib" />
  </webApplication>
</server>
```

**Todo List:**
1. Finalize `exercise7/products-api/server.xml` with all elements above
2. Document in lab file: the PostgreSQL JDBC driver JAR must be placed in `${server.config.dir}/lib/` (i.e. the `lib/` folder inside the Liberty server directory). Add the step:
   ```bash
   # Copy the JDBC driver into the Liberty server lib folder
   cp ~/.m2/repository/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar \
      $WLP_HOME/usr/servers/productsApiServer/lib/
   ```
3. Document how to create the Liberty server instance:
   ```bash
   $WLP_HOME/bin/server create productsApiServer
   cp exercise7/products-api/server.xml $WLP_HOME/usr/servers/productsApiServer/
   cp exercise7/products-api/target/products-api.war $WLP_HOME/usr/servers/productsApiServer/apps/
   $WLP_HOME/bin/server run productsApiServer
   ```
4. Add a verification step (`curl`) to the lab Markdown

**Relevant Context:**
- Exercise 3 covers explicit `<webApplication>` pattern: [`exercise3/03-liberty-configuration.md`](../exercise3/03-liberty-configuration.md)
- Exercise 4 covers application image patterns: [`exercise4/04-application-image.md`](../exercise4/04-application-image.md)
- `${server.config.dir}` resolves to the server's own directory inside Liberty

**Implementation Notes:**
- `server.xml` now has the `library`/`dataSource`/`connectionManager` block exactly as specified, plus
  `<classloader commonLibraryRef="PostgreSQLLib" />` on the `webApplication` so the WAR can see the
  PostgreSQL driver classes without bundling them again (the driver is already in the WAR per `pom.xml`,
  but the commonLibraryRef keeps the pattern consistent with a driver-in-lib/ deployment).
- `07-liberty-microservices.md` Step 2 now has 2.3 (start server, expected `CWWKZ0001I`/`CWWKF0011I` audit
  lines) and 2.4 (curl verification for GET/POST/PUT/DELETE) filled in.
- **Superseded by Sub-Task 5's pivot below**: the user does not want a local `$WLP_HOME` install at all.
  `server.xml`'s `dataSource.properties.postgresql.serverName` was changed from `localhost` to `products-db`
  (the DB container's name on the `products-net` Docker network), and Step 2 of the lab file was rewritten
  to `docker build` / `docker run` the API as a container instead of a local Liberty server instance. See
  Sub-Task 5 notes for the full pivot and the real end-to-end validation that was run.

---

### Sub-Task 5 — products-ui: JSP Frontend (CRUD via API calls)

**Status:** `[x] done`

**Intent:**
Build the `products-ui` WAR: a JSP page that uses a Java backing servlet to proxy
CRUD operations to the `products-api`. The JSP renders the products table as HTML
and provides forms for create/update/delete. The UI never talks directly to the database —
all data flows through the REST API, demonstrating the microservice boundary.

**Expected Outcomes:**
- `mvn clean package` in `products-ui/` produces `target/products-ui.war`
- Navigating to `http://localhost:9080/ui/products` shows an HTML table of all products
- A "New Product" form submits a POST that creates a row (via API) and refreshes the table
- Edit and Delete buttons trigger the corresponding API calls and refresh the view
- The JSP uses no JavaScript fetch — all actions go through form POST to the backing servlet (server-side round-trip), keeping the implementation simple and J2EE-idiomatic

**Classes and Files to Create:**

| File | Path | Responsibility |
|------|------|----------------|
| `ProductsController` | `com.example.ui.ProductsController` | `@WebServlet("/products/*")` — handles form submissions, calls API via `HttpURLConnection`, forwards to JSP |
| `ApiClient` | `com.example.ui.ApiClient` | Thin wrapper: `GET/POST/PUT/DELETE` to `http://localhost:9081/api/products` using `HttpURLConnection` |
| `products.jsp` | `src/main/webapp/products.jsp` | Renders product table + create form + edit/delete buttons; reads `List<Map>` from request attribute |

**Todo List:**
1. Create `ApiClient.java`
   - Field: `String baseUrl = "http://localhost:9081/api/products"`
   - Methods: `String get(String path)`, `String post(String json)`, `String put(String path, String json)`, `void delete(String path)`
   - Uses `java.net.HttpURLConnection` — no third-party HTTP client
2. Create `ProductsController.java`
   - `@WebServlet("/products/*")`
   - `doGet()`: calls `ApiClient.get("/")`, parses JSON response into a `List<Map<String,String>>`, sets as request attribute `products`, forwards to `products.jsp`
   - `doPost()` with hidden `_method` field to handle PUT and DELETE via HTML forms:
     - `_method=POST`: create
     - `_method=PUT`: update by id
     - `_method=DELETE`: delete by id
   - After each mutation, redirects back to GET (Post-Redirect-Get pattern)
3. Create `products.jsp`
   - JSTL `<c:forEach>` to render the products table
   - Columns: Name, Price, Stock, Actions (Edit / Delete)
   - A "New Product" form below the table with inputs for name, price, stock
   - Inline "Edit" form per row (or a small edit form revealed per row)
   - Hidden `<input name="_method">` for routing in `ProductsController`
4. Update `products-ui/pom.xml` to add JSTL dependency: `jakarta.servlet.jsp.jstl-api:3.0.0` (provided) + `jakarta.servlet.jsp.jstl:3.0.1` (compile, bundled in WAR)
5. Update `products-ui/server.xml` to add `pages-3.1` feature and register the WAR
6. Document in lab file Step 3: build, deploy to Liberty, and browse to `http://localhost:9080/ui/products`

**Relevant Context:**
- `products-ui` runs on port 9080; `products-api` runs on port 9081 — both Liberty instances run in parallel
- `ApiClient` uses `java.net.HttpURLConnection` (Java 11 stdlib, no extra deps)
- JSTL 3.x is the Jakarta EE 10 version, matching the servlet-6.0 / pages-3.1 feature level

**Implementation Notes — pivot to all-Docker, no local JDK/Maven/Liberty:**

Mid-task the user clarified they do not want to install anything locally (no JDK, no Maven, no
WLP_HOME Liberty extraction) — everything must run in Docker, including the build. This changed
the shape of both Sub-Task 4 and Sub-Task 5:

- **Both services now have a `Dockerfile`** (`products-api/Dockerfile`, `products-ui/Dockerfile`)
  using a two-stage build: `maven:3.9-eclipse-temurin-11` compiles the WAR (and, for the API,
  extracts the PostgreSQL driver via a `maven-dependency-plugin` execution bound to `package`,
  writing `target/liberty-lib/postgresql-driver.jar`), then a final stage copies the WAR (+ driver
  + `server.xml`) onto `icr.io/appcafe/websphere-liberty:latest`. Verified the tag
  `maven:3.9-eclipse-temurin-11` exists via `docker manifest inspect` before committing to it.
- **`products-api/server.xml`**: `dataSource.properties.postgresql.serverName` changed from
  `localhost` to `products-db` — the two are separate containers, so `localhost` inside
  `products-api` would mean "myself," not the database.
- **`products-ui`**: `ApiClient`'s `baseUrl` is `http://products-api:9081/api/products` for the
  same reason. Both containers join a user-defined network, `products-net`, created once in
  Step 1 of the lab file — this is the exact problem a Kubernetes `Service` DNS name solves in
  Phase 2/3, called out explicitly as a new "Container-to-container networking" Concept in the
  lab file.
- **`products-ui/pom.xml`** and **`server.xml`** gained a `jakarta.json-api` dependency and the
  `jsonp-2.1` feature respectively — not in the original Sub-Task 5 spec, but required because
  `ProductsController` parses the API's JSON array response and builds JSON request bodies; there
  was no other Jakarta-standard way to do this without a third-party library.
- **Found and fixed a real bug from Sub-Task 3** while build-validating: `ProductsServlet.java`
  uses `@Resource` from `jakarta.annotation`, but `products-api/pom.xml` never declared
  `jakarta.annotation-api` as a dependency — `mvn package` failed on
  `package jakarta.annotation does not exist`. Added it as `provided` scope.
- **Found and fixed a second real bug from Sub-Task 3**: `JsonUtil.fromJson` called
  `obj.getString("price")`, but `JsonUtil.toJson` serializes `price` as a JSON *number*
  (`.add("price", p.getPrice())`), and `JsonObject.getString` throws `ClassCastException` on a
  non-string value. Any client sending `{"price":15.00}` (e.g. the curl examples in the lab file)
  would have broken `POST`/`PUT` at runtime. Fixed `fromJson` to accept price as either a JSON
  string or a JSON number. `ProductsController.toJson()` on the UI side still sends price as a
  quoted string (avoids HTML-input locale/formatting ambiguity), which the fix also handles.
- **Ran the full stack for real** (not just `mvn package`): `docker network create products-net`,
  started `products-db`, `products-api`, `products-ui`, confirmed Liberty's `CWWKZ0001I` /
  `CWWKF0011I` audit lines for both servers, then drove the entire CRUD cycle — create, update,
  delete — as HTTP form posts against `products-ui` (simulating the browser) and cross-checked
  every mutation against `products-api` directly. All four operations worked end to end through
  the real JNDI → JDBC → Postgres chain. Cleaned up all validation containers and the network
  afterward; nothing was left running.
- `07-liberty-microservices.md` was substantially rewritten: Prerequisites no longer mention
  Java/Maven/Liberty; Step 1 gained network creation; Steps 2 and 3 now build/run Docker images
  instead of local `$WLP_HOME` server instances; Step 4 (full stack) is new, covering both the
  browser flow and a `curl` cross-check. Exercises/Questions remain deferred to Sub-Task 6 as
  originally scoped.

**Follow-up — replaced manual `docker network`/`docker build`/`docker run` with `docker-compose.yml`:**

The user asked for a single Compose file to manage the network and let them run "just this one
file" instead of the multi-command manual flow above. Added `exercise7/docker-compose.yml`:
- Three services — `products-db` (plain `postgres:16`, no build), `products-api` and
  `products-ui` (each `build: ./<dir>`, using the Dockerfiles already validated above).
- `networks.default.name: products-net` — keeps the network name identical to what's already
  documented in the lab file's "Container-to-container networking" Concept, so no doc rewrite
  was needed for that section's substance, only its mechanism (Compose creates it, not a manual
  `docker network create`).
- `products-db` has a `healthcheck` (`pg_isready`); `products-api` uses
  `depends_on: products-db: condition: service_healthy`, so Compose blocks starting the API
  until Postgres actually accepts connections — removes the old "watch the logs for the ready
  line" manual step entirely.
- Re-validated the full stack with `docker compose up --build -d`, confirmed
  `products-db` reports `(healthy)` before `products-api`/`products-ui` start, waited for both
  Liberty `CWWKF0011I` lines, and re-ran the same create/update/delete round trip through
  `products-ui` as before. All four operations worked; `docker compose down` cleanly removed
  the containers and the network with nothing left behind (no named volume is defined, so
  `down` alone is sufficient — no `-v` needed for this exercise).
- `07-liberty-microservices.md` Steps 1–4 were restructured around `docker compose up --build -d`
  as the single entry point (Step 1), with Steps 2–4 now being verification of what Compose
  already started rather than build/run instructions. **Cleanup is now fully written** (`docker
  compose down`) since it was trivial and directly tied to this change — Exercises/Questions
  are still deferred to Sub-Task 6.

---

### Sub-Task 6 — Lab Instruction File (07-liberty-microservices.md)

**Status:** `[ ] pending`

**Intent:**
Write the complete lab instruction file following the established lab shape:
Objective → Concepts → Steps → Exercises → Questions → Cleanup.
This is the document a reader follows to reproduce the entire exercise from scratch.

**Expected Outcomes:**
- `exercise7/07-liberty-microservices.md` is complete and consistent with the code
- Every shell command, file path, and URL referenced in the steps is correct and tested
- A reader with no prior knowledge of this exercise can follow the steps sequentially
- The file follows the exact same structural conventions as exercises 1–6

**Sections to Write:**

1. **Objective** — what the reader will build and learn
2. **Concepts** — microservice boundaries, servlet-based REST, JNDI DataSources, JSP → API → DB flow
3. **Step 1: Start the database** — `docker run postgres`, verify with `psql`
4. **Step 2: Build and deploy products-api** — `mvn package`, Liberty server create, copy JDBC driver, `server run`
5. **Step 3: Build and deploy products-ui** — `mvn package`, Liberty server create, `server run`
6. **Step 4: Run the full stack** — open browser, do a full CRUD round-trip
7. **Exercises** — suggested modifications (e.g. add a `category` column, add input validation)
8. **Questions** — reflection questions (e.g. "Why does the UI not connect directly to the DB?")
9. **Cleanup** — `server stop`, `docker stop products-db`

**Todo List:**
1. Write the full Markdown file top-to-bottom following the shape
2. Cross-reference all shell snippets against actual file contents to ensure accuracy
3. Add a "What's next" section pointing to Phase 2 (containerization — Docker images for all 3 services)

**Relevant Context:**
- Shape reference: any existing exercise Markdown (e.g. [`exercise2/02-first-java-application.md`](../exercise2/02-first-java-application.md))
- All URLs, ports, context roots, and paths must match what was built in Sub-Tasks 1–5

---

## Implementation Order

```
Sub-Task 1 (skeleton)
    → Sub-Task 2 (DB schema)
        → Sub-Task 3 (API Java code)
            → Sub-Task 4 (API server.xml wiring)
                → Sub-Task 5 (UI code)
                    → Sub-Task 6 (Lab doc)
```

Each sub-task can be handed to the agent independently. The plan file is the single source
of truth — update each sub-task's `Status` to `[x] done` after completion and add any
implementation notes (e.g. exact Liberty server directory paths used) as **Implementation Notes**
below each sub-task before starting the next one.
