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

# 2. Richiesta credenziali
echo ""
echo "🔐 Per permettere ad ArgoCD di scaricare i file dalla tua repository privata,"
echo "   è necessario inserire le tue credenziali GitHub."
echo "   (Usa il Token Fine-Grained di Sola Lettura appena generato!)"
echo ""
read -p "👤 Inserisci il tuo Username di GitHub: " GITHUB_USER
read -s -p "🔑 Inserisci il tuo Token GitHub (Read-Only): " GITHUB_TOKEN
echo ""
echo ""

if [ -z "$GITHUB_USER" ] || [ -z "$GITHUB_TOKEN" ]; then
    echo "❌ Errore: Username o Token mancanti. Installazione annullata."
    exit 1
fi

# 3. Installazione ArgoCD
echo "📦 Installazione di ArgoCD nel cluster (se non presente)..."
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -n argocd --server-side -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

echo "⏳ Attesa che i componenti di ArgoCD siano pronti (potrebbe volerci un minuto)..."
kubectl wait --for=condition=Ready pods --all -n argocd --timeout=300s

# 4. Creazione del Secret per la repository
echo "🔐 Configurazione del Secret per la repository privata..."
kubectl create secret generic gamems-repo-secret \
  -n argocd \
  --from-literal=type=git \
  --from-literal=url=https://github.com/giovannicataudo/gamems.git \
  --from-literal=username="$GITHUB_USER" \
  --from-literal=password="$GITHUB_TOKEN" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl label secret gamems-repo-secret argocd.argoproj.io/secret-type=repository -n argocd --overwrite

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
