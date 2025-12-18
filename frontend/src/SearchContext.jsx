import React, { createContext, useState, useContext, useEffect } from 'react';

const SearchContext = createContext();

const SEARCH_HISTORY_KEY = 'ebooklib_search_history';

export const SearchProvider = ({ children }) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchHistory, setSearchHistory] = useState([]);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  useEffect(() => {
    try {
      const storedHistory = localStorage.getItem(SEARCH_HISTORY_KEY);
      if (storedHistory) {
        setSearchHistory(JSON.parse(storedHistory));
      }
    } catch (error) {
      console.error("Failed to load search history from localStorage", error);
    }
  }, []);

  const triggerSearch = (query) => {
    setSearchQuery(query);
    setRefreshTrigger(prev => prev + 1);
  };

  const addToHistory = (query) => {
    if (!query) return;
    setSearchHistory(prevHistory => {
      // Avoid adding if it's the same as the most recent entry
      if (prevHistory.length > 0 && prevHistory[0] === query) {
        return prevHistory;
      }
      
      const newHistory = [query, ...prevHistory.filter(item => item !== query)].slice(0, 10);
      try {
        localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(newHistory));
      } catch (error) {
        console.error("Failed to save search history to localStorage", error);
      }
      return newHistory;
    });
  };

  return (
    <SearchContext.Provider value={{ searchQuery, setSearchQuery, searchHistory, addToHistory, refreshTrigger, triggerSearch }}>
      {children}
    </SearchContext.Provider>
  );
};

export const useSearch = () => useContext(SearchContext);
