# GameMS - Piattaforma di Gioco Online

GameMS è una piattaforma di gioco online moderna e scalabile, costruita su un'architettura a microservizi. Il progetto è stato concepito per dimostrare pattern avanzati di ingegneria del software, dalla containerizzazione all'orchestrazione su cluster Kubernetes, **mostrando l'evoluzione dell'infrastruttura in tre diverse fasi didattiche**.

## 🏗 Architettura

Il sistema è composto dai seguenti blocchi principali:

- **Frontend App (React / Vite)**: L'interfaccia utente web con cui interagiscono i giocatori.
- **API Gateway (Spring Cloud Gateway)**: Punto d'accesso unico per il frontend. Gestisce l'instradamento dinamico, l'autenticazione JWT, le API Key e il Rate Limiting.
- **User Service (Spring Boot)**: Gestisce l'autenticazione, la profilazione e l'identità degli utenti.
- **Game Service (Spring Boot)**: Gestisce le logiche di gioco e comunica asincronamente con gli altri servizi.
- **Wallet Service (Spring Boot)**: Gestisce i saldi dei giocatori, i depositi e i prelievi in maniera transazionale.

**Infrastruttura di supporto:**
- **PostgreSQL**: Database relazionale primario (`user_db`, `game_db`, `wallet_db`).
- **RabbitMQ**: Message Broker utilizzato per la comunicazione asincrona.
- **Redis**: Livello di caching e database in-memory.
- **Mailpit**: Server SMTP locale per intercettare email.

## 🚀 Le 3 Fasi Evolutive del Progetto (Kubernetes)

Il progetto è strutturato in cartelle separate che permettono di avviare il cluster in base a tre diversi paradigmi operativi, dal più "manuale" al più "professionale (Cloud Native)".

### Fase 1: Approccio Imperativo (Senza ArgoCD)
L'approccio più semplice. Kubernetes viene gestito interamente tramite comandi bash che applicano i file YAML al cluster. Ideale per capire le basi.
- **Cartella Script:** `scripts/phase1_imperative/`
- **Cartella YAML:** `k8s/phase2_local/` (condivisa con la fase 2)
- **Avvio All-in-One:** Esegui `./scripts/phase1_imperative/deploy_to_k3s.sh` (compila, inietta in K3s e avvia tutto).

### Fase 2: GitOps Locale (ArgoCD + Build manuale)
Introduzione del paradigma GitOps. I file YAML non vengono più spinti manualmente, ma **ArgoCD** si sincronizza in autonomia leggendo la repository. Poiché le immagini sono salvate localmente, bisogna ancora buildarle a mano.
- **Cartella Script:** `scripts/phase2_local/`
- **Cartella YAML:** `k8s/phase2_local/` (con `imagePullPolicy: Never`)
- **Avvio:** Esegui `./scripts/phase2_local/gitops_install.sh` (installa ArgoCD, builda le immagini locali e si collega a GitHub).

### Fase 3: GitOps Cloud (GitHub Actions + GHCR)
Il vero paradigma Cloud-Native aziendale. Non hai più bisogno di compilare niente sul tuo computer. Fai semplicemente un `git push` del codice sorgente. **GitHub Actions** compilerà il codice, spingerà le immagini Docker nel **GitHub Container Registry (GHCR)** e aggiornerà i manifesti. ArgoCD infine ordinerà a Kubernetes di scaricare la nuova immagine da internet.
- **Cartella Script:** `scripts/phase3_cloud/`
- **Cartella YAML:** `k8s/phase3_cloud/` (con `imagePullPolicy: IfNotPresent` e `imagePullSecrets`)
- **Avvio:** Esegui `./scripts/phase3_cloud/gitops_install_cloud.sh`. Ti verrà chiesto il tuo Token GitHub (Classic) con permessi `read:packages` per permettere al cluster di scaricare le immagini private da GHCR.

## 🛠 Come avviare l'ambiente (Locale / Docker Compose)

Il progetto è dotato di un file `docker-compose.yml` preconfigurato.
```bash
docker-compose up -d --build
```
- **Frontend App:** [http://localhost:8080](http://localhost:8080)

## ☸️ Accesso all'applicazione (Kubernetes)

Il frontend sarà raggiungibile tramite Ingress. Assicurati di aggiungere al tuo file `hosts` di sistema:
```text
127.0.0.1 gamems.local
```
Dopodiché, apri il browser su: `https://gamems.local`.

**Strumenti di management (Accesso locale tramite NodePort):**
- **Database UI (Adminer):** `http://localhost:32080`
- **RabbitMQ UI:** `http://localhost:31672`
- **Redis UI:** `http://localhost:32081`
- **Mailpit UI:** `http://localhost:32025`

## 🔒 Sicurezza
L'accesso all'applicazione tramite Kubernetes avviene in modo sicuro tramite protocollo `HTTPS` (`https://gamems.local`). La cifratura e la "TLS Termination" sono delegate all'Ingress Controller (Traefik).
La piattaforma supporta l'autenticazione a più fattori basata su TOTP (Time-based One-Time Password) con generazione QR Code per l'app Authenticator.
Le credenziali del database non sono hardcoded, ma iniettate tramite `Secret` in Kubernetes.

## 📈 Autoscaling e Performance
Il cluster K3s è configurato per scalare automaticamente il numero di repliche dei microservizi sotto carico (HPA), basandosi sul consumo della CPU. Le build Docker dei servizi Java utilizzano il `.dockerignore` per ottimizzare l'upload del context al demone Docker.
