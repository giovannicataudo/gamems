# GameMS - Piattaforma di Gioco Online

GameMS è una piattaforma di gioco online moderna e scalabile, costruita su un'architettura a microservizi. Il progetto è stato concepito per dimostrare pattern avanzati di ingegneria del software, dalla containerizzazione all'orchestrazione su cluster Kubernetes.

## 🏗 Architettura

Il sistema è composto dai seguenti blocchi principali:

- **Frontend App (React / Vite)**: L'interfaccia utente web con cui interagiscono i giocatori, ottimizzata e pacchettizzata tramite Nginx.
- **API Gateway (Spring Cloud Gateway)**: Punto d'accesso unico per il frontend. Gestisce l'instradamento dinamico verso i microservizi interni, l'autenticazione JWT, le API Key e il Rate Limiting.
- **User Service (Spring Boot)**: Gestisce l'autenticazione, la profilazione e l'identità degli utenti.
- **Game Service (Spring Boot)**: Gestisce le logiche di gioco e comunica asincronamente con gli altri servizi.
- **Wallet Service (Spring Boot)**: Gestisce i saldi dei giocatori, i depositi e i prelievi in maniera transazionale.

**Infrastruttura di supporto:**
- **PostgreSQL**: Database relazionale primario. Suddiviso logicamente per ogni microservizio (`user_db`, `game_db`, `wallet_db`).
- **RabbitMQ**: Message Broker utilizzato per la comunicazione asincrona e ad eventi tra i microservizi.
- **Redis**: Livello di caching e database in-memory, sfruttato dall'API Gateway per funzionalità come il Rate Limiting (Bucket4j).

## 🚀 Pre-requisiti

Per avviare il progetto avrai bisogno di:
- **Docker** e **Docker Compose** (per l'esecuzione locale).
- **K3s / Minikube** o un qualsiasi cluster Kubernetes locale (per testare l'orchestrazione).
- **kubectl** configurato per puntare al tuo cluster locale.
- Java 25 (opzionale, solo per sviluppare senza container).
- Node.js (opzionale, solo per sviluppare il frontend senza container).

## 🛠 Come avviare l'ambiente (Locale / Docker Compose)

Il progetto è dotato di un file `docker-compose.yml` preconfigurato con tutti i container, inclusa l'infrastruttura (Postgres, RabbitMQ, Redis) e i microservizi Java assieme al Frontend React.

```bash
# Entra nella directory del progetto
cd gamems

# Avvia tutta l'infrastruttura in background
docker-compose up -d --build
```
Una volta avviato:
- **Frontend App:** [http://localhost:8080](http://localhost:8080)
- **API Gateway:** Esposto internamente, accessibile tramite il frontend (Nginx fa da proxy inverso sotto `/api/`).

Per fermare l'infrastruttura locale:
```bash
docker-compose down
```

## ☸️ Come avviare l'ambiente (Cluster Kubernetes / K3s)

Il progetto include tutti i manifesti Kubernetes necessari e degli script bash di automazione per accendere e spegnere agilmente il cluster.

**Metodo Consigliato (Script Unificato):**
È presente uno script automatico che compila i microservizi Java, builda le immagini Docker e le inietta direttamente in K3s prima di avviare il cluster.
```bash
./deploy_to_k3s.sh
```
Questo script eseguirà tutte le fasi necessarie in un solo passaggio.

**Avvio Manuale:**
Se le immagini sono già presenti nel registro di K3s, puoi avviare e fermare l'infrastruttura manualmente:
1. Avvia l'infrastruttura sul cluster:
   ```bash
   ./start.sh
   ```
   *Lo script si occuperà di creare i namespace, avviare prima lo strato dati (Postgres, RabbitMQ, Redis), attendere che siano pronti, avviare i microservizi core, il gateway e infine il frontend con il rispettivo Ingress.*

2. Il frontend sarà raggiungibile tramite Ingress. Assicurati di aggiungere al tuo file `hosts` di sistema:
   ```text
   127.0.0.1 gamems.local
   ```
   Dopodiché, apri il browser su: `http://gamems.local`.

3. Strumenti di management:
   - **Database UI (Adminer):** `http://localhost:32080`
   - **RabbitMQ UI:** `http://localhost:31672`
   - **Redis UI (Commander):** `http://localhost:32081`

4. Per spegnere o ridurre a zero le repliche del cluster (Scale-to-0):
   ```bash
   ./stop.sh
   ```

## 🔒 Sicurezza

Le password del database, le API Key e i token segreti per la firma dei JWT **non** sono hardcoded nel sorgente Java.
Essi vengono prelevati direttamente da variabili d'ambiente fornite esternamente:
- In locale tramite la sezione `environment` nel file `docker-compose.yml`.
- In Kubernetes tramite il secret configurato nel file `k8s/gamems-secret.yml` e il map in `k8s/gamems-configmap.yml`.

In caso di deploy in un ambiente di produzione reale, è fortemente raccomandato iniettare questi segreti attraverso sistemi di Vault management sicuri.
