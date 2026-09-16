# Bakes the seed schema/data into the image itself instead of mounting it at
# runtime (the ConfigMap approach exercise8/kubernetes/products-db-configmap.yaml
# uses) or via a Compose bind-mount (exercise7/docker-compose.yml). Postgres's
# own entrypoint script still runs anything under docker-entrypoint-initdb.d/
# on first startup — this only changes how the file gets there.
FROM postgres:16
COPY init.sql /docker-entrypoint-initdb.d/init.sql
