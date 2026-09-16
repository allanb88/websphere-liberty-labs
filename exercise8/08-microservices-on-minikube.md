# Exercise 8 — Products Microservices on Minikube

## Objective

Take the three-service products CRUD stack built in exercise 7 (`products-db`,
`products-api`, `products-ui`) and run it on a local Kubernetes cluster with
Minikube — using the exact same Docker images and the exact same application code.
Nothing in `products-api` or `products-ui` changes. What changes is how the three
containers find each other and how their lifecycle is managed, which is the point
of this exercise: seeing Docker Compose concepts map onto their Kubernetes
equivalents, one at a time.

This is Phase 2 of the migration learning path started in exercise 7. Phase 3 is
taking this same set of manifests and adapting them for Amazon EKS.

---

## Concepts

### Compose concept → Kubernetes equivalent

| Docker Compose (exercise 7) | Kubernetes (this exercise) |
|---|---|
| A `services:` entry | A `Deployment` (Pod template + replica count) + a `Service` (stable network identity) |
| Container name resolved by Docker's embedded DNS | Service name resolved by Kubernetes' CoreDNS |
| `networks: default: name: products-net` | Implicit — every Service in the same namespace is reachable by name, no network object to declare |
| `healthcheck: pg_isready` | The same `pg_isready` command, now wired into a `readinessProbe`/`livenessProbe` |
| `depends_on: products-db: condition: service_healthy` | `readinessProbe` on `products-api` — see below, this is not a 1:1 mapping |
| `docker compose up --build -d` | `docker build` + `minikube image load` + `kubectl apply -f` |
| `docker compose down` | `kubectl delete -f` |
| `-p 9080:9080` (host port mapping) | `kubectl port-forward` for local dev; a `LoadBalancer` Service or `Ingress` for real access |

### The hostnames didn't change

`products-api/server.xml` still points its `dataSource` at `products-db`.
`products-ui`'s `ApiClient` still calls `http://products-api:9081/...`. Neither file
was touched. The Kubernetes `Service` objects in `kubernetes/*-service.yaml` are
named `products-db` and `products-api` on purpose, so the same hostnames that
worked as Docker container names on `products-net` now resolve the same way as
Kubernetes Service names. This is the concrete version of a claim from the
migration guide: a Docker Compose network and a Kubernetes Service solve the exact
same "find my dependency by name" problem.

### Why there's no initContainer waiting for the database

In Compose, `depends_on: condition: service_healthy` physically delayed starting
the `products-api` container until Postgres was healthy. Kubernetes has no direct
equivalent — nothing blocks a Pod from starting because another Pod isn't ready.

It turns out we don't need one here. Liberty's JDBC connection pool is lazy — the
server starts and its HTTP listener comes up on port 9081 whether or not
`products-db` is reachable yet. `products-api-deployment.yaml`'s `readinessProbe`
calls the real `GET /api/products` endpoint, which does touch the database. Until
that call succeeds, the Pod is marked **not Ready**, and the `products-api` Service
simply won't send it traffic. The net effect — nothing gets a request until the
whole chain works — is the same guarantee Compose's healthcheck gave us, achieved
through the Kubernetes-native readiness mechanism instead.

### Why the liveness probe doesn't check the database

`products-api-deployment.yaml`'s `livenessProbe` only checks that TCP port 9081 is
open — it deliberately does **not** call `/api/products`. A liveness probe that
depends on `products-db` would mean: if the database ever goes down, Kubernetes
kills and restarts every `products-api` Pod, repeatedly, forever — restarting a
perfectly healthy API process fixes nothing about a database outage. That's a
common real-world Kubernetes misconfiguration. Readiness and liveness answer
different questions: readiness asks "should this Pod receive traffic right now,"
liveness asks "is this process itself stuck and needs a restart."

### products-ui's readiness probe has a blind spot

`products-ui-deployment.yaml`'s `readinessProbe` calls `GET /ui/products`, which
always returns `200`, even when `products-api` is completely unreachable —
`ProductsController.doGet()` catches that failure and renders an error message
instead of propagating it as an HTTP error. So this probe proves `products-ui`
itself is serving HTTP; it does not prove the backend chain is healthy. That's a
consequence of the UI code intentionally degrading gracefully, not a probe
misconfiguration — worth knowing, not worth "fixing" by making the UI fail harder.

