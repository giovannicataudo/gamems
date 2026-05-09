import { useState, useEffect, useRef } from 'react';
import { apiClient } from '../api/axiosClient';

// --- 1. DEFINIZIONE DEI DTO (Contratti TypeScript) ---
interface WalletStatus {
  realBalance: number;
  withdrawableBalance: number;
  totalBalance: number;
}

interface GameResult {
  matchId: number;
  userChoice: string;
  winningSide: string;
  hasWon: boolean;
  betAmount: number;
  winAmount: number;
  message: string;
}

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
  const [choice, setChoice] = useState<'TESTA' | 'CROCE'>('TESTA');
  
  // Feedback e UX
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isDepositing, setIsDepositing] = useState<boolean>(false);
  const [gameResult, setGameResult] = useState<GameResult | null>(null);
  const [errorMsg, setErrorMsg] = useState<string>('');

  // --- STATI PER L'ANIMAZIONE E AUDIO ---
  const [isFlipping, setIsFlipping] = useState<boolean>(false);
  const [visualSide, setVisualSide] = useState<'TESTA' | 'CROCE'>('TESTA');
  const [isAudioEnabled, setIsAudioEnabled] = useState<boolean>(false);

  // --- REFERENZE AUDIO ---
  // Rimosso il bgMusicRef per la musica di sottofondo costante. Manteniamo solo gli SFX.
  const sfxRef = useRef<HTMLAudioElement | null>(null);

  // URL delle tracce audio (Suoni di interazione e risultato)
  const SFX = {
    click: 'https://assets.mixkit.co/active_storage/sfx/2568/2568-preview.mp3',
    win: 'https://assets.mixkit.co/active_storage/sfx/2000/2000-preview.mp3', // Suono allegro
    lose: 'https://assets.mixkit.co/active_storage/sfx/2003/2003-preview.mp3'  // Suono triste/trombone
  };

  // --- 3. EFFETTI COLLATERALI ---
  useEffect(() => {
    fetchWalletBalance();
    checkGameStatus();

    sfxRef.current = new Audio();
    sfxRef.current.volume = 0.8;
  }, []);

  // --- GESTIONE AUDIO (Solo Effetti Speciali) ---
  const toggleAudio = () => {
    setIsAudioEnabled(!isAudioEnabled);
  };

  const playSfx = (type: 'click' | 'win' | 'lose', force: boolean = false) => {
    // Se l'audio non è abilitato e NON stiamo forzando l'avvio, esci
    if ((!isAudioEnabled && !force) || !sfxRef.current) return;
    
    sfxRef.current.src = SFX[type];
    sfxRef.current.play().catch(() => {});
  };

  // --- CHIAMATE API ---
  const checkGameStatus = async () => {
    try {
      const response = await apiClient.get<GameSystemStatus>('/game/status');
      setIsGameActive(response.data.active);
      if (!response.data.active) {
        setSystemMessage("Il gioco è momentaneamente disattivato dall'amministratore.");
      }
    } catch (error) {
      setIsGameActive(false);
      setSystemMessage("Impossibile connettersi al server di gioco.");
    }
  };

  const fetchWalletBalance = async () => {
    try {
      const response = await apiClient.get<WalletStatus>('/wallet');
      setWallet(response.data);
    } catch (error) {
      console.error("Impossibile recuperare il portafoglio", error);
    }
  };

  const handleDeposit = async () => {
    setIsDepositing(true);
    setErrorMsg('');
    try {
      await apiClient.post('/wallet/deposit', { amount: 100.00 });
      await fetchWalletBalance();
      playSfx('win'); // Suono gratificante alla ricarica
    } catch (error: any) {
      setErrorMsg(error.response?.data?.message || "Errore durante la ricarica del conto.");
    } finally {
      setIsDepositing(false);
    }
  };

  // --- 4. LOGICA DI GIOCO ---
  const handlePlay = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    setGameResult(null);
    setIsLoading(true);

    if (!isAudioEnabled) {
      setIsAudioEnabled(true);
    }
    
    // Inizializza l'animazione e il click
    setIsFlipping(true);
    playSfx('click');

    try {
      // Chiama POST /api/v1/game/play
      const response = await apiClient.post<GameResult>('/game/play', {
        betAmount: parseFloat(betAmount),
        choice: choice
      });

      // TENSIONE: 4 secondi di attesa per permettere alla moneta di ruotare a sufficienza
      setTimeout(async () => {
        setIsFlipping(false);
        setVisualSide(response.data.winningSide as 'TESTA' | 'CROCE');
        
        // Risultato a schermo
        setGameResult(response.data);
        await fetchWalletBalance();
        setIsLoading(false);

        // Suono finale di vittoria o sconfitta
        if (response.data.hasWon) {
          playSfx('win');
        } else {
          playSfx('lose');
        }
      }, 4000); // 4 Secondi di durata animazione

    } catch (error: any) {
      setIsFlipping(false);
      setIsLoading(false);
      
      if (error.response?.data?.message) {
        setErrorMsg(error.response.data.message);
      } else {
        setErrorMsg("Errore di connessione col server di gioco.");
      }
    } 
  };

  // Helper per attivare l'audio implicitamente al primo clic sulla scelta, se spento
  const handleChoiceClick = (selected: 'TESTA' | 'CROCE') => {
    setChoice(selected);
    if (!isAudioEnabled) {
      setIsAudioEnabled(true);
    }
    // Forziamo il suono del click
    playSfx('click', true); 
  };

  // --- 5. DISEGNO DELL'INTERFACCIA (UI COMPATTATA) ---
  return (
    // Spaziature e padding ridotti (mt-2, p-2, space-y-4) per evitare lo scroll verticale
    <div className="max-w-3xl mx-auto mt-2 p-2 sm:p-4 space-y-4">

      {!isGameActive && (
        <div className="bg-amber-500/10 border-l-4 border-amber-500 p-3 rounded shadow-md flex items-center gap-3">
          <span className="text-xl">⚠️</span>
          <div>
            <h4 className="text-amber-500 font-bold text-xs uppercase">Sistema Offline</h4>
            <p className="text-slate-300 text-xs">{systemMessage}</p>
          </div>
        </div>
      )}
      
      {/* SEZIONE 1: Top Bar (Wallet Rapido Centrato) */}
      <div className="bg-slate-800/80 p-4 rounded-xl border border-slate-700 shadow-xl flex flex-col justify-center items-center gap-3 backdrop-blur-sm relative">
        
        {/* Audio Toggle (Posizionato assolutamente a sinistra per non intaccare il centraggio centrale) */}
        <button 
          onClick={toggleAudio} 
          className={`absolute left-4 top-1/2 -translate-y-1/2 p-2 rounded-full transition-colors ${isAudioEnabled ? 'bg-emerald-500/20 text-emerald-400' : 'bg-slate-700 text-slate-400 hover:text-white'}`}
          title="Attiva/Disattiva Effetti Sonori"
        >
          {isAudioEnabled ? '🔊' : '🔇'}
        </button>

        {/* Saldo ridotto e testo centrato */}
        <div className="text-center mt-1">
          <p className="text-slate-400 text-[10px] font-bold uppercase tracking-wider">Saldo Disponibile</p>
          <p className="text-lg font-bold text-emerald-400 font-mono leading-none mt-1">
            {wallet ? `€ ${wallet.totalBalance.toFixed(2)}` : '...'}
          </p>
        </div>
        
        {/* Bottone Ricarica Ingrandito */}
        <button 
          onClick={handleDeposit}
          disabled={isDepositing || isLoading}
          className="bg-amber-500 hover:bg-amber-400 text-slate-900 font-black text-sm sm:text-base py-3 px-10 rounded-xl shadow-lg shadow-amber-500/20 transition-all disabled:opacity-50"
        >
          {isDepositing ? '...' : '+ RICARICA RAPIDA 100€'}
        </button>
      </div>

      {/* SEZIONE 2: Il Tavolo da Gioco */}
      <div className="bg-slate-800 p-4 sm:p-6 rounded-2xl border border-slate-700 shadow-2xl flex flex-col items-center relative overflow-hidden">
        
        <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-emerald-500 via-amber-500 to-emerald-500 opacity-50"></div>

        <h2 className="text-2xl font-black text-white mb-6 text-center tracking-tighter uppercase">Fai la tua <span className="text-amber-500">Puntata</span></h2>

        {/* LA MONETA 3D (Margini e scala ridotti) */}
        <div className="flex justify-center mb-6">
          <div className="coin-wrapper scale-90">
            <div className={`coin-inner ${isFlipping ? 'animate-spin-slow' : visualSide === 'TESTA' ? 'land-heads' : 'land-tails'}`}>
              <div className="coin-side heads">TESTA</div>
              <div className="coin-side tails">CROCE</div>
            </div>
          </div>
        </div>

        {errorMsg && (
          <div className="w-full bg-red-500/10 border border-red-500 text-red-500 p-3 rounded-lg mb-4 text-center text-sm font-medium animate-fade-in">
            {errorMsg}
          </div>
        )}

        <form onSubmit={handlePlay} className="w-full max-w-md space-y-5">
          
          {/* Input Puntata */}
          <div className="bg-slate-900/50 p-4 rounded-xl border border-slate-700/50">
            <label className="block text-slate-400 mb-2 font-bold text-center text-xs uppercase tracking-widest">Quanto vuoi puntare?</label>
            <div className="relative">
              <span className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 font-bold text-xl">€</span>
              <input
                type="number"
                step="0.01"
                min="1.00"
                required
                value={betAmount}
                onChange={(e) => setBetAmount(e.target.value)}
                disabled={isLoading}
                className="w-full bg-slate-900 text-white border border-slate-600 focus:border-amber-500 focus:ring-1 focus:ring-amber-500 rounded-lg py-3 pl-10 pr-4 text-center text-2xl font-black outline-none transition-all disabled:opacity-50"
              />
            </div>
          </div>

          {/* Scelta Testa/Croce */}
          <div className="flex justify-center gap-3">
            <button
              type="button"
              onClick={() => handleChoiceClick('TESTA')}
              disabled={isLoading}
              className={`flex-1 py-4 text-lg font-black rounded-xl transition-all border-2 ${
                choice === 'TESTA' 
                  ? 'bg-amber-500 border-amber-400 text-slate-900 shadow-[0_0_15px_rgba(245,158,11,0.3)]' 
                  : 'bg-slate-800 border-slate-600 text-slate-400 hover:bg-slate-700'
              }`}
            >
              TESTA
            </button>
            <button
              type="button"
              onClick={() => handleChoiceClick('CROCE')}
              disabled={isLoading}
              className={`flex-1 py-4 text-lg font-black rounded-xl transition-all border-2 ${
                choice === 'CROCE' 
                  ? 'bg-amber-500 border-amber-400 text-slate-900 shadow-[0_0_15px_rgba(245,158,11,0.3)]' 
                  : 'bg-slate-800 border-slate-600 text-slate-400 hover:bg-slate-700'
              }`}
            >
              CROCE
            </button>
          </div>

          {/* Bottone Gioca */}
          <button
            type="submit"
            disabled={isLoading || !isGameActive} 
            className="w-full bg-emerald-600 hover:bg-emerald-500 disabled:bg-slate-700 disabled:text-slate-500 text-white font-black text-xl py-4 rounded-xl transition-all shadow-lg active:scale-95"
          >
            {!isGameActive ? 'SISTEMA OFFLINE' : isLoading ? 'ATTENDI...' : 'LANCIA LA MONETA'}
          </button>
        </form>
      </div>

      {/* SEZIONE 3: Il Risultato */}
      {gameResult && !isLoading && (
        <div className={`p-4 rounded-xl border-2 text-center shadow-xl animate-bounce ${
          gameResult.hasWon ? 'bg-emerald-500/10 border-emerald-500' : 'bg-red-500/10 border-red-500'
        }`}>
          <h3 className={`text-2xl font-black mb-1 ${gameResult.hasWon ? 'text-emerald-400' : 'text-red-400'}`}>
            {gameResult.hasWon ? 'VITTORIA!' : 'SCONFITTA'}
          </h3>
          <p className="text-white text-base font-medium">{gameResult.message}</p>
        </div>
      )}

    </div>
  );
}