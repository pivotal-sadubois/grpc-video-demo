#!/bin/bash

export KUBECONFIG=$HOME/workspace/tdh-vsphere-tkgs-sdubois-svc-kubeconfig
export CERTPATH=$HOME/Documents/Certificate/STAR.apps.tkg.corelab.core-software.ch

NAMESPACE=grpc-video-server

kubectl delete ns $NAMESPACE > /dev/null 2>&1
kubectl create ns $NAMESPACE > /dev/null 2>&1
kubectl -n $NAMESPACE label --overwrite ns $NAMESPACE pod-security.kubernetes.io/enforce=privileged

##################################################################################################
##################################### CREATE TLS SECRETS #########################################
##################################################################################################
kubectl -n $NAMESPACE delete secret core-tls-secret > /dev/null 2>&1
  ex=$(openssl x509 -in $CERTPATH/k8s-apps-core.crt -noout -dates | tail -1 | sed 's/^.*=//g')
  echo " ▪ Create TLS/SSL Certificat Secret (core-tls-secret) Expiring: $ex"

  kubectl -n $NAMESPACE create secret tls core-tls-secret \
    --cert=$CERTPATH/k8s-apps-core.crt \
    --key=$CERTPATH/k8s-apps-core.key


kubectl -n $NAMESPACE delete secret tdh-tls-secret > /dev/null 2>&1
export CERTPATH=$HOME/Documents/Certificate/STAR.apps.tkg.fortidemo.ch
  ex=$(openssl x509 -in $CERTPATH/fullchain.pem -noout -dates | tail -1 | sed 's/^.*=//g')
  echo " ▪ Create TLS/SSL Certificat Secret (tdh-tls-secret) Expiring: $ex"

  kubectl -n $NAMESPACE create secret tls tdh-tls-secret \
    --cert=$CERTPATH/fullchain.pem \
    --key=$CERTPATH/privkey.pem

echo "kubectl -n $NAMESPACE create -f files/grpc-videp-server.yaml"
echo "kubectl -n $NAMESPACE create -f files/grpc-videp-server-clusterip-svc.yaml"

echo "kubectl -n $NAMESPACE create -f files/ingress-core.yaml"
echo "kubectl -n $NAMESPACE create -f files/ingress-forti.yaml"

exit

kubectl -n $NAMESPACE create -f files/grpc-videp-server.yaml
kubectl -n $NAMESPACE create -f httpbin-clusterip-svc.yaml

kubectl -n $NAMESPACE create -f ingress-core.yaml
kubectl -n $NAMESPACE create -f ingress-forti.yaml


