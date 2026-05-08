import { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';

/**
 * ========================================================
 * 1. DEFINIZIONE DEI TIPI (Il contratto TypeScript)
 * ========================================================
 * Diciamo esattamente a React che forma avranno i dati del nostro utente.
 * Deve combaciare con l'AuthResponseDto del backend Spring Boot.
 */
interface User {
  token: string;
  userId: number;
  email: string;
  role: string;
}

interface AuthContextType {
  user: User | null; // L'utente può esserci (loggato) o non esserci (null)
  login: (userData: User) => void;
  logout: () => void;
}

// Creiamo la "cassaforte" vuota
const AuthContext = createContext<AuthContextType | undefined>(undefined);

/**
 * ========================================================
 * 2. IL PROVIDER (Il gestore della cassaforte)
 * ========================================================
 * Questo componente avvolgerà l'intera applicazione.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  // Variabile di stato che contiene l'utente corrente
  const [user, setUser] = useState<User | null>(null);

  // Al caricamento dell'app controlliamo se c'è un utente salvato
  useEffect(() => {
    const storedUser = localStorage.getItem('gamems_user');
    const storedToken = localStorage.getItem('jwt_token'); // Per l'axiosClient

    if (storedUser && storedToken) {
      setUser(JSON.parse(storedUser));
    }
  }, []);

  // Metodo chiamato quando la pagina di Login riceve l'OK dal backend
  const login = (userData: User) => {
    setUser(userData);
    // Salviamo nel browser per non perdere il login se si aggiorna la pagina
    localStorage.setItem('gamems_user', JSON.stringify(userData));
    localStorage.setItem('jwt_token', userData.token); // Vitale per l'Interceptor di Axios!
  };

  // Metodo chiamato cliccando "Esci"
  const logout = () => {
    setUser(null);
    localStorage.removeItem('gamems_user');
    localStorage.removeItem('jwt_token');
  };

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * ========================================================
 * 3. IL CUSTOM HOOK (La chiave per aprire la cassaforte)
 * ========================================================
 * Invece di importare roba complessa, le nostre pagine useranno 
 * semplicemente: const { user, login, logout } = useAuth();
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth deve essere usato all\'interno di un AuthProvider');
  }
  return context;
}