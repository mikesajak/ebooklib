import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);

  const checkAuth = useCallback(async () => {
    try {
      setLoading(true);
      const response = await fetch('/api/me');
      if (response.ok) {
        const data = await response.json();
        setUser(data);
        setIsAuthenticated(true);
      } else {
        setUser(null);
        setIsAuthenticated(false);
      }
    } catch (error) {
      console.error('Failed to check authentication', error);
      setUser(null);
      setIsAuthenticated(false);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  const login = useCallback(async (credentials) => {
    // Placeholder for actual login implementation in task-183
    // For now, it might just trigger checkAuth after some action
    console.log('Login attempt with', credentials);
    await checkAuth();
  }, [checkAuth]);

  const logout = useCallback(async () => {
    // Placeholder for actual logout implementation
    try {
      const response = await fetch('/logout', { method: 'POST' });
      if (response.ok) {
        setUser(null);
        setIsAuthenticated(false);
      }
    } catch (error) {
      console.error('Logout failed', error);
    } finally {
      await checkAuth();
    }
  }, [checkAuth]);

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, loading, login, logout, checkAuth }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
