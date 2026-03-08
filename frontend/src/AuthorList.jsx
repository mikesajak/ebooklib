import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FaUserTag, FaPlus, FaList, FaLayerGroup } from 'react-icons/fa';
import { groupBy } from './grouping-utils';
import PaginatedAuthorTable from './PaginatedAuthorTable';
import ConfirmationDialog from './ConfirmationDialog';
import Notification from './Notification';
import AuthorGroupTable from './AuthorGroupTable';
import { useSearch } from './SearchContext';
import SearchBar from './SearchBar';
import { fetchWithCsrf } from './api';

const AuthorList = () => {
  const { t } = useTranslation();
  const [authors, setAuthors] = useState([]);
  const [expandedLetters, setExpandedLetters] = useState({});
  const [groupingCriteria, setGroupingCriteria] = useState('lastName'); // Default to last name
  const [viewMode, setViewMode] = useState(() => localStorage.getItem('authorListViewMode') || 'grouped'); // 'grouped' or 'plain'

  const [showConfirmDialog, setShowConfirmDialog] = useState(false);
  const [authorToDelete, setAuthorToDelete] = useState(null);
  const [affectedBooks, setAffectedBooks] = useState([]);
  const [notification, setNotification] = useState({ message: '', type: '', visible: false });

  const navigate = useNavigate();
  const { searchQuery, refreshTrigger } = useSearch('authors');

  const authorQueryTransformer = (input) => {
    const operatorRegex = /[=<>!();,]/;
    if (input && !operatorRegex.test(input)) {
      return `name=like="${input}"`;
    }
    return input;
  };

  useEffect(() => {
    localStorage.setItem('authorListViewMode', viewMode);
  }, [viewMode]);

  useEffect(() => {
    if (searchQuery) {
      const grouped = groupBy(authors, groupingCriteria);
      const sortedKeys = Object.keys(grouped).sort();
      if (sortedKeys.length > 0) {
        setExpandedLetters({ [sortedKeys[0]]: true });
      } else {
        setExpandedLetters({});
      }
    }
  }, [searchQuery, authors, groupingCriteria]);

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
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchAuthors = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const endpoint = '/api/authors/search';
      const params = new URLSearchParams({
        page: '0',
        size: '500', // Fetch more for grouped view
      });
      if (searchQuery) {
        params.append('query', searchQuery);
      }

      const response = await fetchWithCsrf(`${endpoint}?${params.toString()}`);
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
  }, [searchQuery, refreshTrigger]);

  useEffect(() => {
    fetchAuthors();
  }, [fetchAuthors]);

  const openConfirmDialog = async (author) => {
    setAuthorToDelete(author);
    try {
      const response = await fetchWithCsrf(`/api/authors/${author.id}/books`);
      if (!response.ok) {
        throw new Error('Failed to fetch affected books');
      }
      const data = await response.json();
      setAffectedBooks(data);
    } catch (error) {
      console.error('Error fetching affected books:', error);
      setAffectedBooks([]);
      setNotification({ message: t('authorList.errorFetchingBooks'), type: 'error', visible: true });
    }
    setShowConfirmDialog(true);
  };

  const closeConfirmDialog = () => {
    setShowConfirmDialog(false);
    setAuthorToDelete(null);
    setAffectedBooks([]);
  };

  const handleDeleteConfirmed = async () => {
    if (authorToDelete) {
      try {
        const response = await fetchWithCsrf(`/api/authors/${authorToDelete.id}`, {
          method: 'DELETE',
        });
        if (!response.ok) {
          throw new Error('Failed to delete author');
        }
        setNotification({ message: t('authorList.authorDeleted'), type: 'success', visible: true });
        closeConfirmDialog();
        fetchAuthors(); // Refresh the author list
      } catch (error) {
        console.error('Error deleting author:', error);
        setNotification({ message: `${t('authorList.errorDeletingAuthor')}: ${error.message}`, type: 'error', visible: true });
        closeConfirmDialog();
      }
    }
  };

  const handleNotificationClose = () => {
    setNotification(prev => ({ ...prev, visible: false }));
  };

  const groupedAuthors = groupBy(authors, groupingCriteria);

  return (
    <div className="container mx-auto p-4 max-w-7xl">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-6 gap-4">
        <div className="flex items-center gap-3">
          <div className="bg-emerald-600 text-white p-3 rounded-xl shadow-md">
            <FaUserTag className="text-xl" />
          </div>
          <div>
            <h1 className="text-2xl font-extrabold text-gray-900 tracking-tight">{t('authorList.title')}</h1>
            <p className="text-xs text-gray-500 font-medium">{authors.length} authors in database</p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {viewMode === 'grouped' && (
            <div className="flex items-center bg-white border border-gray-200 rounded-xl px-3 py-1 shadow-sm">
              <label htmlFor="grouping-criteria" className="text-[10px] font-bold text-gray-400 uppercase mr-2 tracking-tighter">{t('authorList.groupBy')}:</label>
              <select 
                id="grouping-criteria" 
                value={groupingCriteria} 
                onChange={handleGroupingChange} 
                className="bg-transparent text-sm font-bold text-gray-700 focus:outline-none py-1"
              >
                <option value="lastName">{t('authorList.lastName')}</option>
                <option value="firstName">{t('authorList.firstName')}</option>
              </select>
            </div>
          )}
          
          <div className="bg-white border border-gray-200 rounded-xl p-1 shadow-sm flex items-center">
            <button
              onClick={() => setViewMode('grouped')}
              className={`p-2 rounded-lg transition-all flex items-center gap-2 text-xs font-black uppercase tracking-widest ${viewMode === 'grouped' ? 'bg-emerald-600 text-white shadow-inner' : 'text-gray-400 hover:text-gray-600'}`}
              title={t('authorList.groupedView')}
            >
              <FaLayerGroup />
              <span className="hidden sm:inline">{t('authorList.groupedView', 'Grouped')}</span>
            </button>
            <button
              onClick={() => setViewMode('plain')}
              className={`p-2 rounded-lg transition-all flex items-center gap-2 text-xs font-black uppercase tracking-widest ${viewMode === 'plain' ? 'bg-emerald-600 text-white shadow-inner' : 'text-gray-400 hover:text-gray-600'}`}
              title={t('authorList.plainView')}
            >
              <FaList />
              <span className="hidden sm:inline">{t('authorList.plainView', 'Plain')}</span>
            </button>
          </div>

          <Link to="/authors/add">
            <button className="bg-emerald-600 hover:bg-emerald-700 text-white font-black text-sm uppercase tracking-wider py-3 px-8 rounded-2xl shadow-xl shadow-emerald-100 transition-all flex items-center gap-2 transform hover:-translate-y-1 active:scale-95">
              <FaPlus />
              {t('authorList.addAuthor')}
            </button>
          </Link>
        </div>
      </div>

      <div className="w-full mb-6">
        <SearchBar scope="authors" queryTransformer={authorQueryTransformer} />
      </div>

      {loading ? (
        <div className="py-12 flex flex-col items-center gap-2 text-gray-400 italic">
          <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-emerald-600"></div>
          {t('authorList.loading')}
        </div>
      ) : authors.length === 0 ? (
        <div className="py-12 text-center text-gray-400 italic bg-white rounded-2xl border border-dashed border-gray-200 shadow-inner">
          {t('authorList.noAuthorsFound')}
        </div>
      ) : viewMode === 'grouped' ? (
        <div className="space-y-4">
          {Object.keys(groupedAuthors).sort().map((letter) => (
              <div key={letter} className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                <h2
                    className={`text-lg font-black cursor-pointer px-6 py-3 flex justify-between items-center transition-colors ${expandedLetters[letter] ? 'bg-emerald-50 text-emerald-900' : 'bg-white text-gray-700 hover:bg-gray-50'}`}
                    onClick={() => toggleLetterExpansion(letter)}
                >
                  <div className="flex items-center gap-3">
                    <span className="text-xl font-black">{letter}</span>
                    <span className="text-xs bg-white text-gray-400 px-2 py-0.5 rounded-full border border-gray-100 font-bold">
                      {groupedAuthors[letter].length}
                    </span>
                  </div>
                  <div className={`transition-transform duration-300 ${expandedLetters[letter] ? 'rotate-180' : ''}`}>
                    <svg className="w-5 h-5 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M19 9l-7 7-7-7" /></svg>
                  </div>
                </h2>
                {expandedLetters[letter] && (
                    <div className="p-2 animate-fade-in">
                      <AuthorGroupTable authors={groupedAuthors[letter]} openConfirmDialog={openConfirmDialog} />
                    </div>
                )}
              </div>
          ))}
        </div>
      ) : (
        <div className="bg-white shadow-xl rounded-2xl overflow-hidden border border-gray-100">
          <PaginatedAuthorTable />
        </div>
      )}

      {showConfirmDialog && authorToDelete && (
        <ConfirmationDialog
          title={t('authorList.confirmDeleteTitle')}
          message={
            <div className="text-sm">
              <p className="text-gray-600 mb-4">{t('authorList.confirmDeleteMessage', { authorName: `${authorToDelete.firstName} ${authorToDelete.lastName}` })}</p>
              {affectedBooks.length > 0 && (
                <div className="bg-red-50 p-4 rounded-xl border border-red-100">
                  <p className="font-extrabold text-red-800 uppercase text-[10px] tracking-widest mb-2">{t('authorList.affectedBooks')}:</p>
                  <ul className="space-y-1">
                    {affectedBooks.map(book => (
                      <li key={book.id} className="text-red-700 flex items-center gap-2">
                        <span className="text-[10px]">▪</span> {book.title}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
              {affectedBooks.length === 0 && (
                <p className="bg-emerald-50 text-emerald-700 p-3 rounded-xl border border-emerald-100 italic">{t('authorList.noAffectedBooks')}</p>
              )}
            </div>
          }
          onConfirm={handleDeleteConfirmed}
          onCancel={closeConfirmDialog}
          confirmButtonText={t('common.delete')}
          cancelButtonText={t('common.cancel')}
        />
      )}

      {notification.visible && (
        <Notification
          message={notification.message}
          type={notification.type}
          onClose={handleNotificationClose}
        />
      )}
    </div>
  );
};

export default AuthorList;
