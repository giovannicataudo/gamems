#!/bin/bash
set -e

echo "============================================================"
echo "🚀 INIZIALIZZAZIONE INFRASTRUTTURA GITOPS (GameMS) 🚀"
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


# 5. Build delle immagini
echo "🛠️ Compilazione e iniezione delle immagini Docker in locale (tramite gitops_build.sh)..."
if [ -f "./scripts/phase2_local/gitops_build.sh" ]; then
    chmod +x ./scripts/phase2_local/gitops_build.sh
    ./scripts/phase2_local/gitops_build.sh
else
    echo "❌ Errore: gitops_build.sh non trovato!"
    exit 1
fi

# 6. Applicazione dell'app ArgoCD
echo "🚀 Avvio della sincronizzazione GitOps..."
kubectl apply -f argocd-apps/argocd-app-local.yml
kubectl annotate application gamems -n argocd argocd.argoproj.io/refresh=hard --overwrite || true

echo "============================================================"
echo "🎉 INSTALLAZIONE COMPLETATA! 🎉"
echo "============================================================"
echo "ArgoCD ora gestirà in totale autonomia l'infrastruttura."
echo "Potrebbe volerci qualche minuto prima che tutti i servizi siano 'Healthy'."
echo ""
echo "🌍 Quando i servizi saranno pronti, l'app sarà raggiungibile su:"
echo "    https://gamems.local"
echo ""
echo "Per vedere in diretta cosa sta facendo ArgoCD, port-forwarda la UI:"
echo "    kubectl port-forward svc/argocd-server -n argocd 8080:443"
echo "Poi accedi su https://localhost:8080"
echo "============================================================"
