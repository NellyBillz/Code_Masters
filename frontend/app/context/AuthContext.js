'use client';

import React, { createContext, useContext, useEffect, useState } from 'react';
import { getMe, logout } from '../../lib/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    async function checkAuth() {
      setLoading(true);
      setError('');
      try {
        const userData = await getMe();
        setUser(userData);
      } catch (err) {
        if (err.status !== 401) {
          setError(err.message || 'Failed to check auth status');
        }
        setUser(null);
      } finally {
        setLoading(false);
      }
    }
    checkAuth();
  }, []);

  const handleLogout = async () => {
    try {
      await logout();
      setUser(null);
      window.location.href = '/';
    } catch (err) {
      setError(err.message || 'Logout failed');
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, error, logout: handleLogout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
