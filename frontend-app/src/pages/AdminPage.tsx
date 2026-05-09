import { useState, useEffect } from 'react';
import { apiClient } from '../api/axiosClient';

/**
 * ========================================================
 * 1. DEFINIZIONE DEI CONTRATTI DTO
 * ========================================================
 */
interface AdminUser {
  id: number;
  email: string;
  role: string;
  enabled: boolean;
  createdAt: string;
}

interface UserListResponse {
  users: AdminUser[];
  totalCount: number;
}

interface AdminWallet {
  userId: number;
  totalBalance: number;
  realBalance: number;
  withdrawableBalance: number;
}

export default function AdminPage() {
  // --- STATI GLOBALI DASHBOARD ---
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [totalUsers, setTotalUsers] = useState<number>(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [isGameActive, setIsGameActive] = useState(true);
  
  // --- STATI UX ---
  const [isLoading, setIsLoading] = useState(true);
  const [message, setMessage] = useState({ text: '', type: '' });

  // --- STATI PER IL MODALE WALLET ---
  const [selectedWalletUser, setSelectedWalletUser] = useState<AdminUser | null>(null);
  const [userWalletData, setUserWalletData] = useState<AdminWallet | null>(null);
  const [adjustAmount, setAdjustAmount] = useState<string>('');
  const [adjustType, setAdjustType] = useState<'REAL' | 'WITHDRAWABLE'>('REAL');
  const [adjustReason, setAdjustReason] = useState<string>('');
  const [isWalletLoading, setIsWalletLoading] = useState(false);

  // --- 2. CARICAMENTO DATI INIZIALI ---
  useEffect(() => {
    loadAdminData();
  }, []);

  const loadAdminData = async () => {
    try {
      setIsLoading(true);
      const [usersRes, statusRes] = await Promise.all([
        apiClient.get<UserListResponse>('/admin/users'),
        apiClient.get<{ active: boolean }>('/game/status')
      ]);
      
      // Mappiamo correttamente basandoci sul UserListResponseDto
      setUsers(usersRes.data.users);
      setTotalUsers(usersRes.data.totalCount);
      setIsGameActive(statusRes.data.active);
    } catch (error) {
      showMsg("Errore nel caricamento dati admin", "error");
    } finally {
      setIsLoading(false);
    }
  };

  const showMsg = (text: string, type: 'success' | 'error') => {
    setMessage({ text, type });
    setTimeout(() => setMessage({ text: '', type: '' }), 4000);
  };

  // --- 3. AZIONI SISTEMA E UTENTI ---
  const toggleGameStatus = async () => {
    try {
      const nextStatus = !isGameActive;
      await apiClient.patch('/admin/game/status', { active: nextStatus });
      setIsGameActive(nextStatus);
      showMsg(`Gioco ${nextStatus ? 'ATTIVATO' : 'DISATTIVATO'}`, "success");
    } catch (error) {
      showMsg("Errore cambio stato gioco", "error");
    }
  };

  const handleUserStatus = async (id: number, currentEnabled: boolean) => {
    try {
      // Invia il JSON body come richiesto da UserStatusRequestDto
      await apiClient.patch(`/admin/users/${id}/status`, { enabled: !currentEnabled });
      setUsers(users.map(u => u.id === id ? { ...u, enabled: !currentEnabled } : u));
      showMsg(`Stato utente aggiornato con successo`, "success");
    } catch (error) {
      showMsg("Errore aggiornamento stato utente", "error");
    }
  };

  const handlePromote = async (id: number) => {
    if (!window.confirm("Attenzione: rendere questo utente ADMIN gli darà accesso totale. Procedere?")) return;
    try {
      await apiClient.post(`/admin/users/${id}/promote`);
      setUsers(users.map(u => u.id === id ? { ...u, role: 'ADMIN' } : u));
      showMsg("Utente promosso con successo", "success");
    } catch (error) {
      showMsg("Errore durante la promozione", "error");
    }
  };

  // --- 4. LOGICA GESTIONE WALLET (MODALE) ---
  const openWalletModal = async (user: AdminUser) => {
    setSelectedWalletUser(user);
    setIsWalletLoading(true);
    setUserWalletData(null);
    setAdjustAmount('');
    setAdjustReason('');
    try {
      // Recupera il portafoglio attuale dell'utente
      const res = await apiClient.get<AdminWallet>(`/admin/wallets/${user.id}`);
      setUserWalletData(res.data);
    } catch (error) {
      showMsg("Impossibile recuperare il portafoglio di questo utente", "error");
      closeWalletModal();
    } finally {
      setIsWalletLoading(false);
    }
  };

  const closeWalletModal = () => {
    setSelectedWalletUser(null);
  };

  const handleWalletAdjustment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedWalletUser) return;

    const amount = parseFloat(adjustAmount);
    if (isNaN(amount) || amount < 0.01) {
      showMsg("Inserisci un importo valido superiore a 0", "error");
      return;
    }
    if (adjustReason.trim().length === 0) {
      showMsg("La causale è obbligatoria per l'audit", "error");
      return;
    }

    try {
      // Payload esatto richiesto dal WalletAdjustmentRequestDto
      await apiClient.post(`/admin/wallets/${selectedWalletUser.id}/adjust`, {
        amount: amount,
        balanceType: adjustType,
        reason: adjustReason
      });
      showMsg("Saldo utente modificato con successo", "success");
      closeWalletModal();
    } catch (error: any) {
      showMsg(error.response?.data?.message || "Errore durante la modifica del saldo", "error");
    }
  };

  const filteredUsers = users.filter(u => 
    u.email.toLowerCase().includes(searchQuery.toLowerCase()) || 
    u.id.toString().includes(searchQuery)
  );

  if (isLoading) return <div className="p-10 text-center text-white animate-pulse">Inizializzazione dashboard...</div>;

  return (
    <div className="max-w-7xl mx-auto mt-10 p-4 space-y-8 relative">
      
      {/* HEADER & FEEDBACK */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-4xl font-black text-white tracking-tighter uppercase">Admin <span className="text-amber-500">Panel</span></h1>
          <p className="text-slate-400 text-sm mt-1">Utenti totali a sistema: {totalUsers}</p>
        </div>
        {message.text && (
          <div className={`px-4 py-2 rounded font-bold shadow-lg animate-fade-in ${message.type === 'success' ? 'bg-emerald-500 text-slate-900' : 'bg-red-500 text-white'}`}>
            {message.text}
          </div>
        )}
      </div>

      {/* BOX 1: CONTROLLO SISTEMA */}
      <div className="bg-slate-800 p-6 rounded-2xl border border-slate-700 shadow-2xl flex items-center justify-between">
        <div>
          <h3 className="text-xl font-bold text-white">Stato Globale Gioco</h3>
          <p className="text-slate-400 text-sm">Se disattivato, il sistema respingerà tutte le puntate.</p>
        </div>
        <button 
          onClick={toggleGameStatus}
          className={`px-8 py-3 rounded-xl font-black transition-all shadow-lg ${isGameActive ? 'bg-red-500 hover:bg-red-400 text-white shadow-red-500/20' : 'bg-emerald-500 hover:bg-emerald-400 text-slate-900 shadow-emerald-500/20'}`}
        >
          {isGameActive ? 'BLOCCA GIOCO' : 'RIATTIVA GIOCO'}
        </button>
      </div>

      {/* BOX 2: GESTIONE UTENTI E TABELLA */}
      <div className="bg-slate-800 rounded-2xl border border-slate-700 shadow-2xl overflow-hidden">
        <div className="p-6 border-b border-slate-700 flex flex-col md:flex-row gap-4 justify-between items-center bg-slate-800/50">
          <input 
            type="text" 
            placeholder="🔍 Cerca per email o ID..." 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="bg-slate-900 border border-slate-600 rounded-lg px-4 py-2 text-white w-full md:w-96 outline-none focus:border-amber-500 transition-colors"
          />
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead className="bg-slate-900/80 text-slate-400 text-xs uppercase font-black tracking-wider">
              <tr>
                <th className="px-6 py-4">ID</th>
                <th className="px-6 py-4">Utente</th>
                <th className="px-6 py-4">Ruolo</th>
                <th className="px-6 py-4 text-center">Accesso</th>
                <th className="px-6 py-4 text-right">Azioni Amministrative</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-700/50">
              {filteredUsers.map(u => (
                <tr key={u.id} className="hover:bg-slate-700/30 transition-colors">
                  <td className="px-6 py-4 font-mono text-slate-500">#{u.id}</td>
                  <td className="px-6 py-4 text-white font-medium">{u.email}</td>
                  <td className="px-6 py-4 text-xs font-bold uppercase">
                    <span className={u.role === 'ADMIN' ? 'text-amber-500 bg-amber-500/10 px-2 py-1 rounded' : 'text-slate-400'}>
                      {u.role}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-center">
                    <button 
                      onClick={() => handleUserStatus(u.id, u.enabled)}
                      className={`px-3 py-1 text-[10px] font-black rounded-full border transition-all ${
                        u.enabled 
                          ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30 hover:bg-red-500 hover:text-white hover:border-red-500' 
                          : 'bg-red-500/10 text-red-400 border-red-500/30 hover:bg-emerald-500 hover:text-white hover:border-emerald-500'
                      }`}
                      title="Clicca per invertire lo stato"
                    >
                      {u.enabled ? 'ATTIVO' : 'BANNATO'}
                    </button>
                  </td>
                  <td className="px-6 py-4 text-right space-x-3">
                    <button 
                      onClick={() => openWalletModal(u)}
                      className="text-xs font-bold text-emerald-400 hover:text-emerald-300 underline"
                    >
                      Gestisci Wallet
                    </button>
                    {u.role !== 'ADMIN' && (
                      <button 
                        onClick={() => handlePromote(u.id)}
                        className="text-xs font-bold text-amber-500 hover:text-amber-400 underline"
                      >
                        Promuovi
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {filteredUsers.length === 0 && (
            <div className="p-8 text-center text-slate-500 font-medium">
              Nessun utente trovato con i criteri di ricerca.
            </div>
          )}
        </div>
      </div>

      {/* =========================================================
          MODALE GESTIONE WALLET
          ========================================================= */}
      {selectedWalletUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="bg-slate-800 rounded-2xl border border-slate-600 shadow-2xl w-full max-w-md overflow-hidden animate-fade-in">
            
            {/* Header Modale */}
            <div className="bg-slate-900 p-4 border-b border-slate-700 flex justify-between items-center">
              <h3 className="text-white font-bold">Portafoglio Utente #{selectedWalletUser.id}</h3>
              <button onClick={closeWalletModal} className="text-slate-400 hover:text-white font-bold text-xl">&times;</button>
            </div>

            <div className="p-6">
              {isWalletLoading ? (
                <div className="text-center text-slate-400 py-6 animate-pulse">Recupero dati finanziari...</div>
              ) : userWalletData ? (
                <>
                  {/* Saldi Attuali */}
                  <div className="grid grid-cols-2 gap-4 mb-6 bg-slate-900/50 p-4 rounded-lg border border-slate-700">
                    <div>
                      <p className="text-[10px] text-slate-500 uppercase font-bold">Saldo Reale</p>
                      <p className="text-lg text-white font-mono">€ {userWalletData.realBalance.toFixed(2)}</p>
                    </div>
                    <div>
                      <p className="text-[10px] text-slate-500 uppercase font-bold">Saldo Prelevabile</p>
                      <p className="text-lg text-emerald-400 font-mono">€ {userWalletData.withdrawableBalance.toFixed(2)}</p>
                    </div>
                  </div>

                  {/* Form Modifica Saldo */}
                  <form onSubmit={handleWalletAdjustment} className="space-y-4">
                    <div>
                      <label className="block text-xs font-bold text-slate-400 mb-1">Tipo di Saldo da Modificare</label>
                      <select 
                        value={adjustType}
                        onChange={(e) => setAdjustType(e.target.value as 'REAL' | 'WITHDRAWABLE')}
                        className="w-full bg-slate-900 border border-slate-600 rounded px-3 py-2 text-white focus:border-amber-500 outline-none"
                      >
                        <option value="REAL">Saldo Reale (Depositi)</option>
                        <option value="WITHDRAWABLE">Saldo Prelevabile (Vincite)</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-400 mb-1">Importo (€)</label>
                      <input 
                        type="number" step="0.01" min="0.01" required
                        value={adjustAmount}
                        onChange={(e) => setAdjustAmount(e.target.value)}
                        placeholder="Es: 50.00"
                        className="w-full bg-slate-900 border border-slate-600 rounded px-3 py-2 text-white focus:border-amber-500 outline-none"
                      />
                      <p className="text-[10px] text-slate-500 mt-1">L'importo verrà aggiunto (usa la transazione lato backend per scalare fondi se necessario).</p>
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-400 mb-1">Causale (Audit Log)</label>
                      <input 
                        type="text" required
                        value={adjustReason}
                        onChange={(e) => setAdjustReason(e.target.value)}
                        placeholder="Motivo dell'accredito manuale..."
                        className="w-full bg-slate-900 border border-slate-600 rounded px-3 py-2 text-white focus:border-amber-500 outline-none"
                      />
                    </div>

                    <button 
                      type="submit"
                      className="w-full bg-emerald-600 hover:bg-emerald-500 text-white font-bold py-3 rounded-lg mt-4 transition-colors"
                    >
                      Conferma Operazione
                    </button>
                  </form>
                </>
              ) : (
                <div className="text-center text-red-400 py-6">Errore caricamento portafoglio.</div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}