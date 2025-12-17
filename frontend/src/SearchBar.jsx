import React, { useState, useEffect } from 'react';
import { useSearch } from './SearchContext';

const SearchBar = () => {
  const { searchQuery, setSearchQuery } = useSearch();
  const [localQuery, setLocalQuery] = useState(searchQuery);

  useEffect(() => {
    setLocalQuery(searchQuery);
  }, [searchQuery]);

  const handleSearch = () => {
    const operatorRegex = /[=<>!();,]/;
    if (localQuery && !operatorRegex.test(localQuery)) {
      const newQuery = `title=like="${localQuery}"`;
      setSearchQuery(newQuery);
    } else {
      setSearchQuery(localQuery);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  return (
    <div className="flex items-center">
      <input
        type="text"
        value={localQuery}
        onChange={(e) => setLocalQuery(e.target.value)}
        onKeyPress={handleKeyPress}
        placeholder="Search..."
        className="flex-grow px-2 py-1 border rounded-l-md focus:outline-none focus:ring-2 focus:ring-blue-500"
      />
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
