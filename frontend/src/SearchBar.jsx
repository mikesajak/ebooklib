import React, { useState, useEffect, useRef } from 'react';
import { useSearch } from './SearchContext';

const SearchBar = () => {
  const { searchQuery, setSearchQuery, searchHistory, addToHistory } = useSearch();
  const [localQuery, setLocalQuery] = useState(searchQuery);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const searchBarRef = useRef(null);

  useEffect(() => {
    setLocalQuery(searchQuery);
  }, [searchQuery]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (searchBarRef.current && !searchBarRef.current.contains(event.target)) {
        setIsHistoryOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [searchBarRef]);

  const handleSearch = () => {
    const operatorRegex = /[=<>!();,]/;
    let queryToSearch = localQuery;
    if (localQuery && !operatorRegex.test(localQuery)) {
      queryToSearch = `title=like="${localQuery}"`;
    }
    
    if (queryToSearch) {
      addToHistory(queryToSearch);
    }
    setSearchQuery(queryToSearch);
    setIsHistoryOpen(false);
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  const handleClear = () => {
    setLocalQuery('');
    setSearchQuery('');
  };

  const handleHistoryClick = (query) => {
    setLocalQuery(query);
    setSearchQuery(query);
    setIsHistoryOpen(false);
  };

  return (
    <div className="flex items-center" ref={searchBarRef}>
      <div className="relative flex-grow">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center">
          <button onClick={() => setIsHistoryOpen(!isHistoryOpen)} className="focus:outline-none">
            <svg className="h-5 w-5 text-gray-400 hover:text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </button>
        </div>
        <input
          type="text"
          value={localQuery}
          onChange={(e) => setLocalQuery(e.target.value)}
          onKeyPress={handleKeyPress}
          onFocus={() => setIsHistoryOpen(true)}
          placeholder="Search..."
          className="w-full pl-10 pr-10 py-1 border rounded-l-md focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        {localQuery && (
          <div className="absolute inset-y-0 right-0 pr-3 flex items-center">
            <button onClick={handleClear} className="focus:outline-none">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-gray-400 hover:text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        )}
        {isHistoryOpen && searchHistory.length > 0 && (
          <div className="absolute z-10 mt-1 w-full bg-white border border-gray-300 rounded-md shadow-lg">
            <ul>
              {searchHistory.map((query, index) => (
                <li
                  key={index}
                  onClick={() => handleHistoryClick(query)}
                  className="px-4 py-2 hover:bg-gray-100 cursor-pointer"
                >
                  {query}
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
      <button
        onClick={handleSearch}
        className="px-4 py-1 bg-blue-500 text-white rounded-r-md hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
        Search
      </button>
    </div>
  );
};

export default SearchBar;
