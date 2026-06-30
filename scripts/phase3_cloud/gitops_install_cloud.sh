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

# 2. Richiesta credenziali
echo ""
echo "🔐 Per permettere ad ArgoCD e a Kubernetes di scaricare i manifesti e le immagini da GitHub,"
echo "   è necessario inserire il tuo Token (Classic) con permessi 'read:packages'."
echo ""
read -p "👤 Inserisci il tuo Username di GitHub: " GITHUB_USER
read -s -p "🔑 Inserisci il tuo Token GitHub (Classic): " GITHUB_TOKEN
echo ""
echo ""

if [ -z "$GITHUB_USER" ] || [ -z "$GITHUB_TOKEN" ]; then
    echo "❌ Errore: Username o Token mancanti. Installazione annullata."
    exit 1
fi

# 3. Installazione ArgoCD
echo "📦 Installazione di ArgoCD nel cluster (se non presente)..."
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

echo "⏳ Attesa che i componenti di ArgoCD siano pronti (potrebbe volerci un minuto)..."
kubectl wait --for=condition=Ready pods --all -n argocd --timeout=300s

# 4. Creazione Namespace Applicazione
echo "🛡️ Creazione preventiva del namespace gamems..."
kubectl create namespace gamems --dry-run=client -o yaml | kubectl apply -f -

# 5. Creazione Secret per GitHub Container Registry (GHCR) in gamems
echo "🐳 Configurazione del Secret Docker Registry per GHCR..."
kubectl create secret docker-registry gamems-registry-secret \
  -n gamems \
  --docker-server=ghcr.io \
  --docker-username="$GITHUB_USER" \
  --docker-password="$GITHUB_TOKEN" \
  --docker-email="ghcr@gamems.local" \
  --dry-run=client -o yaml | kubectl apply -f -

# 6. Creazione del Secret per la repository Git in argocd
echo "🔐 Configurazione del Secret per la repository privata..."
kubectl create secret generic gamems-repo-secret \
  -n argocd \
  --from-literal=type=git \
  --from-literal=url=https://github.com/giovannicataudo/gamems.git \
  --from-literal=username="$GITHUB_USER" \
  --from-literal=password="$GITHUB_TOKEN" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl label secret gamems-repo-secret argocd.argoproj.io/secret-type=repository -n argocd --overwrite

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
