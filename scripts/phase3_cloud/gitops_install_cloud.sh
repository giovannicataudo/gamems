#!/bin/bash
set -e

echo "============================================================"
echo "☁️ INIZIALIZZAZIONE GITOPS CLOUD & GHCR (Fase 3) ☁️"
echo "============================================================"

# 1. Verifica prerequisiti
if ! command -v kubectl &> /dev/null; then
    echo "❌ Errore: kubectl non è installato o non è nel PATH."
    exit 1
fi

echo "✅ kubectl trovato."


# 3. Installazione ArgoCD
echo "📦 Installazione di ArgoCD nel cluster (se non presente)..."
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -n argocd --server-side -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

echo "⏳ Attesa che i componenti di ArgoCD siano pronti (potrebbe volerci un minuto)..."
kubectl wait --for=condition=Ready pods --all -n argocd --timeout=300s

# 4. Creazione Namespace Applicazione
echo "🛡️ Creazione preventiva del namespace gamems..."
kubectl create namespace gamems --dry-run=client -o yaml | kubectl apply -f -


# 7. Applicazione dell'app ArgoCD Cloud
echo "🚀 Avvio della sincronizzazione GitOps Cloud..."
kubectl apply -f argocd-apps/argocd-app-cloud.yml
kubectl annotate application gamems -n argocd argocd.argoproj.io/refresh=hard --overwrite || true

echo "============================================================"
echo "🎉 GITOPS CLOUD AVVIATO CON SUCCESSO! 🎉"
echo "============================================================"
echo "Da adesso Kubernetes scaricherà le immagini direttamente da GHCR."
echo "Non hai più bisogno di compilare il codice sul tuo PC!"
echo "Fai un 'git push' e GitHub Actions farà il resto."
echo "============================================================"
