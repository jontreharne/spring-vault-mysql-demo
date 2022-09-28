#!/bin/bash

echo "*******************************************************"
echo "                 Unsealing the vault"
echo "*******************************************************"
VAULT_UNSEAL_KEY=$(cat init-keys.json | jq -r ".unseal_keys_b64[]")

kubectl exec vault-0 -- vault operator unseal $VAULT_UNSEAL_KEY
