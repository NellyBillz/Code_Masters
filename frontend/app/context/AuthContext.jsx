"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { getCurrentUser, logout as apiLogout } from "../../lib/api";

const AuthContext = createContext({
  user: null,
  loading: true,
  refresh: () => {},
  logout: () => {},
});

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const currentUser = await getCurrentUser();
      setUser(currentUser);
    } catch {
      // Not logged in (401) or a transient error — either way, no user.
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch-on-mount pattern; no derived-state alternative for reading auth session
    refresh();
  }, [refresh]);

  const logout = useCallback(async () => {
    try {
      await apiLogout();
    } catch {
      // If the POST itself fails (e.g. session already expired server-side),
      // there's nothing actionable to show the user — fall through to
      // refresh() below, which will resync the UI to whatever the server
      // actually thinks the session state is.
    } finally {
      // Re-fetch /users/me rather than optimistically clearing local state —
      // this is what actually confirms the server-side session is gone
      // (a 401 now) instead of just trusting the POST succeeded. Matches
      // the "log out, then refresh" round-trip the acceptance test checks.
      await refresh();
    }
  }, [refresh]);

  return (
    <AuthContext.Provider value={{ user, loading, refresh, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
