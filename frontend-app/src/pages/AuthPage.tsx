import { useState } from 'react';
// Strumento per "teletrasportare" l'utente in un'altra pagina
import { useNavigate } from 'react-router-dom'; 
// Il "postino" configurato prima
import { apiClient } from '../api/axiosClient';
// La chiave della "cassaforte" globale
import { useAuth } from '../context/AuthContext';

export default function AuthPage() {
  // --- 1. GESTIONE DELLO STATO (Le variabili di questa pagina) ---
   // True = Modalità Login, False = Modalità Registrazione
  const [isLogin, setIsLogin] = useState(true);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
   // Per mostrare gli errori del backend (es. "Password errata")
  const [errorMsg, setErrorMsg] = useState('');

  // Importiamo gli strumenti globali
  const { login } = useAuth();
  const navigate = useNavigate();

  // --- 2. GESTIONE DELL'INVIO DEL FORM ---
  // Questa funzione scatta quando l'utente preme il bottone "Accedi/Registrati"
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault(); // Blocca il comportamento di default di HTML che ricaricherebbe la pagina
    setErrorMsg(''); // Puliamo eventuali errori precedenti

    try {
      // Scegliamo la rotta giusta in base a cosa sta facendo l'utente
      const endpoint = isLogin ? '/auth/login' : '/auth/register';
      
      // Facciamo la chiamata asincrona al backend. 
      // Il payload corrisponde ESATTAMENTE ai tuoi LoginRequestDto / RegisterRequestDto
      const response = await apiClient.post(endpoint, {
        email: email,
        password: password
      });

      // Se arriviamo qui, il tuo Spring Boot ha risposto con successo (200 o 201)
      // Passiamo il payload in arrivo (AuthResponseDto) al nostro cervello globale
      // Faccio atterrare tutti su /play (sia admin che user)
      login(response.data);
      navigate('/play');

    } catch (error: any) {
      // --- 3. GESTIONE ERRORI ---
      // Catturiamo gli errori (es. il 401 BadCredentialsException o il 409 UserAlreadyExistsException)
      if (error.response && error.response.data && error.response.data.message) {
        // Estraiamo il messaggio dal tuo ErrorResponseDto!
        setErrorMsg(error.response.data.message);
      } else {
        setErrorMsg("Si è verificato un errore di connessione col server.");
      }
    }
  };

  // --- 4. DISEGNO DELL'INTERFACCIA (JSX + Tailwind) ---
  return (
    // Sfondo a tutto schermo, contenuto centrato
    <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4">
      {/* La "Card" bianca del form */}
      <div className="bg-slate-800 p-8 rounded-xl shadow-2xl w-full max-w-md border border-slate-700">
        
        {/* Titolo dinamico */}
        <h2 className="text-3xl font-bold text-white text-center mb-6">
          {isLogin ? 'Accedi al Gioco' : 'Crea un Account'}
        </h2>

        {/* Box per gli errori (visibile solo se errorMsg non è vuoto) */}
        {errorMsg && (
          <div className="bg-red-500/10 border border-red-500 text-red-500 p-3 rounded mb-4 text-sm">
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Campo Email */}
          <div>
            <label className="block text-slate-300 mb-1 text-sm">Email</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)} // Aggiorna lo stato ad ogni tasto premuto
              className="w-full p-3 rounded bg-slate-900 text-white border border-slate-600 focus:border-emerald-500 focus:outline-none"
              placeholder="giocatore@esempio.com"
            />
          </div>

          {/* Campo Password */}
          <div>
            <label className="block text-slate-300 mb-1 text-sm">Password</label>
            <input
              type="password"
              required
              // Il tuo RegisterRequestDto richiede min 8 caratteri, lo forziamo anche qui nell'HTML
              minLength={8} 
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full p-3 rounded bg-slate-900 text-white border border-slate-600 focus:border-emerald-500 focus:outline-none"
              placeholder="••••••••"
            />
          </div>

          {/* Bottone Invio */}
          <button
            type="submit"
            className="w-full bg-emerald-600 hover:bg-emerald-500 text-white font-bold py-3 rounded transition-colors"
          >
            {isLogin ? 'Entra' : 'Registrati'}
          </button>
        </form>

        {/* Il Toggle ("L'interruttore" Login/Registrazione) */}
        <div className="mt-6 text-center text-slate-400 text-sm">
          {isLogin ? "Non hai un account? " : "Hai già un account? "}
          <button
            type="button"
            onClick={() => {
              setIsLogin(!isLogin); // Inverte lo stato
              setErrorMsg(''); // Pulisce gli errori cambiando modalità
            }}
            className="text-emerald-400 hover:text-emerald-300 font-semibold underline"
          >
            {isLogin ? 'Registrati ora' : 'Accedi'}
          </button>
        </div>

      </div>
    </div>
  );
}