### products-ui is NodePort; products-api and products-db stay ClusterIP

`products-ui-service.yaml` uses `type: NodePort` with a pinned `nodePort: 30080`,
because it's the one service meant to be reached from outside the cluster.
`products-api` and `products-db` remain `ClusterIP` — nothing outside the cluster
should call them directly; `products-ui` is the only front door.

A `NodePort` opens that port on every cluster node's IP, in addition to still
getting a normal `ClusterIP`. On Linux, that means `http://$(minikube ip):30080`
works immediately, no extra tooling.

### Why `http://$(minikube ip):30080` does not work on macOS

On macOS with the `docker` driver, `minikube ip` returns the IP of the Minikube
node's *container*, which lives inside Docker Desktop's internal VM network —
not bridged to the Mac's network stack. This was confirmed directly: even the
Kubernetes API server itself, already listening on that IP, is unreachable from
the host —

~~~bash
curl -sk https://$(minikube ip):8443
# curl: (7) Failed to connect ... 000
~~~

A `NodePort` on that same IP has the identical problem — changing the Service
type doesn't change the network path. This is a driver/OS limitation, not
something fixable in the manifest. On Linux (or with drivers that do bridge the
node network, like `hyperkit` used to), the exact same YAML would just work with
a direct IP. Use `minikube service` instead, see Step 6.

### products-db is a Deployment, not a StatefulSet

`products-db-deployment.yaml` runs Postgres as an ordinary Deployment with no
PersistentVolumeClaim. If that Pod is deleted or rescheduled, its data is gone —
Kubernetes will start a fresh, empty database and reseed it from
`products-db-init` (the ConfigMap holding `init.sql`). That's an acceptable
simplification for a learning exercise; it is not how you would run a real
database on Kubernetes. See Exercises below.

---

## Prerequisites

