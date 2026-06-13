import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { apiClient } from '../api/axiosClient';

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');
  const [qrCodeUri, setQrCodeUri] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  useEffect(() => {
    if (!token) {
      setErrorMsg("Nessun token fornito.");
      setLoading(false);
      return;
    }

    const verifyEmail = async () => {
      try {
        const response = await apiClient.get(`/auth/verify-email?token=${token}`);
        setQrCodeUri(response.data.qrCodeUri);
        setSuccessMsg(response.data.message || "Email verificata con successo!");
      } catch (error: any) {
        if (error.response && error.response.data && error.response.data.message) {
          setErrorMsg(error.response.data.message);
        } else {
          setErrorMsg("Errore di connessione o token non valido.");
        }
      } finally {
        setLoading(false);
      }
    };

    verifyEmail();
  }, [token]);

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="bg-slate-800 p-8 rounded-xl shadow-2xl w-full max-w-md border border-slate-700 text-center">
        
        <h2 className="text-3xl font-bold text-white mb-6">
          Verifica Email
        </h2>

        {loading ? (
          <div className="text-emerald-400 animate-pulse font-semibold">Verifica in corso...</div>
        ) : errorMsg ? (
          <div>
            <div className="bg-red-500/10 border border-red-500 text-red-500 p-3 rounded mb-4 text-sm">
              {errorMsg}
            </div>
            <button
              onClick={() => navigate('/login')}
              className="mt-4 w-full bg-slate-700 hover:bg-slate-600 text-white font-bold py-3 rounded transition-colors"
            >
              Torna al Login
            </button>
          </div>
        ) : (
          <div>
            <div className="bg-emerald-500/10 border border-emerald-500 text-emerald-400 p-3 rounded mb-4 text-sm font-semibold">
              {successMsg}
            </div>
            
            {qrCodeUri && (
              <div className="bg-white p-4 rounded-lg inline-block mb-4 shadow-inner">
                <img src={qrCodeUri} alt="QR Code" className="w-48 h-48 mx-auto" />
              </div>
            )}
            
            <p className="text-slate-300 mb-6 text-sm">
              Scansiona questo QR Code con la tua app Authenticator (es. Google Authenticator, Authy).
              Dovrai inserire il codice a 6 cifre ad ogni accesso.
            </p>

            <button
              onClick={() => navigate('/login')}
              className="w-full bg-emerald-600 hover:bg-emerald-500 text-white font-bold py-3 rounded transition-colors"
            >
              Vai al Login
            </button>
          </div>
        )}

      </div>
    </div>
  );
}
