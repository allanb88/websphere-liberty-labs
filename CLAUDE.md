# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

A self-contained set of hands-on lab instructions (Markdown) for learning WebSphere Liberty: running it in Docker, building and deploying a Java servlet WAR, configuring `server.xml`, packaging a reusable application image, troubleshooting, and deploying to Kubernetes (Minikube) and OpenShift (OpenShift Local/CRC). There is no application build system at the repo root — each exercise's Markdown file is the source of truth for the commands and files a reader creates as they work through the lab, and `exercise2/example/hello-app/` and `exercise6/example/{kubernetes,openshift}/` hold worked-example copies of what the labs produce.

Labs are meant to be followed in order (01 → 08); later labs assume artifacts (the `hello-liberty` image, `server.xml`, manifests, and from exercise 7 onward the `products-*` services) built in earlier ones.

## Repository layout

- `README.md` — prerequisites and the lab index/table.
- `exercise1/` … `exercise8/` — one lab each, numbered `NN-topic.md`. Some contain an `example/` subfolder with the finished files for that lab.
- `exercise2/example/hello-app/` — the sample Maven WAR project (`pom.xml`, `dockerfile`, `server.xml`, `src/main/java/.../HelloServlet.java`) referenced throughout labs 2–5.
- `exercise6/example/kubernetes/` and `exercise6/example/openshift/` — `deployment.yaml`/`service.yaml` manifests referenced by lab 6 (the OpenShift deployment additionally points at a pushed Docker Hub image).
- `exercise7/` — a 3-tier products CRUD app (`products-db` Postgres + `products-api` Liberty REST servlet + `products-ui` Liberty JSP frontend) built and run entirely with `docker-compose.yml`; each service has its own Maven project and multi-stage `Dockerfile` (no local JDK/Maven/Liberty install required). The UI never talks to the database directly — it calls `products-api` over HTTP, which is the only service holding the JNDI/JDBC connection; the three containers address each other by Compose service name (`products-db:5432`, `products-api:9081`), never `localhost`. `liberty-microservices-plan.md` tracks this exercise's sub-task-by-sub-task build plan and implementation notes — read it before making further changes here.
- `exercise8/` — deploys the products stack to Minikube, translating each Compose concept to its Kubernetes equivalent (see the table in `README.md`), **and is the canonical source for the images published to Docker Hub** (`allanbs88/products-api`, `allanbs88/products-ui`, `allanbs88/products-db`) and for `.github/workflows/ci-cd.yml`. `exercise8/products-api/`, `exercise8/products-ui/`, and `exercise8/db/` (a `postgres:16` base plus `init.sql` baked in via `docker-entrypoint-initdb.d/`) hold their own copies of the app code/Dockerfiles, independent of `exercise7/`'s copies — the two exercises are each self-contained (exercise 7 for the local Compose workflow, exercise 8 for Minikube + CI/CD) and are **not** kept in sync automatically; a change to shared logic (e.g. `JsonUtil`, `ProductsController`) needs to be applied to both `exercise7/products-*` and `exercise8/products-*` by hand. `kubernetes/` holds a ConfigMap (Postgres init SQL, inlined from `exercise8/db/init.sql`) plus a Deployment+Service pair per service — the manifests still use the ConfigMap approach rather than the `products-db` image built from `exercise8/db/`, so `minikube image load` + `kubectl apply` and "pull `allanbs88/products-db` from Docker Hub" are two independent, not-yet-unified paths to a seeded database. `products-api`'s readiness probe calls the real `/api/products` endpoint (not a TCP/port check) so the Pod gets no traffic until the full Liberty→JNDI→JDBC→Postgres chain works; its liveness probe deliberately checks only that the port is open, so a database outage doesn't cause Kubernetes to kill and restart otherwise-healthy API pods.
- `FAQ/` — standalone reference docs (context roots/URL composition, the Liberty welcome page, inspecting/decompiling JAR/WAR files). Numbered but not sequential like the exercises.

## Working with this content

There is nothing to build, lint, or test at the repository level — this is documentation. When asked to change a lab or example:

- Keep the example files under `exercise*/example/` in sync with the corresponding steps described in that exercise's Markdown file (e.g. `exercise2/example/hello-app/*` must match the code blocks in `exercise2/02-first-java-application.md`; the Kubernetes/OpenShift manifests must match `exercise6/06-kubernetes-and-openshift.md`).
- Each lab file follows the same shape: Objective → Concepts → numbered Steps (with runnable shell/XML/Java snippets) → Exercises → Questions → Cleanup. Preserve this structure when editing or adding labs.
- Commands in the labs are written for macOS (e.g. Colima architecture handling in `exercise6`); note this if adapting instructions for another OS.

## The example application (`exercise2/example/hello-app/`)

A minimal Jakarta Servlet 6.0 app, built and containerized as:

```bash
mvn clean package                        # produces target/hello.war
docker build -t hello-liberty:1.0 .      # bakes server.xml + hello.war into a Liberty image
docker run -d --name hello-liberty -p 9080:9080 hello-liberty:1.0
curl -i http://localhost:9080/hello/hello
```

- Context root and servlet path compose the URL: `/hello` (from the WAR filename via dropins, or an explicit `contextRoot` in `server.xml`) + `/hello` (the `@WebServlet` mapping) → `/hello/hello`. See `FAQ/01-context-roots-and-urls.md` for the full breakdown.
- Two deployment styles are used across labs: dropins (`COPY target/hello.war /config/dropins/`, context root inferred from filename) in early exercises, and explicit `apps/` deployment with a `<webApplication>` element in `server.xml` (`COPY target/hello.war /config/apps/`) from exercise 3 onward.
- `maven.compiler.release` is 11; the servlet API dependency is `provided` scope since Liberty supplies it at runtime.
