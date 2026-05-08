import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom' // Importiamo il gestore delle URL
import { AuthProvider } from './context/AuthContext' // Importiamo la nostra cassaforte
import App from './App.tsx'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    {/* 1. Abilitiamo il sistema di navigazione a URL */}
    <BrowserRouter>
      {/* 2. Avvolgiamo tutto nell'AuthProvider per avere l'utente globale */}
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)