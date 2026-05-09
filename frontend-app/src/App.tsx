import { Routes, Route, Navigate } from 'react-router-dom';
import AuthPage from './pages/AuthPage';
import Navbar from './components/Navbar';
import PlayPage from './pages/PlayPage';
import WalletPage from './pages/WalletPage';
import HistoryPage from './pages/HistoryPage';
import ProfilePage from './pages/ProfilePage';
import AdminPage from './pages/AdminPage';
import { useAuth } from './context/AuthContext';

export default function App() {
  const { user } = useAuth();

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100">
      {/* La Navbar è fuori dalle Routes: 
          apparirà sempre in alto se l'utente è loggato! 
      */}
      <Navbar />

      <main className="container mx-auto">
        <Routes>
          {/* Rotta pubblica per Login/Register */}
          <Route 
            path="/login" 
            element={!user ? <AuthPage /> : <Navigate to="/play" />} 
          />

          {/* Rotte protette: accessibili solo se loggati */}
          <Route 
            path="/play" 
            element={user ? <PlayPage /> : <Navigate to="/login" />} 
          />

          <Route 
            path="/wallet" 
            element={user ? <WalletPage /> : <Navigate to="/login" />} 
          />
          
          <Route 
            path="/history" 
            element={user ? <HistoryPage /> : <Navigate to="/login" />} 
          />

          {/* Rotta profile: accessibile solo se ruolo loggati */}
          <Route 
          path="/profile" 
          element={user ? <ProfilePage /> : <Navigate to="/login" />} />

          {/* Rotta Admin: accessibile solo se ruolo === ADMIN */}
          <Route 
            path="/admin" 
            element={user?.role === 'ADMIN' ? <AdminPage /> : <Navigate to="/play" />} 
          />

          {/* Fallback: se non sai dove andare, vai al gioco o al login */}
          <Route path="*" element={<Navigate to={user ? "/play" : "/login"} />} />
        </Routes>
      </main>
    </div>
  );
}