import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { groupBy } from './grouping-utils';

const AuthorList = () => {
  const { t } = useTranslation();
  const [authors, setAuthors] = useState([]);
  const [expandedLetters, setExpandedLetters] = useState({});
  const [groupingCriteria, setGroupingCriteria] = useState('lastName'); // Default to last name

  const toggleLetterExpansion = (letter) => {
    setExpandedLetters(prev => ({
      ...prev,
      [letter]: !prev[letter]
    }));
  };

  const handleGroupingChange = (event) => {
    setGroupingCriteria(event.target.value);
    setExpandedLetters({}); // Collapse all groups when criteria changes
  };
  const groupedAuthors = groupBy(authors, groupingCriteria);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const AUTHOR_DISPLAY_THRESHOLD = 25; // Define the threshold for switching between flat list and tree view

  useEffect(() => {
    const fetchAuthors = async () => {
      try {
        const response = await fetch('/api/authors?page=0&size=100');
        if (!response.ok) {
          throw new Error('Failed to fetch authors');
        }
        const data = await response.json();
        const sortedAuthors = (data.content || []).sort((a, b) => {
          if (a.lastName < b.lastName) return -1;
          if (a.lastName > b.lastName) return 1;
          if (a.firstName < b.firstName) return -1;
          if (a.firstName > b.firstName) return 1;
          return 0;
        });
        setAuthors(sortedAuthors);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchAuthors();
  }, []);

  if (loading) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">{t('authorList.title')}</h1>
        <p className="text-center text-gray-500">{t('authorList.loading')}</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">{t('authorList.title')}</h1>
        <p className="text-center text-red-500">{t('common.error')}: {error}</p>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4" data-testid="author-list-container">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold">{t('authorList.title')}</h1>
        <div>
          <label htmlFor="grouping-criteria" className="mr-2">{t('authorList.groupBy')}:</label>
          <select id="grouping-criteria" value={groupingCriteria} onChange={handleGroupingChange} className="border border-gray-300 rounded p-1" data-testid="grouping-criteria-select">
            <option value="lastName">{t('authorList.lastName')}</option>
            <option value="firstName">{t('authorList.firstName')}</option>
            <option value="nationality">{t('authorList.nationality')}</option>
          </select>
        </div>
      </div>
      {authors.length === 0 ? (
        <p className="text-gray-500">{t('authorList.noAuthorsFound')}</p>
      ) : authors.length <= AUTHOR_DISPLAY_THRESHOLD ? (
        <ul className="list-disc list-inside bg-white border border-gray-300 rounded p-4 shadow">
          {authors.map((author) => (
            <li key={author.id} className="mb-2">
              <Link to={`/author/${author.id}`} className="author-link">
                {author.firstName} {author.lastName}
              </Link>
            </li>
          ))}
        </ul>
      ) : (
        <div>
          {/* Alphabetic tree will be rendered here */}
        <div>
          {Object.keys(groupedAuthors).sort().map((letter) => (
            <div key={letter} className="mb-2">
              <h2
                className="text-xl font-semibold cursor-pointer bg-gray-200 p-2 rounded flex justify-between items-center"
                onClick={() => toggleLetterExpansion(letter)}
              >
                {letter} <span className="text-sm text-gray-500">({groupedAuthors[letter].length})</span>
                <span>{expandedLetters[letter] ? '-' : '+'}</span>
              </h2>
              {expandedLetters[letter] && (
                <ul className="list-disc list-inside bg-white border border-gray-300 rounded p-4 shadow mt-2">
                  {groupedAuthors[letter].map((author) => (
                    <li key={author.id} className="mb-1">
                      <Link to={`/author/${author.id}`} className="author-link">
                        {author.firstName} {author.lastName}
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </div>
        </div>
      )}
    </div>
  );
};

export default AuthorList;