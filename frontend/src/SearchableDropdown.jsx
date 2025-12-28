import React, { useState, useEffect, useRef } from 'react';

const SearchableDropdown = ({ options, value, onChange, placeholder, label, id }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const dropdownRef = useRef(null);
  const valueRef = useRef(value);
  const optionsRef = useRef(options);

  // Keep refs in sync with latest props to avoid closure issues in setTimeout
  useEffect(() => {
    valueRef.current = value;
    optionsRef.current = options;
  }, [value, options]);

  // Synchronize searchTerm with selected value
  useEffect(() => {
    const selectedOption = options.find(opt => opt.id === value);
    setSearchTerm(selectedOption ? selectedOption.name : '');
  }, [value, options]);

  const filteredOptions = options.filter(option =>
    option.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleInputChange = (e) => {
    setSearchTerm(e.target.value);
    setIsOpen(true);
  };

  const handleOptionClick = (option) => {
    setSearchTerm(option.name);
    setIsOpen(false);
    onChange(option.id);
  };

  const handleBlur = () => {
    // Small delay to allow handleOptionClick to trigger before closing
    setTimeout(() => {
      const selectedOption = optionsRef.current.find(opt => opt.id === valueRef.current);
      setSearchTerm(selectedOption ? selectedOption.name : '');
      setIsOpen(false);
    }, 200);
  };

  return (
    <div className="mb-4 relative" ref={dropdownRef}>
      {label && (
        <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor={id}>
          {label}:
        </label>
      )}
      <div className="relative">
        <input
          type="text"
          id={id}
          autoComplete="off"
          value={searchTerm}
          onChange={handleInputChange}
          onFocus={() => setIsOpen(true)}
          onBlur={handleBlur}
          placeholder={placeholder}
          className="shadow appearance-none border rounded w-full py-2 pl-3 pr-10 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
        />
        {value && (
          <button
            type="button"
            onClick={() => onChange('')}
            className="absolute inset-y-0 right-0 px-3 flex items-center text-gray-400 hover:text-gray-600"
            title="Clear"
          >
            ✖
          </button>
        )}
      </div>
      {isOpen && filteredOptions.length > 0 && (
        <ul className="absolute z-10 w-full bg-white border border-gray-300 rounded mt-1 max-h-60 overflow-y-auto shadow-lg">
          {filteredOptions.map(option => (
            <li
              key={option.id}
              onClick={() => handleOptionClick(option)}
              className="px-3 py-2 cursor-pointer hover:bg-blue-500 hover:text-white"
            >
              {option.name}
            </li>
          ))}
        </ul>
      )}
      {isOpen && filteredOptions.length === 0 && (
        <div className="absolute z-10 w-full bg-white border border-gray-300 rounded mt-1 p-2 text-gray-500 shadow-lg">
          No matches found
        </div>
      )}
    </div>
  );
};

export default SearchableDropdown;
