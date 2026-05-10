export default function BannedPage() {
  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4 font-sans">
      <div className="max-w-md w-full bg-slate-800 border border-slate-700 rounded-2xl p-8 text-center shadow-2xl">
        
        {/* ICONA COERENTE CON IL TUO LOGO */}
        <div className="flex justify-center mb-6">
          <div className="w-20 h-20 bg-red-500/10 rounded-2xl flex items-center justify-center border border-red-500/20">
            <svg 
              xmlns="http://www.w3.org/2000/svg" 
              fill="none" viewBox="0 0 24 24" 
              strokeWidth={1.5} 
              stroke="currentColor" 
              className="w-10 h-10 text-red-500"
            >
              <path strokeLinecap="round" strokeLinejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z" />
            </svg>
          </div>
        </div>
        
        {/* TESTO */}
        <h1 className="text-2xl font-black text-white mb-2 tracking-tighter">
          ACCOUNT <span className="text-red-500">SOSPESO</span>
        </h1>
        
        <p className="text-slate-400 mb-8 text-sm leading-relaxed">
          Il sistema di sicurezza ha rilevato una violazione dei termini d'uso. 
          L'accesso ai servizi GAME<span className="text-emerald-500">MS</span> è stato revocato.
        </p>

        <div className="bg-slate-900/50 border border-slate-700 rounded-xl p-4 mb-8">
          <p className="text-xs text-slate-500 uppercase tracking-widest font-bold mb-1">Supporto Tecnico</p>
          <p className="text-emerald-500 font-mono text-sm">support@gamems.it</p>
        </div>

        <button 
          onClick={() => window.location.href = '/auth'}
          className="text-slate-500 hover:text-slate-300 text-xs transition-colors underline decoration-slate-700"
        >
          Torna alla pagina di accesso
        </button>
      </div>
    </div>
  );
}