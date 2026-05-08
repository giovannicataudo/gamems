import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * ========================================================
 * COMPONENTE: Navbar
 * ========================================================
 * Questo componente cambia aspetto in base al ruolo dell'utente.
 * Utilizza Tailwind per restare leggero e responsivo.
 */
export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  // Se l'utente non è loggato, non mostriamo la barra (o potremmo mostrare solo il logo)
  if (!user) return null;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="bg-slate-800 border-b border-slate-700 p-4 shadow-lg">
      <div className="container mx-auto flex justify-between items-center">
        
        {/* LOGO E LINK PRINCIPALI */}
        <div className="flex items-center space-x-6">
          <Link to="/play" className="text-emerald-400 font-black text-xl tracking-tighter">
            GAME<span className="text-white">MS</span>
          </Link>
          
          <div className="hidden md:flex space-x-4 text-slate-300 font-medium">
            <Link title="Gioca ora" to="/play" className="hover:text-emerald-400 transition-colors">
                Lancia Moneta
            </Link>
            <Link title="Le tue partite" to="/history" className="hover:text-emerald-400 transition-colors">
                Storico
            </Link>
            <Link to="/profile" className="hover:text-emerald-400 transition-colors">
                Profilo
            </Link>
            
            {/* LOGICA DI AUTORIZZAZIONE:
               Solo se il ruolo è ADMIN, mostriamo il link al pannello di controllo.
               Questo link punta alla rotta frontend /admin.
            */}
            {user.role === 'ADMIN' && (
              <Link to="/admin" className="text-amber-400 hover:text-amber-300 font-bold border-l border-slate-600 pl-4">
                Pannello ADMIN
              </Link>
            )}
          </div>
        </div>

        {/* INFO UTENTE E LOGOUT */}
        <div className="flex items-center space-x-4">
          <div className="text-right hidden sm:block">
            <p className="text-white text-xs font-bold">{user.email}</p>
            <p className="text-emerald-500 text-[10px] uppercase tracking-widest">{user.role}</p>
          </div>
          
          <button
            onClick={handleLogout}
            className="bg-slate-700 hover:bg-red-600 text-white text-xs py-2 px-4 rounded transition-all"
          >
            Esci
          </button>
        </div>
      </div>
    </nav>
  );
}