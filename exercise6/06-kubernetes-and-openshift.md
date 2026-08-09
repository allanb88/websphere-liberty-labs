# Lab 06: From Docker to Kubernetes and OpenShift

## Objective

Deploy the WebSphere Liberty application from the previous exercises to:

1. A local Kubernetes cluster running with Minikube.
2. A local OpenShift cluster running with Red Hat OpenShift Local.

You will install the command-line tools, create Kubernetes manifests, deploy the application, expose it through a Service, and create an OpenShift Route.

## What you will learn

- How a container image becomes a Kubernetes Deployment
- How a Service provides stable network access to Pods
- How Kubernetes manifests describe desired state
- How Minikube makes Kubernetes available on a laptop
- How OpenShift Local provides a local OpenShift environment
- How an OpenShift Route exposes a Service
- Why a local image must be loaded into Minikube or pushed to a registry for OpenShift
- How Operators fit into the platform

## Prerequisites

Complete the Docker and Liberty exercises first. You should have a working application image such as:

~~~text
hello-liberty:1.0
~~~

You also need:

- macOS, Linux, or Windows
- Docker Desktop or another supported container runtime
- At least 2 CPUs, 2 GB of free memory, 20 GB of free disk space, and an Internet connection for Minikube
- More resources for OpenShift Local; the exact requirement depends on the OpenShift Local release and preset
- A Red Hat account if you want to install OpenShift Local

This guide uses macOS commands because the rest of this repository was created on macOS. For another operating system, use the corresponding installer from the official documentation.

## Part 1: Install Kubernetes tools

### Step 1: Verify Docker

Start Docker Desktop, then run:

~~~bash
docker version
docker info
~~~

Docker must be running before you start Minikube.

### macOS and Colima: match the CPU architecture

On macOS, Linux is the operating system inside Colima, but the CPU architecture still matters. The Colima Docker engine, the Minikube node image, and the Liberty application image must use the same architecture.

Check both the Mac and Docker architectures:

~~~bash
uname -m
docker info --format '{{.Architecture}}'
~~~

#### Apple Silicon Macs

If uname -m returns arm64, use a native ARM64 Colima environment. Do not configure Colima as x86_64 for this Minikube lab:

~~~bash
colima stop
colima start \
  --arch aarch64 \
  --vm-type=vz \
  --runtime docker \
  --cpu 4 \
  --memory 6 \
  --disk 60
~~~

Verify that Docker reports aarch64 or arm64:

~~~bash
docker info --format '{{.Architecture}}'
~~~

Build the Liberty image as ARM64:

~~~bash
docker build --platform linux/arm64 -t hello-liberty:1.0 .
~~~

Do not build the application as linux/amd64 while using an ARM64 Colima environment. Although emulation can run some foreign-architecture containers, it can prevent Minikube's Docker driver from starting correctly.

If an existing Colima profile was created as x86_64, changing its architecture might require creating a new profile or recreating the existing profile. Existing containers and images belong to the original Colima environment.

#### Intel Macs

If uname -m returns x86_64, use an x86_64 Colima environment and build the application image as linux/amd64:

~~~bash
colima start \
  --arch x86_64 \
  --runtime docker \
  --cpu 4 \
  --memory 6 \
  --disk 60

docker build --platform linux/amd64 -t hello-liberty:1.0 .
~~~

The important rule is:

~~~text
Apple Silicon Mac → ARM64 Colima → linux/arm64 images
Intel Mac         → AMD64 Colima → linux/amd64 images
~~~

After changing the Colima architecture, verify Docker before starting Minikube:

~~~bash
docker info --format '{{.Architecture}}'
~~~

Minikube does not need an architecture flag in this setup. It uses the architecture of the Docker environment:

~~~bash
minikube start --driver=docker
~~~

If a previous Minikube profile was created with the wrong architecture, remove that local profile before starting again:

~~~bash
minikube delete --all
minikube start --driver=docker
~~~

### Step 2: Verify Homebrew

~~~bash
brew --version
~~~

