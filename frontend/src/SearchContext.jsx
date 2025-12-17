import React, { createContext, useState, useContext, useEffect } from 'react';

const SearchContext = createContext();

const SEARCH_HISTORY_KEY = 'ebooklib_search_history';

export const SearchProvider = ({ children }) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchHistory, setSearchHistory] = useState([]);

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

  const addToHistory = (query) => {
    if (!query) return;
    setSearchHistory(prevHistory => {
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
    <SearchContext.Provider value={{ searchQuery, setSearchQuery, searchHistory, addToHistory }}>
      {children}
    </SearchContext.Provider>
  );
};

export const useSearch = () => useContext(SearchContext);
