#!/bin/bash

# ==========================================
# GAMEMS - K3S BUILD & DEPLOY SCRIPT
# ==========================================
# Questo script automatizza:
# 1. La compilazione dei microservizi Java (tramite mvnw)
# 2. La build delle immagini Docker
# 3. L'importazione delle immagini nel registro interno di K3s
# 4. Il riavvio dell'infrastruttura sul cluster
# ==========================================

echo "🚀 [1/4] Compilazione dei microservizi Java..."

# Definisci i microservizi da compilare
SERVICES=("user-service" "wallet-service" "game-service" "api-gateway")

for SERVICE in "${SERVICES[@]}"; do
    echo "⚙️ Compilando $SERVICE..."
    cd $SERVICE
    ./mvnw clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "❌ Errore durante la compilazione di $SERVICE! Interruzione."
        exit 1
    fi
    cd ..
done
echo "✅ Compilazione completata!"

echo "🐳 [2/4] Build delle immagini Docker tramite docker compose..."
sudo docker compose build
if [ $? -ne 0 ]; then
    echo "❌ Errore durante la build delle immagini Docker! Interruzione."
    exit 1
fi
echo "✅ Build delle immagini completata!"

echo "📦 [3/4] Esportazione e caricamento delle immagini in K3s..."
SERVICES_TO_IMPORT=("user-service" "wallet-service" "game-service" "api-gateway" "frontend-app")

for SVC in "${SERVICES_TO_IMPORT[@]}"; do
    echo "🔄 Importando $SVC:latest in K3s..."
    # Docker Compose V2 antepone il nome della cartella (gamems-) alle immagini buildate.
    # Le rinominiamo per farle combaciare con i file YAML di Kubernetes.
    sudo docker tag gamems-${SVC}:latest ${SVC}:latest
    sudo docker save ${SVC}:latest | sudo k3s ctr images import -
    if [ $? -ne 0 ]; then
        echo "❌ Errore durante l'importazione di ${SVC}:latest in K3s! Interruzione."
        exit 1
    fi
done
echo "✅ Immagini caricate con successo nel registro di K3s!"

echo "♻️ [4/4] Riavvio dell'infrastruttura Kubernetes..."
echo "Spegni l'infrastruttura corrente se accesa..."
./stop.sh
echo "Attendo 5 secondi per garantire la chiusura dei pod..."
sleep 5
echo "Riavvio l'infrastruttura..."
./start.sh

echo "========================================="
echo "🎉 TUTTO PRONTO! IL CLUSTER È AGGIORNATO 🎉"
echo "========================================="
