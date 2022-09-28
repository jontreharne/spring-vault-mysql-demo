#!/bin/bash

cat init-keys.json | jq -r ".root_token"
