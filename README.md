# GameMS - Piattaforma di Gioco Online

GameMS è nato come un progetto didattico personale per esplorare e padroneggiare gli standard aziendali dell'ingegneria del software. Nel corso del tempo, l'architettura è stata iterativamente migliorata e stratificata: partendo dallo sviluppo dei singoli microservizi (Spring Boot e React), si è evoluta verso la containerizzazione, fino a raggiungere una complessa infrastruttura Cloud-Native completamente automatizzata (Kubernetes, GitOps, CI/CD). Questa repository documenta l'**evoluzione dell'infrastruttura in tre diverse fasi di apprendimento**.

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

## 🚀 Le 4 Fasi Evolutive del Progetto (Dal Locale al Cloud)

Il progetto è strutturato per permettere di avviare l'ambiente in base a quattro diversi paradigmi operativi, dimostrando concretamente il passaggio da un ambiente di sviluppo basilare fino al più "professionale (Cloud Native)".

### Fase 0: Sviluppo Locale (Docker Compose)
La primissima fase del progetto, ovvero il "Pre-Cluster". Ideale per lo sviluppo rapido in locale. Tutto viene fatto girare tramite un singolo comando Docker Compose senza alcun orchestratore Kubernetes di mezzo. 
- **File Principale:** `docker-compose.yml` (nella root)
- **Avvio:** `docker-compose up -d --build`
- **Frontend App:** [http://localhost:8080](http://localhost:8080)

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
- **Cartella YAML:** `k8s/phase3_cloud/`
- **Avvio "One-Click":** Esegui `./scripts/phase3_cloud/gitops_install_cloud.sh`. Poiché la repository e le immagini sono **pubbliche**, lo script configurerà ArgoCD nel tuo cluster e inizierà a scaricare tutto da GitHub in totale autonomia. Non è richiesta alcuna password o token!

> [!TIP]
> **Come testare o clonare il progetto?**
> Essendo un'infrastruttura Cloud-Native pubblica, chiunque può testarla! Ti basta clonare questa repository sul tuo PC, avere un cluster Kubernetes attivo (K3s, Minikube, Docker Desktop) e lanciare lo script della Fase 3. Il tuo cluster si sincronizzerà automaticamente con le ultime immagini stabili pubblicate sul GHCR originale.
> *(Nota: Le Fasi 1 e 2, poiché iniettano le immagini compilate in locale direttamente nel runtime del nodo, sono scriptate specificamente per K3s. La Fase 3 invece scarica dal cloud, quindi è universale per qualsiasi cluster K8s).*
> 
> Se invece vuoi **sviluppare** la tua versione, ti basterà fare un *Fork* su GitHub e modificare il file `argocd-apps/argocd-app-cloud.yml` inserendo il link alla tua repository. GitHub Actions, grazie a degli script intelligenti, aggiornerà tutti i percorsi delle immagini in totale autonomia ad ogni tuo push!



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
