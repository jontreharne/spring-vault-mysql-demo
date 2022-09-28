# Vault, MySQL & Spring Boot

```
.
├── deploy                  # Helm Chart with values for step 1,2,3
├── mysql-demo              # Spring boot application
├── HELP.md                 # Spring Boot help file
├── README.md               # This File
├── ingress_route.yaml      # Ingress for remote access to Vault
├── init-keys.json          # Vault Key file generated by vault-setup.sh
├── token.sh                # Script to print Token for root vault user
├── unseal.sh               # Script to unseal vault 
├── vault-mysql.yaml        # Helmsman File for vault/mysql install
└── vault-setup.sh          # Script to setup Vault
```

## Prerequisites
- rancher-desktop
- helm
- kubectl
- [Helmsman](https://github.com/Praqma/helmsman#install)
- maven

> **NOTE: If you already have vault installed you will need to reset your kube cluster in order to fully remove Vault**

## Install Vault and MySQL
```shell
helmsman --apply --always-upgrade -f vault-mysql.yaml
```
## Initialise Vault
```shell
./vault-setup.sh
```
## Accessing Vault
1. Get the root user token
```shell
./token.sh
```
2. Port forward to Vault Pod
```shell
kubectl port-forward vault-0 8200
```
3. go to http://localhost:8200
login with the token method and the token from step 1.

## Build
port forward onto the mysql running in your cluster so the local build works and the test passes.
```bash
kubectl port-forward mysql-0 3306
```
compile, test and build the image
```bash
cd mysql-demo
mvn clean package
```
## Deploy Step 1
No Vault integration.
```bash
helm install mysql-demo deploy -f deploy/values-step1.yaml
```
## Uninstalling the Deployed Application
```shell
helm uninstall mysql-demo
```
## Deploy Step 2
Static username and password held in vault
```bash
helm install mysql-demo deploy -f deploy/values-step2.yaml
```
## Deploy Step 3
Rotating username and password held in vault
```bash
helm install mysql-demo deploy -f deploy/values-step3.yaml
```

## Application Usage
Port forward to the app in the cluster
```bash
kubectl port-forward mysql-demo-<add pod name> 8080
```

exercise the endpoints
```bash
# to add a greeting
curl -X POST "http://localhost:8080/demo/add?greeting=bonjour"

# to get all greetings
curl -X GET "http://localhost:8080/demo/all"
```