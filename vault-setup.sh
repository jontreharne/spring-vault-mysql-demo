if kubectl get pod | grep vault-0 | grep -q Running
then
    echo "Vault is installed continuing"
    break
else
    echo "Vault not installed in default namespace: cannot setup"
    exit 1
fi

# initialise the vault
if [ ! -f init-keys.json ]; then
    kubectl exec vault-0 -- vault operator init -key-shares=1 -key-threshold=1 \
      -format=json > init-keys.json
else
    echo "*******************************************************"
    echo "init-keys.json exist Vault could already be initialised"
    echo "Remove or rename the init-keys.json to reinitialise"
    echo "*******************************************************"
    exit 1
fi

echo " "
echo "*******************************************************"
echo "                 Unsealing the vault"
echo "*******************************************************"
VAULT_UNSEAL_KEY=$(cat init-keys.json | jq -r ".unseal_keys_b64[]")

kubectl exec vault-0 -- vault operator unseal $VAULT_UNSEAL_KEY

echo " "
echo "*******************************************************"
echo "                 Installing Vault Ingress"
echo "*******************************************************"
kubectl apply -f ingress_route.yaml

echo " "
echo "*******************************************************"
echo "                     Logging in"
echo "*******************************************************"
VAULT_ROOT_TOKEN=$(cat init-keys.json | jq -r ".root_token")

kubectl exec vault-0 -- vault login $VAULT_ROOT_TOKEN

echo " "
echo "*******************************************************"
echo "                 Enabling Kube Auth"
echo "*******************************************************"
kubectl exec vault-0 -ti -- /bin/sh -c 'vault auth enable kubernetes'

echo " "
echo "*******************************************************"
echo "          Granting Kube access to Vault"
echo "*******************************************************"
kubectl exec vault-0 -ti -- /bin/sh -c 'vault write auth/kubernetes/config \
    token_reviewer_jwt="$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)" \
    kubernetes_host="https://kubernetes.default.svc" \
    kubernetes_ca_cert=@/var/run/secrets/kubernetes.io/serviceaccount/ca.crt'

echo " "
echo "*******************************************************"
echo "                 Enabling KV secrets and database Engine"
echo "*******************************************************"
kubectl exec vault-0 -ti -- /bin/sh -c 'vault secrets enable -path=kv kv'

echo " "
echo "*******************************************************"
echo "                 Creating policy"
echo "*******************************************************"
kubectl exec vault-0 -ti -- /bin/sh -c 'vault policy write vault-secret-policy - <<EOF
path "kv/*" {  capabilities = ["read", "list"]}
path "database/*" {  capabilities = ["read", "list"]}
path "auth/token/lookup-self" {  capabilities = ["read"]}
path "auth/token/create" {capabilities = ["create", "read", "update", "list"]}
path "sys/renew/database/creds/*" {  capabilities = ["read", "update", "list"]}
path "sys/leases/*" {	capabilities = ["create", "read", "update", "delete", "list"]}
path "auth/token/renew-self" {capabilities = ["read", "update", "list"]}
EOF'

echo " "
echo "*******************************************************"
echo "                 Adding role"
echo "*******************************************************"
kubectl exec vault-0 -ti -- /bin/sh -c 'vault write auth/kubernetes/role/demo \
    bound_service_account_names=default \
    bound_service_account_namespaces=default \
    policies=vault-secret-policy \
    ttl=1h'

echo " "
echo "*******************************************************"
echo "                 Inserting secrets"
echo "*******************************************************"
kubectl exec vault-0 -ti -- /bin/sh -c 'vault kv put kv/application password=password username=root secret=aSuperSecretPassword'

echo " "
echo "*******************************************************"
echo "                 Enabling Vault database Engine"
echo "*******************************************************"
kubectl exec vault-0 -ti -- /bin/sh -c 'vault secrets enable database'

kubectl exec vault-0 -ti -- /bin/sh -c 'vault write database/config/my-mysql-database \
    plugin_name=mysql-database-plugin \
    connection_url="{{username}}:{{password}}@tcp(mysql.default.svc.cluster.local:3306)/" \
    allowed_roles="my-role" \
    username="root" \
    password="password"'

SQL="CREATE USER '{{name}}'@'%' IDENTIFIED BY '{{password}}';GRANT ALL PRIVILEGES ON *.* TO '{{name}}'@'%'; "
kubectl exec vault-0 -ti -- vault write database/roles/my-role db_name=my-mysql-database creation_statements="${SQL}" default_ttl=1m max_ttl=3m
