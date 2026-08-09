# Lab 06: From Docker to Kubernetes and OpenShift

## Objective

Connect the Liberty deployment you built to Kubernetes concepts: images, deployments, services, routes, and Operators.

This lab is intentionally a follow-on exercise. Make sure the application works in Docker first.

## Concepts

- Container image
- Kubernetes Deployment
- Kubernetes Service
- OpenShift Route
- Kubernetes manifest
- Operator and Custom Resource

## Step 1: Create a Kubernetes Deployment

Create deployment.yaml:

~~~yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: hello-liberty
spec:
  replicas: 2
  selector:
    matchLabels:
      app: hello-liberty
  template:
    metadata:
      labels:
        app: hello-liberty
    spec:
      containers:
        - name: hello-liberty
          image: hello-liberty:1.0.0
          ports:
            - containerPort: 9080
~~~

Apply it:

~~~bash
kubectl apply -f deployment.yaml
kubectl get pods
kubectl describe deployment hello-liberty
~~~

## Step 2: Create a Service

Create service.yaml:

~~~yaml
apiVersion: v1
kind: Service
metadata:
  name: hello-liberty
spec:
  selector:
    app: hello-liberty
  ports:
    - port: 9080
      targetPort: 9080
~~~

Apply it:

~~~bash
kubectl apply -f service.yaml
kubectl get service hello-liberty
~~~

For a local cluster, test it with port forwarding:

~~~bash
kubectl port-forward service/hello-liberty 9080:9080
~~~

Then visit:

~~~text
http://localhost:9080/hello/hello
~~~

## Step 3: Explore OpenShift

On OpenShift, expose the Service with a Route:

~~~bash
oc expose service hello-liberty
oc get route hello-liberty
~~~

Open the hostname returned by oc get route and append:

~~~text
/hello/hello
~~~

## Step 4: Operator discussion

A Deployment knows how to keep pods running. A Liberty or application Operator can provide higher-level knowledge about configuring and operating Liberty applications.

Research and answer:

1. What Custom Resources does the Operator provide?
2. What configuration does the Operator create for you?
3. Which tasks are handled by Kubernetes itself?
4. Which tasks require application-specific knowledge?

## Exercises

1. Change replicas from 2 to 3.
2. Update the image tag and observe the rollout.
3. Delete one pod and watch Kubernetes recreate it.
4. Scale the Deployment down to zero and back to two.
5. Add a readiness or liveness probe.

## Cleanup

~~~bash
kubectl delete -f service.yaml
kubectl delete -f deployment.yaml
~~~

