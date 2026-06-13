import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom'; 
import { apiClient } from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';

export default function AuthPage() {
  // Modalità: "login", "register", "mfa"
  const [mode, setMode] = useState<'login' | 'register' | 'mfa'>('login');
  
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [tempToken, setTempToken] = useState('');
  
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const { login } = useAuth();
  const navigate = useNavigate();

  const [searchParams] = useSearchParams();
  const isExpired = searchParams.get('expired') === 'true';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    try {
      if (mode === 'register') {
        await apiClient.post('/auth/register', { email, password });
        setSuccessMsg("Account creato! Controlla la tua casella email per verificare l'indirizzo.");
        // Reset the form but keep them on the page to read the message
        setEmail('');
        setPassword('');
      } 
      else if (mode === 'login') {
        const response = await apiClient.post('/auth/login', { email, password });
        
        // Se il backend risponde con tempToken, ci serve il MFA
        if (response.data.tempToken) {
          setTempToken(response.data.tempToken);
          setMode('mfa');
        } else {
          // Fallback (se il backend fosse configurato per bypassare MFA in alcuni casi)
          login(response.data);
          navigate('/play');
        }
      }
      else if (mode === 'mfa') {
        const response = await apiClient.post('/auth/login/mfa', {
          tempToken: tempToken,
          code: otpCode
        });
        
        // Riceviamo il JWT finale
        login(response.data);
        navigate('/play');
      }

    } catch (error: any) {
      if (error.response && error.response.data && error.response.data.message) {
        setErrorMsg(error.response.data.message);
      } else {
        setErrorMsg("Si è verificato un errore di connessione col server.");
      }
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4">
      <div className="bg-slate-800 p-8 rounded-xl shadow-2xl w-full max-w-md border border-slate-700">
        
        <h2 className="text-3xl font-bold text-white text-center mb-6">
          {mode === 'login' && 'Accedi al Gioco'}
          {mode === 'register' && 'Crea un Account'}
          {mode === 'mfa' && 'Verifica a Due Passi'}
        </h2>

        {errorMsg && (
          <div className="bg-red-500/10 border border-red-500 text-red-500 p-3 rounded mb-4 text-sm">
            {errorMsg}
          </div>
        )}

        {successMsg && (
          <div className="bg-emerald-500/10 border border-emerald-500 text-emerald-400 p-3 rounded mb-4 text-sm font-semibold">
            {successMsg}
          </div>
        )}

        {isExpired && mode === 'login' && !successMsg && (
          <div className="bg-amber-500/10 border border-amber-500 text-amber-500 p-3 rounded text-sm mb-4 text-center">
            La tua sessione è scaduta per inattività. Effettua di nuovo l'accesso.
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          
          {(mode === 'login' || mode === 'register') && (
            <>
              <div>
                <label className="block text-slate-300 mb-1 text-sm">Email</label>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full p-3 rounded bg-slate-900 text-white border border-slate-600 focus:border-emerald-500 focus:outline-none"
                  placeholder="giocatore@esempio.com"
                />
              </div>
              <div>
                <label className="block text-slate-300 mb-1 text-sm">Password</label>
                <input
                  type="password"
                  required
                  minLength={8} 
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full p-3 rounded bg-slate-900 text-white border border-slate-600 focus:border-emerald-500 focus:outline-none"
                  placeholder="••••••••"
                />
              </div>
            </>
          )}

          {mode === 'mfa' && (
            <div>
              <label className="block text-slate-300 mb-1 text-sm text-center">Inserisci il codice a 6 cifre</label>
              <input
                type="text"
                required
                maxLength={6}
                value={otpCode}
                onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ''))} // Solo numeri
                className="w-full p-3 rounded bg-slate-900 text-white border border-slate-600 focus:border-emerald-500 focus:outline-none text-center tracking-widest text-xl font-mono"
                placeholder="123456"
              />
            </div>
          )}

          <button
            type="submit"
            className="w-full bg-emerald-600 hover:bg-emerald-500 text-white font-bold py-3 rounded transition-colors"
          >
            {mode === 'login' && 'Entra'}
            {mode === 'register' && 'Registrati'}
            {mode === 'mfa' && 'Verifica'}
          </button>
        </form>

        {(mode === 'login' || mode === 'register') && (
          <div className="mt-6 text-center text-slate-400 text-sm">
            {mode === 'login' ? "Non hai un account? " : "Hai già un account? "}
            <button
              type="button"
              onClick={() => {
                setMode(mode === 'login' ? 'register' : 'login');
                setErrorMsg('');
                setSuccessMsg('');
              }}
              className="text-emerald-400 hover:text-emerald-300 font-semibold underline"
            >
              {mode === 'login' ? 'Registrati ora' : 'Accedi'}
            </button>
          </div>
        )}

        {mode === 'mfa' && (
          <div className="mt-6 text-center">
            <button
              type="button"
              onClick={() => {
                setMode('login');
                setOtpCode('');
                setErrorMsg('');
              }}
              className="text-slate-400 hover:text-white text-sm underline"
            >
              Torna al Login
            </button>
          </div>
        )}

      </div>
    </div>
  );
}