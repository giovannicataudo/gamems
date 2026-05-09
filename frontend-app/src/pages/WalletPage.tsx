import { useState, useEffect } from 'react';
import { apiClient } from '../api/axiosClient';

interface WalletStatus {
  realBalance: number;
  withdrawableBalance: number;
  totalBalance: number;
}

export default function WalletPage() {
  const [wallet, setWallet] = useState<WalletStatus | null>(null);
  
  // Stati per i form
  const [depositAmount, setDepositAmount] = useState<string>('100.00');
  const [withdrawAmount, setWithdrawAmount] = useState<string>('');
  
  // Stati UI
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [message, setMessage] = useState<{ text: string, type: 'success' | 'error' } | null>(null);

  useEffect(() => {
    fetchWalletBalance();
  }, []);

  const fetchWalletBalance = async () => {
    try {
      const response = await apiClient.get<WalletStatus>('/wallet');
      setWallet(response.data);
    } catch (error) {
      showMsg("Impossibile caricare il portafoglio.", "error");
    } finally {
      setIsLoading(false);
    }
  };

  const showMsg = (text: string, type: 'success' | 'error') => {
    setMessage({ text, type });
    setTimeout(() => setMessage(null), 5000);
  };

  // --- LOGICA DEPOSITO ---
  const handleDeposit = async (e: React.FormEvent) => {
    e.preventDefault();
    const amount = parseFloat(depositAmount);
    if (isNaN(amount) || amount <= 0) return showMsg("Importo non valido.", "error");

    setIsProcessing(true);
    try {
      await apiClient.post('/wallet/deposit', { amount });
      await fetchWalletBalance();
      setDepositAmount('100.00');
      showMsg(`Ricarica di €${amount.toFixed(2)} effettuata con successo!`, "success");
    } catch (error: any) {
      showMsg(error.response?.data?.message || "Errore durante la ricarica.", "error");
    } finally {
      setIsProcessing(false);
    }
  };

  // --- LOGICA PRELIEVO ---
  const handleWithdraw = async (e: React.FormEvent) => {
    e.preventDefault();
    const amount = parseFloat(withdrawAmount);
    if (isNaN(amount) || amount <= 0) return showMsg("Importo non valido.", "error");
    
    if (wallet && amount > wallet.withdrawableBalance) {
      return showMsg("L'importo supera il tuo saldo prelevabile.", "error");
    }

    setIsProcessing(true);
    try {
      await apiClient.post('/wallet/withdraw', { amount });
      await fetchWalletBalance();
      setWithdrawAmount('');
      showMsg(`Prelievo di €${amount.toFixed(2)} elaborato.`, "success");
    } catch (error: any) {
      showMsg(error.response?.data?.message || "Errore durante il prelievo.", "error");
    } finally {
      setIsProcessing(false);
    }
  };

  if (isLoading) return <div className="text-center text-white mt-20 animate-pulse">Caricamento Cassa...</div>;

  return (
    <div className="max-w-4xl mx-auto mt-10 p-4 space-y-8">
      
      <div className="flex items-center justify-between">
        <h1 className="text-4xl font-black text-white uppercase tracking-tighter">
          La Tua <span className="text-emerald-500">Cassa</span>
        </h1>
        {message && (
          <div className={`px-4 py-2 rounded font-bold animate-fade-in shadow-lg ${message.type === 'success' ? 'bg-emerald-500 text-slate-900' : 'bg-red-500 text-white'}`}>
            {message.text}
          </div>
        )}
      </div>

      {/* DASHBOARD SALDI */}
      <div className="bg-slate-800 rounded-2xl border border-slate-700 p-8 shadow-2xl">
        <p className="text-slate-400 text-sm font-bold uppercase tracking-widest mb-2">Saldo Totale Disponibile</p>
        <p className="text-5xl font-black text-white font-mono mb-8">€ {wallet?.totalBalance.toFixed(2)}</p>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-6 border-t border-slate-700">
          <div className="bg-slate-900/50 p-4 rounded-xl border border-slate-700/50">
            <p className="text-slate-500 text-xs font-bold uppercase mb-1">Saldo Reale (Depositi)</p>
            <p className="text-xl font-bold text-slate-300 font-mono">€ {wallet?.realBalance.toFixed(2)}</p>
            <p className="text-[10px] text-slate-500 mt-2">Utilizzabile per giocare.</p>
          </div>
          <div className="bg-slate-900/50 p-4 rounded-xl border border-emerald-900/50">
            <p className="text-emerald-500 text-xs font-bold uppercase mb-1">Saldo Prelevabile (Vincite)</p>
            <p className="text-xl font-bold text-emerald-400 font-mono">€ {wallet?.withdrawableBalance.toFixed(2)}</p>
            <p className="text-[10px] text-emerald-600/70 mt-2">I fondi che puoi ritirare sul tuo conto.</p>
          </div>
        </div>
      </div>

      {/* SEZIONI OPERATIVE */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        
        {/* COLONNA RICARICA */}
        <div className="bg-slate-800 rounded-2xl border border-slate-700 p-6 shadow-xl relative overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-1 bg-blue-500"></div>
          <h3 className="text-xl font-bold text-white mb-6">➕ Ricarica Conto</h3>
          
          <form onSubmit={handleDeposit} className="space-y-4">
            <div>
              <label className="block text-slate-400 text-xs font-bold uppercase mb-2">Importo da depositare (€)</label>
              <input 
                type="number" step="0.01" min="5.00" required
                value={depositAmount}
                onChange={(e) => setDepositAmount(e.target.value)}
                disabled={isProcessing}
                className="w-full bg-slate-900 border border-slate-600 rounded-lg p-3 text-white focus:border-blue-500 outline-none transition-colors"
              />
            </div>
            <button 
              type="submit" 
              disabled={isProcessing}
              className="w-full bg-blue-600 hover:bg-blue-500 text-white font-bold py-3 rounded-lg transition-all disabled:opacity-50"
            >
              {isProcessing ? 'Elaborazione...' : 'Conferma Ricarica'}
            </button>
          </form>
        </div>

        {/* COLONNA PRELIEVO */}
        <div className="bg-slate-800 rounded-2xl border border-slate-700 p-6 shadow-xl relative overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-1 bg-emerald-500"></div>
          <h3 className="text-xl font-bold text-white mb-6">💸 Preleva Vincite</h3>
          
          <form onSubmit={handleWithdraw} className="space-y-4">
            <div>
              <label className="block text-slate-400 text-xs font-bold uppercase mb-2">Importo da prelevare (€)</label>
              <input 
                type="number" step="0.01" min="1.00" max={wallet?.withdrawableBalance} required
                value={withdrawAmount}
                onChange={(e) => setWithdrawAmount(e.target.value)}
                disabled={isProcessing || !wallet || wallet.withdrawableBalance <= 0}
                placeholder="0.00"
                className="w-full bg-slate-900 border border-slate-600 rounded-lg p-3 text-white focus:border-emerald-500 outline-none transition-colors disabled:opacity-50"
              />
            </div>
            <button 
              type="submit" 
              disabled={isProcessing || !wallet || wallet.withdrawableBalance <= 0}
              className="w-full bg-emerald-600 hover:bg-emerald-500 text-white font-bold py-3 rounded-lg transition-all disabled:opacity-50"
            >
              {isProcessing ? 'Elaborazione...' : 'Richiedi Prelievo'}
            </button>
            {wallet && wallet.withdrawableBalance <= 0 && (
              <p className="text-center text-xs text-slate-500 mt-2">Nessun fondo prelevabile disponibile.</p>
            )}
          </form>
        </div>

      </div>
    </div>
  );
}