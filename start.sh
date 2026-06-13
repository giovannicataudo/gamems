#!/bin/bash

# Configurazione
NAMESPACE="gamems"
K8S_DIR="./k8s"

echo "🚀 Avvio automazione infrastruttura gamems kub..."

# 1. IL RECINTO (Nessuna dipendenza)
echo "🛡️  Configurazione Namespace, Sicurezza e Variabili..."
kubectl apply -f $K8S_DIR/00-namespace.yml
kubectl apply -f $K8S_DIR/gamems-configmap.yml -n $NAMESPACE
kubectl apply -f $K8S_DIR/gamems-secret.yml -n $NAMESPACE
kubectl apply -f $K8S_DIR/tls-secret.yml -n $NAMESPACE
kubectl apply -f $K8S_DIR/01-network-security.yml -n $NAMESPACE

# 2. STRATO DATI E MESSAGGISTICA (Dipendenze di base)
echo "📦 Avvio Postgres, RabbitMQ e Redis..."
kubectl apply -f $K8S_DIR/postgres.yml -n $NAMESPACE
kubectl apply -f $K8S_DIR/rabbit.yml -n $NAMESPACE
kubectl apply -f $K8S_DIR/redis.yml -n $NAMESPACE

echo "⏳ Attesa Strato Dati (Max 90s)..."
# Aspettiamo che tutti i pod dello strato dati abbiano il semaforo verde
kubectl wait --for=condition=ready pod -l app=postgres --timeout=90s -n $NAMESPACE
kubectl wait --for=condition=ready pod -l app=rabbitmq --timeout=90s -n $NAMESPACE
kubectl wait --for=condition=ready pod -l app=redis --timeout=90s -n $NAMESPACE
echo "✅ Strato Dati operativo!"

# 3. MICROSERVIZI CORE (Dipendono da DB e RabbitMQ)
echo "⚙️  Avvio User, Wallet e Game Service..."
kubectl apply -f $K8S_DIR/user.yml -n $NAMESPACE
kubectl apply -f $K8S_DIR/wallet.yml -n $NAMESPACE
kubectl apply -f $K8S_DIR/game.yml -n $NAMESPACE

echo "⏳ Attesa Microservizi Core (Max 120s)..."
# Usiamo i label dei deployment per assicurarci che Spring Boot sia avviato
kubectl wait --for=condition=ready pod -l app=user-service --timeout=120s -n $NAMESPACE
kubectl wait --for=condition=ready pod -l app=wallet-service --timeout=120s -n $NAMESPACE
kubectl wait --for=condition=ready pod -l app=game-service --timeout=120s -n $NAMESPACE
echo "✅ Microservizi Core operativi!"

echo " HPA..."
kubectl apply -f $K8S_DIR/hpa-game.yml

# 4. API GATEWAY E TOOLS (Dipendono dai Microservizi e Redis)
echo "🚪 Avvio API Gateway e Tools di Management..."
kubectl apply -f $K8S_DIR/gateway.yml -n $NAMESPACE
kubectl apply -f $K8S_DIR/adminer.yml -n $NAMESPACE
kubectl apply -f $K8S_DIR/commander.yml -n $NAMESPACE
kubectl apply -f $K8S_DIR/mailpit.yml -n $NAMESPACE

echo "⏳ Attesa API Gateway (Max 60s)..."
kubectl wait --for=condition=ready pod -l app=api-gateway --timeout=60s -n $NAMESPACE
echo "✅ API Gateway operativo!"

# 5. FRONTEND E INGRESS (L'ultimo miglio)
echo "🌐 Avvio Frontend React e Ingress Controller..."
kubectl apply -f $K8S_DIR/front.yml -n $NAMESPACE
kubectl apply -f $K8S_DIR/02-ingress.yml -n $NAMESPACE

# L'Ingress (Traefik) è già acceso nel kube-system, dobbiamo solo aspettare Nginx
kubectl wait --for=condition=ready pod -l app=frontend --timeout=60s -n $NAMESPACE

echo "========================================="
echo "🎉 INFRASTRUTTURA AVVIATA CON SUCCESSO 🎉"
echo "========================================="
echo "🔗 App Principale: http://gamems.local"
echo "🗄️  Database UI:  http://localhost:32080"
echo "🐇 RabbitMQ UI:  http://localhost:31672"
echo "🔴 Redis UI:     http://localhost:32081"
echo "📧 Mailpit UI:   http://localhost:32025"
echo "========================================="

# Mostra lo stato finale pulito
kubectl get pods -n $NAMESPACE