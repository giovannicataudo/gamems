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
  // Questo è l'indirizzo dell' API Gateway
  // Tutte le chiamate partiranno da qui. Es: apiClient.post('/auth/login') 
  // diventerà automaticamente http://localhost:8080/api/v1/auth/login
  baseURL: 'http://localhost:8080/api/v1',
  
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