import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { groupBy } from './grouping-utils';
import PaginatedAuthorTable from './PaginatedAuthorTable';
import ConfirmationDialog from './ConfirmationDialog';
import Notification from './Notification';

const AUTHOR_DISPLAY_THRESHOLD = 20;

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

  useEffect(() => {
    localStorage.setItem('authorListViewMode', viewMode);
  }, [viewMode]);

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
  }, []);

  useEffect(() => {
    fetchAuthors();
  }, [fetchAuthors]);

  const openConfirmDialog = async (author) => {
    setAuthorToDelete(author);
    try {
      const response = await fetch(`/api/authors/${author.id}/books`);
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
        const response = await fetch(`/api/authors/${authorToDelete.id}`, {
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
        <div className="flex items-center space-x-4">
          {viewMode === 'grouped' && ( // Only show grouping controls in grouped view
            <div>
              <label htmlFor="grouping-criteria" className="mr-2">{t('authorList.groupBy')}:</label>
              <select id="grouping-criteria" value={groupingCriteria} onChange={handleGroupingChange} className="border border-gray-300 rounded p-1" data-testid="grouping-criteria-select">
                <option value="lastName">{t('authorList.lastName')}</option>
                <option value="firstName">{t('authorList.firstName')}</option>
              </select>
            </div>
          )}
          <div>
            <button
              onClick={() => setViewMode('grouped')}
              className={`py-2 px-4 rounded-l ${viewMode === 'grouped' ? 'bg-gray-500 text-white' : 'bg-gray-200 text-gray-800'}`}
            >
              {t('authorList.groupedView')}
            </button>
            <button
              onClick={() => setViewMode('plain')}
              className={`py-2 px-4 rounded-r ${viewMode === 'plain' ? 'bg-gray-500 text-white' : 'bg-gray-200 text-gray-800'}`}
            >
              {t('authorList.plainView')}
            </button>
          </div>
          <Link to="/authors/add">
            <button className="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded">
              {t('authorList.addAuthor')}
            </button>
          </Link>
        </div>
      </div>
      {authors.length === 0 ? (
        <p className="text-gray-500">{t('authorList.noAuthorsFound')}</p>
      ) : viewMode === 'grouped' ? (
        authors.length <= AUTHOR_DISPLAY_THRESHOLD ? (
          <ul className="list-disc list-inside bg-white border border-gray-300 rounded p-4 shadow">
            {authors.map((author) => (
              <li key={author.id} className="mb-2 flex justify-between items-center">
                <Link to={`/author/${author.id}`} className="author-link">
                  {author.firstName} {author.lastName} ({author.bookCount})
                </Link>
                <div>
                  <Link to={`/authors/${author.id}/edit`} className="text-indigo-600 hover:text-indigo-900 mr-2">{t('common.edit')}</Link>
                  <button onClick={() => openConfirmDialog(author)} className="text-red-600 hover:text-red-900">{t('common.delete')}</button>
                </div>
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
                        <li key={author.id} className="mb-1 flex justify-between items-center">
                          <Link to={`/author/${author.id}`} className="author-link">
                            {author.firstName} {author.lastName} ({author.bookCount})
                          </Link>
                          <div>
                            <Link to={`/authors/${author.id}/edit`} className="text-indigo-600 hover:text-indigo-900 mr-2">{t('common.edit')}</Link>
                            <button onClick={() => openConfirmDialog(author)} className="text-red-600 hover:text-red-900">{t('common.delete')}</button>
                          </div>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              ))}
            </div>
          </div>
        )
      ) : (
        <PaginatedAuthorTable />
      )}

      {showConfirmDialog && authorToDelete && (
        <ConfirmationDialog
          title={t('authorList.confirmDeleteTitle')}
          message={
            <div>
              <p>{t('authorList.confirmDeleteMessage', { authorName: `${authorToDelete.firstName} ${authorToDelete.lastName}` })}</p>
              {affectedBooks.length > 0 && (
                <div className="mt-2">
                  <p className="font-semibold">{t('authorList.affectedBooks')}:</p>
                  <ul className="list-disc list-inside">
                    {affectedBooks.map(book => (
                      <li key={book.id}>{book.title}</li>
                    ))}
                  </ul>
                </div>
              )}
              {affectedBooks.length === 0 && (
                <p className="mt-2">{t('authorList.noAffectedBooks')}</p>
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