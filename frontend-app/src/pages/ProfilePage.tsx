import { useState, useEffect } from 'react';
import { apiClient } from '../api/axiosClient'; // Utilizza il client configurato per l'API Gateway [cite: 138]

/**
 * ========================================================
 * 1. DEFINIZIONE DEL TIPO
 * ========================================================
 * Mappatura esatta del record UserProfileDto.java [cite: 122]
 */
interface UserProfile {
  id: number;
  email: string;
  role: string;
  createdAt: string;
}

export default function ProfilePage() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [errorMsg, setErrorMsg] = useState<string>('');

  // --- 2. RECUPERO DATI ---
  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      // Chiama GET /api/v1/users/profile
      const response = await apiClient.get<UserProfile>('/users/profile');
      setProfile(response.data);
    } catch (error: any) {
      setErrorMsg("Impossibile caricare le informazioni del profilo.");
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  // Helper per la data
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('it-IT', {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    });
  };

  if (isLoading) {
    return <div className="text-white p-10 text-center animate-pulse">Caricamento profilo...</div>;
  }

  return (
    <div className="max-w-3xl mx-auto mt-10 p-4">
      {/* Box Errore */}
      {errorMsg && (
        <div className="bg-red-500/10 border border-red-500 text-red-500 p-4 rounded-lg mb-6">
          {errorMsg}
        </div>
      )}

      {/* Card Profilo */}
      <div className="bg-slate-800 rounded-2xl border border-slate-700 overflow-hidden shadow-2xl">
        
        {/* Header con Iniziale Avatar */}
        <div className="bg-gradient-to-r from-slate-900 to-slate-800 p-8 border-b border-slate-700 flex items-center gap-6">
          <div className="w-20 h-20 bg-emerald-500 rounded-full flex items-center justify-center text-3xl font-black text-slate-900 shadow-lg">
            {profile?.email[0].toUpperCase()}
          </div>
          <div>
            <h2 className="text-2xl font-bold text-white">Profilo Giocatore</h2>
            <p className="text-emerald-400 font-medium">{profile?.email}</p>
          </div>
        </div>

        {/* Dettagli Tecnici (Allineati al DTO) */}
        <div className="p-8 space-y-6">
          <h3 className="text-slate-400 font-bold uppercase text-xs tracking-widest border-b border-slate-700 pb-2">
            Informazioni Sistema
          </h3>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* ID Utente */}
            <div className="bg-slate-900/40 p-4 rounded-lg border border-slate-700/50">
              <p className="text-slate-500 text-xs uppercase mb-1">Identificativo Utente</p>
              <p className="text-white font-mono font-bold">#{profile?.id}</p>
            </div>

            {/* Ruolo */}
            <div className="bg-slate-900/40 p-4 rounded-lg border border-slate-700/50">
              <p className="text-slate-500 text-xs uppercase mb-1">Permessi Account</p>
              <p className={`font-bold ${profile?.role === 'ADMIN' ? 'text-amber-400' : 'text-emerald-400'}`}>
                {profile?.role}
              </p>
            </div>

            {/* Data Creazione */}
            <div className="bg-slate-900/40 p-4 rounded-lg border border-slate-700/50">
              <p className="text-slate-500 text-xs uppercase mb-1">Data Registrazione</p>
              <p className="text-white font-medium">
                {profile?.createdAt ? formatDate(profile.createdAt) : '-'}
              </p>
            </div>

            {/* Stato (Placeholder logico) */}
            <div className="bg-slate-900/40 p-4 rounded-lg border border-slate-700/50">
              <p className="text-slate-500 text-xs uppercase mb-1">Stato Account</p>
              <p className="text-emerald-500 font-bold flex items-center gap-2">
                <span className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse"></span>
                ATTIVO
              </p>
            </div>
          </div>
        </div>

        {/* Footer Card */}
        <div className="bg-slate-900/20 p-6 text-center">
          <p className="text-slate-500 text-xs italic">
            Questi dati sono gestiti dal microservizio User Service e protetti tramite JWT[cite: 131, 141].
          </p>
        </div>
      </div>
    </div>
  );
}