import { useState, useEffect } from 'react';
import { apiClient } from '../api/axiosClient';

// --- 1. DEFINIZIONE DEI DTO (Contratti TypeScript) ---
// Rispecchia esattamente il WalletResponseDto.java
interface WalletStatus {
  realBalance: number;
  withdrawableBalance: number;
  totalBalance: number;
}

// Rispecchia esattamente il GamePlayResponseDto.java
interface GameResult {
  matchId: number;
  userChoice: string;
  winningSide: string;
  hasWon: boolean;
  betAmount: number;
  winAmount: number;
  message: string;
}

 // Corrisponde al Boolean active del record Java
interface GameSystemStatus {
  active: boolean;
}

export default function PlayPage() {
  // --- 2. STATO DEL COMPONENTE (Variabili Reattive) ---
  const [wallet, setWallet] = useState<WalletStatus | null>(null);
  const [isGameActive, setIsGameActive] = useState<boolean>(true);
  const [systemMessage, setSystemMessage] = useState<string>('');
  
  // Input del form
  const [betAmount, setBetAmount] = useState<string>('1.00');
  const [withdrawAmount, setWithdrawAmount] = useState<string>('0.00');
  const [choice, setChoice] = useState<'TESTA' | 'CROCE'>('TESTA');
  
  // Feedback e UX
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isDepositing, setIsDepositing] = useState<boolean>(false);
  const [isWithdrawing, setIsWithdrawing] = useState<boolean>(false);
  const [gameResult, setGameResult] = useState<GameResult | null>(null);
  const [errorMsg, setErrorMsg] = useState<string>('');

  // --- 3. EFFETTI COLLATERALI (Al caricamento della pagina) ---
  // useEffect con array vuoto [] esegue questa funzione UNA SOLA VOLTA quando la pagina viene aperta
  useEffect(() => {
    fetchWalletBalance();
    checkGameStatus();
  }, []);

  const checkGameStatus = async () => {
    try {
      const response = await apiClient.get<GameSystemStatus>('/game/status');
      
      // Usiamo direttamente il booleano 'active' in arrivo dal backend
      setIsGameActive(response.data.active);
      
      if (!response.data.active) {
        setSystemMessage("Il gioco è momentaneamente disattivato dall'amministratore.");
      }
    } catch (error) {
      setIsGameActive(false);
      setSystemMessage("Impossibile connettersi al server di gioco.");
    }
  };

  // Funzione per recuperare il saldo
  const fetchWalletBalance = async () => {
    try {
      // Chiama GET /api/v1/wallet (Il Gateway lo instrada al Wallet Service)
      const response = await apiClient.get<WalletStatus>('/wallet');
      setWallet(response.data);
    } catch (error) {
      console.error("Impossibile recuperare il portafoglio", error);
    }
  };

  // --- Ricarica Rapida (Chiama il Wallet Service) ---
  const handleDeposit = async () => {
    setIsDepositing(true);
    setErrorMsg('');
    try {
      // Invia la richiesta di deposito con un importo fisso di 100€ per i test
      await apiClient.post('/wallet/deposit', { amount: 100.00 });
      // Ricarica il saldo a schermo
      await fetchWalletBalance();
    } catch (error: any) {
      setErrorMsg(error.response?.data?.message || "Errore durante la ricarica del conto.");
    } finally {
      setIsDepositing(false);
    }
  };

  /**
   * ========================================================
   * LOGICA DI PRELIEVO (Withdraw)
   * ========================================================
   * Invia l'importo desiderato al Wallet Service.
   * Il backend verificherà se l'importo è disponibile nel withdrawableBalance.
   */
  const handleWithdraw = async () => {
    const amount = parseFloat(withdrawAmount);
    if (isNaN(amount) || amount <= 0) {
      setErrorMsg("Inserisci un importo valido per il prelievo.");
      return;
    }

    setIsWithdrawing(true);
    setErrorMsg('');
    try {
      // Chiamata POST verso /api/v1/wallet/withdraw
      await apiClient.post('/wallet/withdraw', { amount: amount });
      
      // Reset dell'input e aggiornamento del saldo a video
      setWithdrawAmount('0.00');
      await fetchWalletBalance();
      
      alert("Prelievo effettuato con successo!");
    } catch (error: any) {
      // Gestiamo l'errore specifico (es. fondi insufficienti) restituito dal tuo backend
      setErrorMsg(error.response?.data?.message || "Errore durante il prelievo.");
    } finally {
      setIsWithdrawing(false);
    }
  };

  // --- 4. LOGICA DI GIOCO ---
  const handlePlay = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    setGameResult(null);
    setIsLoading(true);

    try {
      // Chiama POST /api/v1/game/play (Il Gateway lo instrada al Game Service)
      const response = await apiClient.post<GameResult>('/game/play', {
        betAmount: parseFloat(betAmount), // Convertiamo la stringa in numero decimale
        choice: choice
      });

      // Salviamo il risultato per mostrarlo a schermo
      setGameResult(response.data);
      
      // Aggiorniamo immediatamente il saldo interrogando di nuovo il Wallet
      // (Ricordi? Il Game Service ha scalato i soldi tramite chiamata sincrona interna,
      // e accreditato la vincita asincronamente via RabbitMQ)
      await fetchWalletBalance();

    } catch (error: any) {
      // Catturiamo gli errori (es. Saldo insufficiente dal tuo GameOperationException)
      if (error.response?.data?.message) {
        setErrorMsg(error.response.data.message);
      } else {
        setErrorMsg("Errore di connessione col server di gioco.");
      }
    } finally {
      setIsLoading(false); // Riabilitiamo i bottoni in ogni caso (successo o errore)
    }
  };

  // --- 5. DISEGNO DELL'INTERFACCIA (UI) ---
  return (
    <div className="max-w-2xl mx-auto mt-10 p-4 space-y-8">

      {!isGameActive && (
  <div className="bg-amber-500/10 border-l-4 border-amber-500 p-4 rounded shadow-md flex items-center gap-3">
    <span className="text-2xl">⚠️</span>
    <div>
      <h4 className="text-amber-500 font-bold text-sm uppercase">Sistema Offline</h4>
      {/* Qui usiamo la variabile systemMessage che abbiamo appena dichiarato */}
      <p className="text-slate-300 text-xs">{systemMessage}</p>
    </div>
  </div>
)}
      
      {/* SEZIONE 1: Il Portafoglio (Card Unica) */}
      <div className="bg-slate-800 p-6 rounded-xl border border-slate-700 shadow-xl space-y-6">
        
        {/* Intestazione Saldo */}
        <div className="flex justify-between items-center">
          <div>
            <h3 className="text-slate-400 text-sm font-bold uppercase tracking-wider">Il tuo Saldo</h3>
            <p className="text-3xl font-black text-emerald-400">
              {wallet ? `€ ${wallet.totalBalance.toFixed(2)}` : 'Caricamento...'}
            </p>
          </div>
          <button 
            onClick={handleDeposit}
            disabled={isDepositing || isLoading}
            className="bg-amber-500 hover:bg-amber-400 text-slate-900 font-bold text-xs py-2 px-4 rounded shadow transition-colors disabled:opacity-50"
          >
            {isDepositing ? 'Elaborazione...' : '+ Ricarica 100€'}
          </button>
        </div>

        {/* Dettaglio Saldi (Grid) */}
        <div className="grid grid-cols-2 gap-4 pt-4 border-t border-slate-700">
          <div className="text-sm">
            <p className="text-slate-500 italic">Saldo Reale</p>
            <p className="text-white font-mono">€ {wallet?.realBalance.toFixed(2) || '0.00'}</p>
          </div>
          <div className="text-sm">
            <p className="text-slate-500 italic">Saldo Prelevabile (Vincite)</p>
            <p className="text-emerald-500 font-mono">€ {wallet?.withdrawableBalance.toFixed(2) || '0.00'}</p>
          </div>
        </div>

        {/* Input e Tasto Prelievo */}
        <div className="flex gap-2 pt-2">
          <input 
            type="number"
            step="0.01"
            value={withdrawAmount}
            onChange={(e) => setWithdrawAmount(e.target.value)}
            className="flex-1 bg-slate-900 border border-slate-600 rounded px-3 py-2 text-white focus:border-emerald-500 outline-none transition-colors"
            placeholder="Quanto vuoi prelevare?"
          />
          <button 
            onClick={handleWithdraw}
            // Il tasto si disabilita se non ci sono soldi o se stiamo già lavorando
            disabled={isWithdrawing || isLoading || !wallet || wallet.withdrawableBalance <= 0}
            className="bg-slate-700 hover:bg-emerald-600 text-white font-bold py-2 px-6 rounded transition-all disabled:opacity-30 disabled:cursor-not-allowed"
          >
            {isWithdrawing ? 'Attendi...' : 'Preleva'}
          </button>
        </div>
      </div> {/* FINE CARD PORTAFOGLIO */}

      {/* SEZIONE 2: Il Tavolo da Gioco */}
      <div className="bg-slate-800 p-8 rounded-xl border border-slate-700 shadow-xl">
        <h2 className="text-2xl font-bold text-white mb-6 text-center">Tenta la Fortuna</h2>

        {errorMsg && (
          <div className="bg-red-500/10 border border-red-500 text-red-500 p-4 rounded mb-6 text-center font-medium">
            {errorMsg}
          </div>
        )}

        <form onSubmit={handlePlay} className="space-y-8">
          <div>
            <label className="block text-slate-300 mb-2 font-medium text-center">Importo Puntata (€)</label>
            <input
              type="number"
              step="0.01"
              min="1.00"
              required
              value={betAmount}
              onChange={(e) => setBetAmount(e.target.value)}
              disabled={isLoading}
              className="w-1/2 mx-auto block p-4 text-center text-2xl rounded bg-slate-900 text-white border border-slate-600 focus:border-emerald-500 focus:outline-none"
            />
          </div>

          <div className="flex justify-center gap-4">
            <button
              type="button"
              onClick={() => setChoice('TESTA')}
              disabled={isLoading}
              className={`flex-1 py-4 text-xl font-bold rounded-lg transition-all ${
                choice === 'TESTA' 
                  ? 'bg-amber-500 text-slate-900 shadow-[0_0_15px_rgba(245,158,11,0.5)]' 
                  : 'bg-slate-700 text-slate-400 hover:bg-slate-600'
              }`}
            >
              TESTA
            </button>
            <button
              type="button"
              onClick={() => setChoice('CROCE')}
              disabled={isLoading}
              className={`flex-1 py-4 text-xl font-bold rounded-lg transition-all ${
                choice === 'CROCE' 
                  ? 'bg-amber-500 text-slate-900 shadow-[0_0_15px_rgba(245,158,11,0.5)]' 
                  : 'bg-slate-700 text-slate-400 hover:bg-slate-600'
              }`}
            >
              CROCE
            </button>
          </div>

          <button
            type="submit"
            // Disabilita il bottone se il caricamento è in corso O se il gioco non è attivo
            disabled={isLoading || !isGameActive} 
            className="w-full bg-emerald-600 hover:bg-emerald-500 disabled:bg-slate-600 text-white font-black text-xl py-4 rounded-lg transition-all shadow-lg">
            {/* Cambia il testo in base allo stato */}
            {!isGameActive ? 'SISTEMA OFFLINE' : isLoading ? 'LANCIO IN CORSO...' : 'LANCIA LA MONETA'}
          </button>
        </form>
      </div>

      {/* SEZIONE 3: Il Risultato */}
      {gameResult && (
        <div className={`p-6 rounded-xl border text-center shadow-2xl animate-fade-in ${
          gameResult.hasWon ? 'bg-emerald-500/20 border-emerald-500' : 'bg-red-500/20 border-red-500'
        }`}>
          <h3 className={`text-3xl font-black mb-2 ${gameResult.hasWon ? 'text-emerald-400' : 'text-red-400'}`}>
            È USCITO {gameResult.winningSide}!
          </h3>
          <p className="text-slate-200 text-lg">{gameResult.message}</p>
        </div>
      )}

    </div>
  );
}