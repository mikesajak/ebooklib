import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useAuth } from './AuthContext';

const ImportContext = createContext();

export const useImport = () => {
  const context = useContext(ImportContext);
  if (!context) {
    throw new Error('useImport must be used within an ImportProvider');
  }
  return context;
};

export const ImportProvider = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const [sessions, setSessions] = useState({});
  const [scanStats, setScanStats] = useState(null);
  const [isConnected, setIsConnected] = useState(false);

  const fetchActiveSessions = useCallback(async () => {
    if (!isAuthenticated) return;
    try {
      const response = await fetch('/api/import/sessions');
      if (response.ok) {
        const data = await response.json();
        const sessionMap = {};
        data.forEach(s => {
          sessionMap[s.id] = s;
        });
        setSessions(sessionMap);
      }
    } catch (err) {
      console.error('Failed to fetch active import sessions', err);
    }
  }, [isAuthenticated]);

  const fetchScanStats = useCallback(async () => {
    if (!isAuthenticated) return;
    try {
      const response = await fetch('/api/admin/maintenance/storage-scan/stats');
      if (response.ok) {
        const data = await response.json();
        setScanStats(data);
      }
    } catch (err) {
      console.error('Failed to fetch scan stats', err);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (!isAuthenticated) {
      setSessions({});
      setScanStats(null);
      setIsConnected(false);
      return;
    }

    fetchActiveSessions();
    fetchScanStats();

    console.log('Connecting to SSE notification stream...');
    const eventSource = new EventSource('/api/notifications/stream');

    eventSource.onopen = () => {
      console.log('SSE connection established');
      setIsConnected(true);
    };

    eventSource.onerror = (err) => {
      console.error('SSE connection error', err);
      setIsConnected(false);
      // EventSource will automatically attempt to reconnect
    };

    eventSource.addEventListener('IMPORT_PROGRESS', (event) => {
      const session = JSON.parse(event.data).payload;
      console.log('SSE: Import progress received', session.id, session.status);
      
      setSessions(prev => {
        const newSessions = { ...prev };
        if (session.status === 'FINALIZED' || session.status === 'CANCELLED' || session.status === 'EXPIRED') {
          delete newSessions[session.id];
        } else {
          newSessions[session.id] = session;
        }
        return newSessions;
      });
    });

    eventSource.addEventListener('STORAGE_SCAN_PROGRESS', (event) => {
      const stats = JSON.parse(event.data).payload;
      console.log('SSE: Storage scan progress received', stats.status);
      setScanStats(stats);
    });

    eventSource.addEventListener('SYSTEM_NOTIFICATION', (event) => {
      const msg = JSON.parse(event.data).payload;
      console.log('SSE: System notification', msg);
    });

    return () => {
      console.log('Closing SSE connection');
      eventSource.close();
    };
  }, [isAuthenticated, fetchActiveSessions, fetchScanStats]);

  const value = {
    sessions: Object.values(sessions),
    scanStats,
    isConnected,
    refreshSessions: fetchActiveSessions,
    refreshScanStats: fetchScanStats
  };

  return (
    <ImportContext.Provider value={value}>
      {children}
    </ImportContext.Provider>
  );
};
