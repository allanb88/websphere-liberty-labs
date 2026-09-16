#!/usr/bin/env bash
# Applies exercise8/kubernetes/ to your local Minikube and forces a fresh pull
# of allanbs88/products-api:latest and allanbs88/products-ui:latest from Docker
# Hub — the images .github/workflows/ci-cd.yml pushes on every merge to main.
# Run this any time you want Minikube to catch up with what CI last published;
# it does not build or push anything itself, it only pulls and deploys.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

if ! minikube status >/dev/null 2>&1; then
  echo "==> Minikube is not running, starting it..."
  minikube start
fi

echo "==> Applying manifests"
kubectl apply -f kubernetes/

echo "==> Forcing a fresh pull of :latest for products-api and products-ui"
kubectl rollout restart deployment/products-api deployment/products-ui

echo "==> Waiting for rollout"
kubectl rollout status deployment/products-db
kubectl rollout status deployment/products-api
kubectl rollout status deployment/products-ui

echo
echo "==> Done. Reach the UI with:"
echo "    kubectl port-forward service/products-ui 9080:9080"
echo "    then open http://localhost:9080/ui/products"
