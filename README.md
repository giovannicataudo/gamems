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
- **Mailpit**: Server SMTP locale e interfaccia web per l'intercettazione e la verifica delle email (es. link di registrazione).

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

## ☸️ Come avviare l'ambiente (Kubernetes / GitOps)

L'infrastruttura di GameMS su Kubernetes è gestita interamente seguendo il paradigma **GitOps** tramite **ArgoCD**.
Non vengono più utilizzati script imperativi (`start.sh` o `stop.sh`), ma il cluster si sincronizza autonomamente in modalità *Pull* dallo stato dichiarativo presente in questo repository (nella cartella `k8s/`). L'ordine di avvio delle dipendenze è orchestrato in modo nativo tramite le **Sync Waves** di ArgoCD.

**1. Requisiti GitOps (ArgoCD):**
Assicurati di avere ArgoCD installato nel tuo cluster e di aver applicato il file `argocd-app.yml` che istruisce il controller a osservare questo repository.

**2. Compilazione e caricamento immagini (Sviluppo Locale):**
Dato che l'infrastruttura locale utilizza `imagePullPolicy: Never` (per evitare download da registri remoti), devi prima compilare i servizi e caricare le immagini nel registro interno di K3s.
Per farlo in un solo colpo (installando ArgoCD, le tue credenziali GitHub di sola lettura e buildando il codice), usa lo script di installazione globale:
```bash
./gitops_install.sh
```
*Questo script si limita a compilare il codice Java, buildare le immagini Docker e iniettarle in K3s, senza riavviare o toccare lo stato del cluster, delegando l'orchestrazione interamente ad ArgoCD.*

**3. Accesso all'applicazione:**
Il frontend sarà raggiungibile tramite Ingress. Assicurati di aggiungere al tuo file `hosts` di sistema:
```text
127.0.0.1 gamems.local
```
Dopodiché, apri il browser su: `https://gamems.local`.

**4. Strumenti di management (Accesso locale tramite NodePort):**
- **Database UI (Adminer):** `http://localhost:32080` (Server: `postgres-service`)
- **RabbitMQ UI:** `http://localhost:31672`
- **Redis UI (Commander):** `http://localhost:32081`
- **Mailpit UI:** `http://localhost:32025`

---

## 📜 Guida agli Script (A cosa servono?)
Nella repository troverai vari script bash, ognuno pensato per uno scopo preciso a seconda di come vuoi gestire il cluster.

**Script per GitOps (Nuovo standard):**
- `gitops_install.sh`: Installa ArgoCD da zero, chiede il tuo token GitHub Read-Only e avvia l'intera architettura in automatico.
- `gitops_build.sh`: Compila il Java, builda le immagini Docker e le inietta in K3s. Usalo dopo ogni modifica al codice quando l'infrastruttura è già gestita da ArgoCD.

**Script Imperativi (Senza ArgoCD):**
Se per qualsiasi motivo non vuoi usare ArgoCD e preferisci gestire Kubernetes in modo "classico" e manuale:
- `deploy_to_k3s.sh`: Script "All-in-One" imperativo. Compila il codice, inietta le immagini in K3s e applica tutti i file YAML nell'ordine corretto tramite il comando `start.sh`.
- `start.sh`: Applica manualmente i file della cartella `k8s/` rispettando delle pause fisse (`sleep`) per aspettare l'avvio dei database.
- `stop.sh`: Scala a zero le repliche di tutti i deployment, spegnendo l'infrastruttura senza distruggere i file YAML (utile per liberare RAM). *Attenzione: non usare questo script se c'è ArgoCD, perché lui li riaccenderà subito!*

**Esempio di avvio SENZA ArgoCD:**
```bash
./deploy_to_k3s.sh
```
Questo script eseguirà compilazione, build e il deployment sul cluster Kubernetes, occupandosi lui stesso di orchestrare la creazione delle risorse al posto di ArgoCD.

## 🔒 Sicurezza

**Traffico Esterno (HTTPS):**
L'accesso all'applicazione tramite Kubernetes avviene in modo sicuro tramite protocollo `HTTPS` (`https://gamems.local`). La cifratura e la "TLS Termination" sono delegate all'Ingress Controller (Traefik), che sfrutta un certificato iniettato tramite il secret `gamems-tls`, garantendo che tutte le interazioni del client (inclusi gli invii di credenziali) viaggino protette su rete.

**Verifica Email e Multi-Factor Authentication (MFA):**
La piattaforma supporta l'autenticazione a più fattori basata su TOTP (Time-based One-Time Password).
1. Al momento della registrazione, l'utente è disabilitato finché non verifica il proprio indirizzo tramite un link temporaneo inviato per email (intercettato da Mailpit in locale).
2. Alla verifica, il backend genera un **QR Code** tramite il quale l'utente collega la propria app Authenticator (es. Google Authenticator).
3. Ogni successivo accesso richiede sia le credenziali base sia il codice numerico a 6 cifre OTP. Il JWT definitivo viene rilasciato solo dopo la validazione a due step.

**Gestione Secret Interni (Credenziali e JWT):**

Le password del database, le API Key e i token segreti per la firma dei JWT **non** sono hardcoded nel sorgente Java.
Essi vengono prelevati direttamente da variabili d'ambiente fornite esternamente:
- In locale tramite la sezione `environment` nel file `docker-compose.yml`.
- In Kubernetes tramite il secret configurato nel file `k8s/gamems-secret.yml` e il map in `k8s/gamems-configmap.yml`.

In caso di deploy in un ambiente di produzione reale, è fortemente raccomandato iniettare questi segreti attraverso sistemi di Vault management sicuri.

## 📈 Autoscaling e Performance

**Horizontal Pod Autoscaler (HPA)**
Il cluster K3s è configurato per scalare automaticamente il numero di repliche dei microservizi sotto carico, basandosi sul consumo della CPU:
- **Game Service:** Scala fino a 5 repliche al 60% di CPU.
- **API Gateway:** Scala fino a 4 repliche al 75% di CPU.
- **User & Wallet Service:** Scalano fino a 3 repliche al 70% di CPU.
- **Frontend App:** Scala fino a 2 repliche al 80% di CPU.

L'autoscaling garantisce che la piattaforma mantenga alte prestazioni nei momenti di picco di gioco, per poi ridurre il numero di pod quando il traffico diminuisce, risparmiando risorse.

**Build Docker Ottimizzate**
Ogni microservizio Java è dotato di file `.dockerignore` configurato su misura, che previene l'upload del compilato (`target/`) al Docker Daemon durante la pacchettizzazione. Questo riduce radicalmente i tempi di build, abbattendo l'uso della CPU ed ottimizzando lo script `./deploy_to_k3s.sh`.