If Homebrew is not installed, install it from [brew.sh](https://brew.sh/) and open a new terminal window.

### Step 3: Install kubectl and Minikube

~~~bash
brew install kubectl minikube
~~~

Kubernetes recommends using a kubectl client within one minor version of the cluster version. Minikube manages the Kubernetes version of its local cluster, so check both versions after the cluster starts.

Verify the installations:

~~~bash
kubectl version --client
minikube version
~~~

## Part 2: Start and verify Minikube

### Step 1: Start a local Kubernetes cluster

Use Docker as the Minikube driver:

~~~bash
minikube start --driver=docker
~~~

Minikube creates a local Kubernetes cluster and updates your kubeconfig so kubectl can use it.

### Step 2: Verify the cluster

~~~bash
minikube status
kubectl config current-context
kubectl cluster-info
kubectl get nodes
kubectl get pods --all-namespaces
~~~

The current context should be minikube and the node should eventually show Ready.

### Step 3: Make the Liberty image available to Minikube

Minikube runs Kubernetes inside its own environment. Load your locally built image into Minikube:

~~~bash
minikube image load hello-liberty:1.0
~~~

Verify that the image is available:

~~~bash
minikube image ls | grep hello-liberty
~~~

The Deployment manifest in this lab uses imagePullPolicy: IfNotPresent. That tells Kubernetes to use the image already loaded into Minikube instead of trying to pull it from a remote registry.

## Part 3: Deploy the Liberty application to Kubernetes

Create a directory for the manifests:

~~~bash
mkdir -p kubernetes
cd kubernetes
~~~

### Step 1: Create deployment.yaml

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
          image: hello-liberty:1.0
          imagePullPolicy: IfNotPresent
          ports:
            - name: http
              containerPort: 9080
~~~

### Step 2: Create service.yaml

~~~yaml
apiVersion: v1
kind: Service
metadata:
  name: hello-liberty
spec:
  selector:
    app: hello-liberty
  ports:
    - name: http
      port: 9080
      targetPort: 9080
~~~

### Step 3: Apply the manifests

~~~bash
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
~~~

### Step 4: Verify the Deployment and Pods

~~~bash
kubectl get deployments
kubectl get pods -o wide
kubectl rollout status deployment/hello-liberty
kubectl describe deployment hello-liberty
~~~

Both Pods should eventually show Running and Ready.

### Step 5: Verify the Service

~~~bash
kubectl get service hello-liberty
kubectl describe service hello-liberty
~~~

The Service provides a stable name and virtual IP even if individual Pods are replaced.

### Step 6: Test the application

The Service created in this exercise is a `ClusterIP` Service. It is accessible inside the Minikube cluster, but it is not automatically exposed on your Mac. Create a temporary local connection with:

~~~bash
kubectl port-forward service/hello-liberty 9080:9080
~~~

The first `9080` is the local port on your computer. The second `9080` is the Service port inside Kubernetes. Keep this command running in one terminal. In another terminal, test the application:

~~~bash
curl -i http://localhost:9080/hello/hello
~~~

Expected response:

~~~text
Hello from WebSphere Liberty!
~~~

## Part 4: Install OpenShift Local

Minikube provides a local Kubernetes cluster. OpenShift Local provides a local, single-node OpenShift cluster with OpenShift-specific features such as Routes, Projects, the OpenShift Console, and Operators.

Do not run Minikube and OpenShift Local at the same time on a resource-constrained laptop. Stop one environment before starting the other.

### Step 1: Check your Mac architecture

~~~bash
uname -m
~~~

Common results are:

- arm64: Apple silicon
- x86_64: Intel Mac

Download the OpenShift Local installer that matches your architecture.

### Step 2: Download OpenShift Local and the pull secret

Open the Red Hat OpenShift Local download page:

[Red Hat OpenShift Local](https://console.redhat.com/openshift/create/local)

Sign in with a Red Hat account, then download:

1. The OpenShift Local installer for your operating system and CPU architecture.
2. The OpenShift pull secret.

Save the pull secret somewhere private, such as:

~~~text
~/Downloads/pull-secret.txt
~~~

Do not commit the pull secret to GitHub. It contains credentials.

### Step 3: Install the macOS package

Open the downloaded macOS package from Finder and complete the installer. Then open a new terminal window.

Verify the installation:

~~~bash
crc version
~~~

The output shows the Red Hat OpenShift Local version and the OpenShift version bundled with that release. Use this version as the OpenShift version for this lab rather than installing an unrelated cluster version.

### Step 4: Configure OpenShift Local

Use the OpenShift preset:

~~~bash
crc config set preset openshift
~~~

If OpenShift Local reports that more resources are required, configure a larger preset. For example:

~~~bash
crc config set cpus 4
crc config set memory 12288
~~~

The exact resource requirements can change between OpenShift Local releases. Check the release notes for the version you downloaded before increasing the limits.

### Step 5: Prepare the host

~~~bash
crc setup
~~~

This prepares the host and creates the local CRC configuration.

### Step 6: Start OpenShift Local

~~~bash
crc start --pull-secret-file ~/Downloads/pull-secret.txt
~~~

The first startup can take several minutes. The command displays login information, including the developer user password.

If you need to display the credentials later:

~~~bash
crc console --credentials
~~~

### Step 7: Open the OpenShift Console

~~~bash
crc console
~~~

This opens the local OpenShift web console in your browser.

### Step 8: Configure the OpenShift CLI

OpenShift Local includes a compatible oc client. Add it to the current shell:

~~~bash
eval "$(crc oc-env)"
~~~

Verify the tools and cluster:

~~~bash
oc version
oc status
oc whoami
~~~

If you are not logged in, log in as the developer user:

~~~bash
oc login -u developer https://api.crc.testing:6443
~~~

Use the developer password shown by crc start or crc console --credentials.

## Part 5: Deploy the application to OpenShift

### Important image note

Minikube can load a local Docker image with minikube image load. OpenShift Local runs its own cluster environment, so it cannot automatically use an image that exists only in your Docker Desktop image store.

For this exercise, push the image to a container registry that OpenShift Local can reach. Docker Hub is used below as an example.

### Step 1: Tag and push the image

Replace YOUR_DOCKERHUB_USER with your Docker Hub username:

~~~bash
docker login

docker tag hello-liberty:1.0 \
  docker.io/YOUR_DOCKERHUB_USER/hello-liberty:1.0

docker push docker.io/YOUR_DOCKERHUB_USER/hello-liberty:1.0
~~~

For a private image, create an OpenShift image pull secret. For this introductory lab, use a public image repository if possible.

### Step 2: Create an OpenShift project

~~~bash
oc new-project hello-liberty
~~~

### Step 3: Update the Deployment image

In deployment.yaml, replace:

~~~yaml
image: hello-liberty:1.0
~~~

with:

~~~yaml
image: docker.io/YOUR_DOCKERHUB_USER/hello-liberty:1.0
imagePullPolicy: IfNotPresent
~~~

Apply the updated Deployment:

~~~bash
oc apply -f deployment.yaml
oc apply -f service.yaml
~~~

### Step 4: Verify the OpenShift Deployment

~~~bash
oc get deployments
oc get pods
oc rollout status deployment/hello-liberty
oc get service hello-liberty
~~~

### Step 5: Create an OpenShift Route

~~~bash
oc expose service hello-liberty
oc get route hello-liberty
~~~

Copy the hostname from the HOST/PORT column and append:

~~~text
/hello/hello
~~~

For example:

~~~text
http://hello-liberty-hello-liberty.apps-crc.testing/hello/hello
~~~

Test it with:

~~~bash
ROUTE_HOST="$(oc get route hello-liberty -o jsonpath='{.spec.host}')"
curl -i "http://${ROUTE_HOST}/hello/hello"
~~~

The `$()` syntax runs the `oc get route` command and stores its result in `ROUTE_HOST`. Do not add a backslash before `$(`. The backslash would prevent zsh from interpreting the command substitution correctly.

## Part 6: Operator discussion

A Kubernetes Deployment keeps Pods running, but it does not understand every operational detail of Liberty or another complex application.

An Operator extends Kubernetes with application-specific knowledge. It normally provides Custom Resource Definitions and a controller that continuously reconciles those Custom Resources.

Research and answer:

1. What Custom Resources does a Liberty-related Operator provide?
2. What configuration does the Operator create for you?
3. Which tasks are handled by Kubernetes itself?
4. Which tasks require application-specific knowledge?
5. How is an Operator different from a Helm chart?

## Exercises

1. Change replicas from 2 to 3 and observe the new Pod.
2. Delete one Pod and watch Kubernetes or OpenShift recreate it.
3. Scale the Deployment down to zero and back to two.
4. Update the image tag and observe the rollout.
5. Add a readiness probe for /hello/hello.
6. Add a liveness probe for /hello/hello.
7. Inspect the Deployment, ReplicaSet, Pods, Service, and Route.
8. Compare kubectl get output in Minikube with oc get output in OpenShift.
9. Stop Minikube, start OpenShift Local, and deploy the same application image through the registry.
10. Research one Operator from OperatorHub and document the Custom Resource it manages.

## Useful commands

~~~bash
# Minikube
minikube status
minikube stop
minikube start --driver=docker
minikube delete

# Kubernetes
kubectl config current-context
kubectl get all
kubectl logs deployment/hello-liberty
kubectl describe pod

# OpenShift Local
crc status
crc stop
crc start
crc console
oc get all
oc logs deployment/hello-liberty
oc describe pod
~~~

## Cleanup

### Kubernetes and Minikube

~~~bash
kubectl delete -f service.yaml
kubectl delete -f deployment.yaml
minikube stop
~~~

To remove the Minikube cluster completely:

~~~bash
minikube delete
~~~

### OpenShift Local

~~~bash
oc delete project hello-liberty
crc stop
~~~

To remove the OpenShift Local instance completely:

~~~bash
crc delete
~~~

## Official references

- [Minikube start guide](https://minikube.sigs.k8s.io/docs/start/)
- [Minikube Docker driver](https://minikube.sigs.k8s.io/docs/drivers/docker/)
- [Install kubectl on macOS](https://kubernetes.io/docs/tasks/tools/install-kubectl-macos/)
- [Red Hat OpenShift Local](https://developers.redhat.com/products/openshift-local/overview)
- [OpenShift Local download and pull secret](https://console.redhat.com/openshift/create/local)
- [OpenShift Local getting started guide](https://docs.redhat.com/en/documentation/red_hat_openshift_local/2.35/html/getting_started_guide/)
