import axios from 'axios';

/**
 * ========================================================
 * CORE: Axios Client Centralizzato
 * ========================================================
 * Questa è l'unica "porta di uscita" del nostro frontend verso il backend.
 * Centralizzando la configurazione qui, evitiamo di dover ripetere 
 * l'URL del server o gli header in ogni singolo componente React.
 */

// Creiamo un'istanza personalizzata di Axios
export const apiClient = axios.create({
  // Impostiamo un percorso relativo in modo che il
  // browser chieda le api allo stesso server su cui
  // è esposto il front
  baseURL: '/api/v1',
  
  // Diciamo al backend che parleremo sempre in JSON
  headers: {
    'Content-Type': 'application/json',
    // Inseriamo l'API Key esterna che il Gateway richiede per far passare le richieste
    // (Questa deve combaciare con 'app.api-key' del tuo application.yml del Gateway)
    'X-Api-Key': 'LaTuaApiKeyEsternaSegreta2026!' 
  },
  
  // Timeout di sicurezza: se il backend non risponde entro 10 secondi, 
  // la chiamata fallisce evitando di bloccare l'interfaccia utente all'infinito.
  timeout: 10000, 
});

/**
 * ========================================================
 * INTERCEPTOR DELLE RICHIESTE (Filtro Uscente)
 * ========================================================
 * Questo blocco di codice viene eseguito automaticamente PRIMA 
 * che ogni singola richiesta parta fisicamente dal browser.
 */
apiClient.interceptors.request.use(
  (config) => {
    // 1. Cerchiamo il token JWT nella memoria locale del browser (LocalStorage).
    // Lo salveremo qui dentro quando faremo la pagina di Login.
    const token = localStorage.getItem('jwt_token');

    // 2. Se il token esiste, lo iniettiamo nell'header "Authorization" 
    // rispettando lo standard "Bearer " che Spring Boot si aspetta.
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // 3. Facciamo proseguire la richiesta verso il backend
    return config;
  },
  (error) => {
    // Se qualcosa va storto prima ancora di inviare la richiesta, rigettiamo l'errore
    return Promise.reject(error);
  }
);

/**
 * ========================================================
 * INTERCEPTOR DELLE RISPOSTE (Filtro Entrante)
 * ========================================================
 * Questo blocco viene eseguito automaticamente DOPO che il 
 * backend ha risposto, ma PRIMA che la risposta arrivi al 
 * componente React che ha fatto la chiamata.
 */
apiClient.interceptors.response.use(
  (response) => {
    // Se la chiamata è andata a buon fine (Status 2xx), passa oltre
    return response;
  },
  (error) => {
    // Se c'è un errore, controlliamo se è un 401 Unauthorized
    if (error.response && error.response.status === 401) {

      const requestUrl = error.config?.url || '';

      if (!requestUrl.includes('/auth/login')) {
        console.warn("🔐 Token scaduto o non valido. Disconnessione di sicurezza in corso.");
      
        // 1. Pulisci il LocalStorage dai dati sensibili
        localStorage.removeItem('jwt_token');
      
        // Se nel tuo AuthContext salvi anche l'utente in locale, rimuovilo
        localStorage.removeItem('gamems_user'); 
      
        // 2. Forza il redirect alla pagina di login
        window.location.href = '/login?expired=true';
      }
    }

    // Se il server risponde con 403 (Forbidden)
    if (error.response && error.response.status === 403) {
      const data = error.response.data;

      // Se il codice d'errore è quello del ban che abbiamo messo nel Gateway
      if (data && data.code === 'USER_BANNED') {
        // 1. Puliamo il token per evitare loop infiniti
        localStorage.removeItem('jwt_token');
        
        // 2. Teletrasportiamo l'utente alla pagina di ban
        // Usiamo window.location invece di useNavigate perché siamo fuori da un componente React
        window.location.href = '/banned';
      }
    }

    // Se si verifica il 409 non facciamo nulla, semplicmente stampiamo in console
    if (error.response && error.response.status === 409) {
      console.warn("⚠️ Conflitto di concorrenza: operazione simultanea intercettata dal backend.");
      return new Promise(()=>{});
    }

    // Se l'errore non viene intercettato viene passato oltre
    return Promise.reject(error);
  }
);