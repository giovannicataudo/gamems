#!/bin/bash

echo "🚀 [1/3] Compilazione dei microservizi Java..."
SERVICES=("user-service" "wallet-service" "game-service" "api-gateway")
for SERVICE in "${SERVICES[@]}"; do
    echo "⚙️ Compilando $SERVICE..."
    cd $SERVICE
    ./mvnw clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "❌ Errore compilazione $SERVICE"
        exit 1
    fi
    cd ..
done
echo "✅ Compilazione completata!"

echo "🐳 [2/3] Build delle immagini Docker..."
sudo docker compose build
if [ $? -ne 0 ]; then
    echo "❌ Errore build Docker"
    exit 1
fi
echo "✅ Build completata!"

echo "📦 [3/3] Importazione immagini in K3s..."
SERVICES_TO_IMPORT=("user-service" "wallet-service" "game-service" "api-gateway" "frontend-app")
for SVC in "${SERVICES_TO_IMPORT[@]}"; do
    echo "🔄 Importando $SVC:latest in K3s..."
    sudo docker tag gamems-${SVC}:latest ${SVC}:latest
    sudo docker save ${SVC}:latest | sudo k3s ctr images import -
done
echo "✅ Immagini caricate in K3s! ArgoCD le rileverà automaticamente."