- Minikube and `kubectl` installed and a cluster running — if you haven't done
  this yet, follow exercise 6 ("Part 1: Install Kubernetes tools" and "Part 2:
  Start and verify Minikube") first. This exercise assumes `minikube start
  --driver=docker` has already been run.
- Exercise 7 completed, or at least present — this exercise reuses
  `exercise7/products-api/Dockerfile` and `exercise7/products-ui/Dockerfile`
  directly, unchanged.
- `curl` (and optionally `jq`) for verification

---

## Steps

### Step 1: Build the images

From the repository root. These use the same Dockerfiles as exercise 7's
`docker-compose.yml` — this just builds them with fixed tags independent of
whether you've run Compose in this session:

~~~bash
docker build -t products-api:1.0 exercise7/products-api
docker build -t products-ui:1.0 exercise7/products-ui
~~~

### Step 2: Load the images into Minikube

Minikube runs its own Docker environment, separate from your host's. A locally
built image is invisible to it until you load it explicitly — same step exercise 6
used for `hello-liberty`:

~~~bash
minikube image load products-api:1.0
minikube image load products-ui:1.0
~~~

Verify:

~~~bash
minikube image ls | grep -E "products-(api|ui)"
~~~

`products-db` needs no such step — it uses the public `postgres:16` image, which
Minikube pulls directly.

### Step 3: Apply the manifests

~~~bash
kubectl apply -f exercise8/kubernetes/
~~~

This creates, in one shot: the `products-db-init` ConfigMap, all three
Deployments, and all three Services.

### Step 4: Watch the rollout

~~~bash
kubectl rollout status deployment/products-db
kubectl rollout status deployment/products-api
kubectl rollout status deployment/products-ui
~~~

`products-api`'s rollout will report fewer than 2 replicas available for a few
seconds while its readiness probe waits on `products-db` — that pause is the
mechanism described in Concepts above, not a problem.

~~~bash
kubectl get pods -o wide
kubectl get deployments
kubectl get services
~~~

All three Deployments should show every replica `READY`; all three Services
should show a `CLUSTER-IP`.

### Step 5: Verify products-api directly

~~~bash
kubectl port-forward service/products-api 9081:9081
~~~

In another terminal:

~~~bash
curl -s http://localhost:9081/api/products | jq
~~~

Expected: the 5 seeded products — proof the full chain (Liberty → JNDI → JDBC →
the `products-db` Service) works inside the cluster.

### Step 6: Run the full stack through products-ui

`products-ui` is a `NodePort` Service (port `30080`), so there are two ways to
reach it. Which one works depends on your OS and Minikube driver.

**Option A — `kubectl port-forward`** (works everywhere, including macOS):

~~~bash
kubectl port-forward service/products-ui 9080:9080
~~~

Open `http://localhost:9080/ui/products`.

**Option B — `minikube service`** (also works everywhere, but on macOS/Windows
with the `docker` driver it still opens a local tunnel and keeps your terminal
occupied — it's a different command, not a way to avoid needing one open):

~~~bash
minikube service products-ui --url
~~~

On Linux with a bridged driver this prints `http://$(minikube ip):30080`
directly, reachable from any terminal without needing this command left
running. On macOS with the `docker` driver, it instead prints a message like:

~~~text
http://127.0.0.1:54321
! Because you are using a Docker driver on darwin, the terminal needs to be open to run it.
~~~

— a random local port tunneled to the NodePort, valid only while that command
keeps running. Confirmed working end to end in this environment (macOS,
`docker` driver): `curl http://127.0.0.1:<that port>/ui/products` returned
`200`, while `curl http://$(minikube ip):30080/ui/products` returned nothing
(connection unreachable) — matching the networking limitation described in
Concepts above.

**Either way**, once you have a working URL: create, edit, and delete a product
exactly as in exercise 7. Cross-check with `curl
http://localhost:9081/api/products` (Step 5's port-forward, if still running) —
every change made through the UI should show up there too.

---

## Exercises

1. Scale `products-api` to 3 replicas with `kubectl scale deployment/products-api
   --replicas=3` and watch the new Pod with `kubectl get pods -w` — notice it
   only turns `Ready` after its readiness probe starts succeeding, not the moment
   it starts `Running`.
2. Delete one `products-api` Pod by name (`kubectl delete pod <name>`) and watch
   Kubernetes replace it automatically. Compare this to what `docker restart`
   would do in exercise 7 — what's different about *who* decides to recreate it?
3. Convert `products-db-deployment.yaml` into a `StatefulSet` backed by a
   `PersistentVolumeClaim`, so product data survives a Pod restart. What else has
   to change (Service type, volume mount) for this to work?
4. Add `resources.requests` / `resources.limits` to all three Deployments. Pick
   values based on what you'd actually expect a small Liberty app to use, then
   verify with `kubectl top pod` (requires the metrics-server addon:
   `minikube addons enable metrics-server`).
5. Replace `port-forward` with a real path in: add an `Ingress` for `products-ui`
   and enable the `ingress` addon (`minikube addons enable ingress`).
6. Externalize `products-api/server.xml` as a ConfigMap instead of baking it into
   the image, and mount it over `/config/server.xml` — this is the exact
   technique the WebSphere-to-EKS migration guide describes for making
   `server.xml` versionable independently of the image.

---

## Questions

1. Why didn't `ApiClient.java` or `server.xml` need to change when moving from
   Docker Compose to Kubernetes?
2. Why does a `readinessProbe` replace `depends_on: condition: service_healthy`
   instead of an `initContainer`, given that Liberty's JDBC pool is lazy?
3. Why must `products-api`'s `livenessProbe` avoid calling an endpoint that
   touches `products-db`?
4. If `products-db`'s Pod is deleted right now, what happens to your data? What
   Kubernetes object is missing that would prevent that?
5. Compare this deployment against the "¿Esto es producción?" gap table from the
   WebSphere-to-EKS migration guide — which gaps does this exercise still have
   (TLS, secrets, resource limits, autoscaling, image versioning by digest...),
   and which ones did it already close relative to exercise 7?
6. Why does changing `products-ui`'s Service from `ClusterIP` to `NodePort` not,
   by itself, make it reachable at `http://$(minikube ip):30080` on macOS? What
   layer is actually blocking that request — Kubernetes, or something below it?

---

## Cleanup

~~~bash
kubectl delete -f exercise8/kubernetes/
~~~

This removes all three Deployments, all three Services, and the ConfigMap.
Minikube itself is shared across exercises — leave it running unless you're done
with the whole lab series:

~~~bash
minikube stop
~~~
