import { useState, useEffect } from 'react';
import { apiClient } from '../api/axiosClient';

/**
 * ========================================================
 * 1. DEFINIZIONE DEI TIPI
 * ========================================================
 * Allineato con il record GameHistoryItemDto del backend.
 */
interface GameHistory {
  matchId: number;
  betAmount: number;
  userChoice: string;
  winningSide: string;
  hasWon: boolean;
  winAmount: number;
  playedAt: string;
}

export default function HistoryPage() {
  const [history, setHistory] = useState<GameHistory[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [errorMsg, setErrorMsg] = useState<string>('');

  useEffect(() => {
    fetchHistory();
  }, []);

  const fetchHistory = async () => {
    try {
      // Axios instrada automaticamente su http://localhost:8080/api/v1/game/history
      const response = await apiClient.get<GameHistory[]>('/game/history');
      setHistory(response.data);
    } catch (error: any) {
      setErrorMsg("Impossibile caricare lo storico delle partite.");
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  // Funzione helper per formattare la data ISO in formato leggibile (es. 07/05/2026, 16:42)
  const formatDate = (dateString: string) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('it-IT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className="max-w-6xl mx-auto mt-10 p-4">
      <div className="flex justify-between items-center mb-8">
        <h2 className="text-3xl font-black text-white">Storico Partite</h2>
        <button 
          onClick={fetchHistory}
          className="bg-slate-700 hover:bg-slate-600 text-white px-4 py-2 rounded text-sm font-bold transition-colors"
        >
          Aggiorna
        </button>
      </div>

      {errorMsg && (
        <div className="bg-red-500/10 border border-red-500 text-red-500 p-4 rounded mb-6 font-medium">
          {errorMsg}
        </div>
      )}

      {isLoading ? (
        <div className="text-center text-slate-400 py-10 animate-pulse">
          Caricamento dati in corso...
        </div>
      ) : history.length === 0 ? (
        <div className="bg-slate-800 p-10 rounded-xl border border-slate-700 text-center text-slate-400">
          Non hai ancora giocato nessuna partita.
        </div>
      ) : (
        <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-slate-300">
              <thead className="bg-slate-900/50 text-slate-400 text-xs uppercase tracking-wider">
                <tr>
                  <th className="px-6 py-4 font-bold">ID</th>
                  <th className="px-6 py-4 font-bold">Data</th>
                  <th className="px-6 py-4 font-bold">Puntata</th>
                  <th className="px-6 py-4 font-bold">Scelta</th>
                  <th className="px-6 py-4 font-bold">Risultato</th>
                  <th className="px-6 py-4 font-bold">Vincita</th>
                  <th className="px-6 py-4 font-bold text-center">Esito</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-700/50">
                {/* Ordiniamo l'array dal più recente al più vecchio prima di stamparlo */}
                {history
                  .sort((a, b) => new Date(b.playedAt).getTime() - new Date(a.playedAt).getTime())
                  .map((match) => (
                  <tr key={match.matchId} className="hover:bg-slate-700/20 transition-colors">
                    <td className="px-6 py-4 text-slate-500 font-mono">#{match.matchId}</td>
                    <td className="px-6 py-4 text-sm text-slate-400">{formatDate(match.playedAt)}</td>
                    <td className="px-6 py-4 font-medium text-white">€ {match.betAmount.toFixed(2)}</td>
                    <td className="px-6 py-4">{match.userChoice}</td>
                    <td className="px-6 py-4">{match.winningSide}</td>
                    <td className="px-6 py-4 font-medium text-white">
                      {match.winAmount > 0 ? `+ € ${match.winAmount.toFixed(2)}` : '€ 0.00'}
                    </td>
                    <td className="px-6 py-4 text-center">
                      <span className={`px-3 py-1 rounded-full text-xs font-black tracking-wide ${
                        match.hasWon 
                          ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/50' 
                          : 'bg-red-500/20 text-red-400 border border-red-500/50'
                      }`}>
                        {match.hasWon ? 'VINTO' : 'PERSO'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}