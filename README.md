# WebSphere Liberty Labs

Hands-on labs for learning WebSphere Liberty: running it in Docker, building and deploying a
Java servlet WAR, configuring `server.xml`, packaging a reusable application image, diagnosing
common failures, and deploying to Kubernetes (Minikube) and OpenShift (OpenShift Local/CRC).

The labs are deliberately progressive — each one builds on artifacts produced by the previous
one — and they end with a small but realistic migration path: a 3-tier microservices app built
and run with Docker Compose (exercise 7), then redeployed unchanged onto a local Kubernetes
cluster (exercise 8).

## Prerequisites

- Docker Desktop or Docker Engine (with the Compose v2 plugin)
- Java 11 or newer
- Apache Maven
- curl or a web browser
- Git
- Minikube and `kubectl` (for exercises 6 and 8) / OpenShift Local (for exercise 6)

Verify the tools:

~~~bash
docker --version
java -version
mvn --version
~~~

## Lab sequence

| Lab | Topic | Main outcome |
| --- | --- | --- |
| 01 | Liberty and containers | Start, inspect, stop, and restart Liberty |
| 02 | First Java application | Build and deploy a Java WAR application |
| 03 | Liberty configuration | Understand server.xml, features, ports, and context roots |
| 04 | Application image | Build a reusable Liberty application image |
| 05 | Troubleshooting | Diagnose common deployment and runtime failures |
| 06 | Kubernetes and OpenShift | Translate the application into container orchestration concepts |
| 07 | Liberty microservices | Build a 3-tier products CRUD app (Postgres + REST API + JSP UI) running entirely in Docker |
| 08 | Microservices on Minikube | Deploy the exercise 7 stack to Kubernetes, mapping Compose concepts onto Deployments, Services, and probes |

Each lab (`exerciseN/NN-topic.md`) follows the same structure: Objective → Concepts →
numbered Steps → Exercises → Questions → Cleanup. Labs 2 and 6–8 also ship an `example/`
(or top-level) folder with the finished, runnable code the lab describes.

## Repository layout

```
.
├── exercise1/               Liberty and containers
├── exercise2/                First Java application (+ example/hello-app)
├── exercise3/                Liberty configuration
├── exercise4/                Application image
├── exercise5/                Troubleshooting
├── exercise6/                Kubernetes and OpenShift (+ example/kubernetes, example/openshift)
├── exercise7/                Liberty microservices — Products CRUD (Docker Compose)
├── exercise8/                Products microservices on Minikube (Kubernetes)
└── FAQ/                      Standalone reference docs (context roots, welcome page, WAR/JAR inspection)
```

---

## Exercise 7 — Liberty Microservices: Products CRUD

A 3-tier CRUD application, split into three independently built and run services,
orchestrated with a single `docker-compose.yml`:

| Service | Technology | Port | Responsibility |
| --- | --- | --- | --- |
| `products-db` | PostgreSQL 16 | 5432 | Owns the `products` table, seeded via `db/init.sql` |
| `products-api` | Liberty + Jakarta Servlet | 9081 | REST API (`/api/products`) — all CRUD over JDBC |
| `products-ui` | Liberty + JSP | 9080 | Browser UI (`/ui/products`) — calls the API, never touches the database |

**Why it matters:** this is the same "monolith → services" boundary a real WebSphere-to-Kubernetes
migration draws — the UI never reaches the database directly, everything flows through the API,
and each service is its own Maven project with its own multi-stage `Dockerfile` (no local JDK,
Maven, or Liberty install required to build or run any of it).

Key mechanics covered:

- **JNDI DataSource** — `products-api/server.xml` provisions the JDBC pool; application code
  only ever asks Liberty for `java:comp/env/jdbc/products`, never a hard-coded JDBC URL.
- **Container-to-container networking** — all three services join one Docker network
  (`products-net`); `products-api` reaches Postgres at `products-db:5432` and `products-ui`
  reaches the API at `http://products-api:9081/...` — never `localhost`.
- **Startup ordering** — `products-db` has a `pg_isready` healthcheck; `products-api` uses
  `depends_on: condition: service_healthy` instead of a fixed sleep.
- **Post-Redirect-Get** — the UI servlet turns every form POST into a redirect back to
  `GET /ui/products`, so a page refresh never resubmits a create/update/delete.

Run it:

