import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';

const SearchContext = createContext();

const SEARCH_HISTORY_PREFIX = 'ebooklib_search_history_';

export const SearchProvider = ({ children }) => {
  const [scopes, setScopes] = useState({});

  const initScope = useCallback((scope) => {
    setScopes(prev => {
      if (prev[scope]) return prev;
      
      let history = [];
      try {
        const stored = localStorage.getItem(`${SEARCH_HISTORY_PREFIX}${scope}`);
        if (stored) {
          history = JSON.parse(stored);
        }
      } catch (error) {
        console.error(`Failed to load search history for scope ${scope}`, error);
      }
      
      return {
        ...prev,
        [scope]: {
          searchQuery: '',
          searchHistory: history,
          refreshTrigger: 0
        }
      };
    });
  }, []);

  const triggerSearch = useCallback((scope, query) => {
    setScopes(prev => ({
      ...prev,
      [scope]: {
        ...prev[scope],
        searchQuery: query,
        refreshTrigger: (prev[scope]?.refreshTrigger || 0) + 1
      }
    }));
  }, []);

  const addToHistory = useCallback((scope, query) => {
    if (!query) return;
    
    setScopes(prev => {
      const scopeState = prev[scope];
      if (!scopeState) return prev; // Should be initialized
      
      const prevHistory = scopeState.searchHistory;
      // Avoid adding if it's the same as the most recent entry
      if (prevHistory.length > 0 && prevHistory[0] === query) {
        return prev;
      }
      
      const newHistory = [query, ...prevHistory.filter(item => item !== query)].slice(0, 10);
      try {
        localStorage.setItem(`${SEARCH_HISTORY_PREFIX}${scope}`, JSON.stringify(newHistory));
      } catch (error) {
        console.error(`Failed to save search history for scope ${scope}`, error);
      }
      
      return {
        ...prev,
        [scope]: {
          ...scopeState,
          searchHistory: newHistory
        }
      };
    });
  }, []);

  return (
    <SearchContext.Provider value={{ scopes, initScope, triggerSearch, addToHistory }}>
      {children}
    </SearchContext.Provider>
  );
};

export const useSearch = (scope) => {
  const context = useContext(SearchContext);
  if (!context) {
    throw new Error('useSearch must be used within a SearchProvider');
  }
  
  const { scopes, initScope, triggerSearch, addToHistory } = context;
  
  useEffect(() => {
    if (scope) {
      initScope(scope);
    }
  }, [scope, initScope]);
  
  const scopeState = scopes[scope] || { searchQuery: '', searchHistory: [], refreshTrigger: 0 };
  
  return {
    searchQuery: scopeState.searchQuery,
    searchHistory: scopeState.searchHistory,
    refreshTrigger: scopeState.refreshTrigger,
    triggerSearch: (query) => triggerSearch(scope, query),
    addToHistory: (query) => addToHistory(scope, query)
  };
};