~~~bash
cd exercise7
docker compose up --build -d
docker compose ps                                   # products-db should show (healthy)
curl -s http://localhost:9081/api/products | jq      # verify the API
open http://localhost:9080/ui/products                # verify the UI (or use a browser)
docker compose down                                   # cleanup
~~~

Full walkthrough, verification steps, and troubleshooting: [exercise7/07-liberty-microservices.md](exercise7/07-liberty-microservices.md).

---

## Exercise 8 — Products Microservices on Minikube

Takes the exact same images and application code from exercise 7 and runs them on a local
Kubernetes cluster (Minikube) — nothing in `products-api` or `products-ui` changes. What
changes is *how the three containers find each other and how their lifecycle is managed*,
which is the point of the exercise: seeing each Docker Compose concept map onto its
Kubernetes equivalent.

| Docker Compose (exercise 7) | Kubernetes (exercise 8) |
| --- | --- |
| A `services:` entry | A `Deployment` (Pod template + replicas) + a `Service` (stable DNS name) |
| Docker embedded DNS | Kubernetes CoreDNS — Service names resolve the same way container names did |
| `networks: products-net` | Implicit — every Service in the namespace is reachable by name |
| `healthcheck: pg_isready` | The same command, wired into a `readinessProbe` / `livenessProbe` |
| `depends_on: condition: service_healthy` | `readinessProbe` on `products-api` (not a 1:1 mapping — see below) |
| `docker compose up --build -d` | `docker build` + `minikube image load` + `kubectl apply -f` |
| `-p 9080:9080` | `kubectl port-forward`, or a `NodePort` Service |

Manifests live in [exercise8/kubernetes/](exercise8/kubernetes/): a ConfigMap for the Postgres
init SQL, and a Deployment + Service pair for each of the three services.

Notable design decisions (explained in full in the lab):

- **No `initContainer` waits for the database.** Liberty's JDBC pool is lazy, so the API's
  HTTP listener comes up regardless. `products-api`'s `readinessProbe` calls the real
  `GET /api/products` endpoint — the Pod isn't marked Ready, and gets no traffic, until the
  whole chain (Liberty → JNDI → JDBC → `products-db`) actually works.
- **The liveness probe deliberately does not check the database.** It only checks that port
  9081 is open — a liveness probe tied to `products-db` would make Kubernetes kill and restart
  healthy API Pods forever during a database outage, fixing nothing.
- **`products-ui` is `NodePort`; `products-api` and `products-db` stay `ClusterIP`.**
  `products-ui` is the only front door meant to be reached from outside the cluster.
- **`products-db` is a plain Deployment, not a StatefulSet** — no `PersistentVolumeClaim`, so a
  Pod restart loses data and reseeds from the ConfigMap. An intentional simplification for a
  learning exercise (see the lab's Exercises section for how to fix it).

Run it (images must be built and loaded before applying manifests):

~~~bash
docker build -t products-api:1.0 exercise7/products-api
docker build -t products-ui:1.0 exercise7/products-ui
minikube image load products-api:1.0
minikube image load products-ui:1.0

kubectl apply -f exercise8/kubernetes/
kubectl rollout status deployment/products-api
kubectl get pods -o wide

kubectl port-forward service/products-ui 9080:9080    # then open http://localhost:9080/ui/products
kubectl delete -f exercise8/kubernetes/                # cleanup
~~~

Full walkthrough, macOS-specific networking notes (`minikube ip` vs. `NodePort` on the `docker`
driver), and follow-up exercises (scaling, `StatefulSet`, resource limits, `Ingress`):
[exercise8/08-microservices-on-minikube.md](exercise8/08-microservices-on-minikube.md).

---

## Recommended workflow

For every lab:

1. Read the objective before running commands.
2. Make one change at a time.
3. Check the container logs after each deployment.
4. Write down what changed and why.
5. Complete the questions before moving to the next lab.

## Official references

- [Setting up Liberty](https://www.ibm.com/docs/en/was-liberty/base?topic=setting-up-liberty)
- [Deploying applications in Liberty](https://www.ibm.com/docs/en/was-liberty/core?topic=deploying-applications-in-liberty)
- [Creating Liberty application images](https://www.ibm.com/docs/en/was-liberty/base?topic=container-creating-application-images)
- [Liberty container images](https://www.ibm.com/docs/en/was-liberty/core?topic=images-liberty-container)
