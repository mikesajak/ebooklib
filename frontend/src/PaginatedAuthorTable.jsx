import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import ConfirmationDialog from './ConfirmationDialog';
import Notification from './Notification';
import Pagination from './Pagination';
import { useSearch } from './SearchContext';

const PaginatedAuthorTable = () => {
  const { t } = useTranslation();
  const [authors, setAuthors] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10); // Default page size
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [sortBy, setSortBy] = useState('lastNameAsc'); // Default sort
  const [isDropdownOpen, setDropdownOpen] = useState(false);

  const [showConfirmDialog, setShowConfirmDialog] = useState(false);
  const [authorToDelete, setAuthorToDelete] = useState(null);
  const [affectedBooks, setAffectedBooks] = useState([]);
  const [notification, setNotification] = useState({ message: '', type: '', visible: false });

  const navigate = useNavigate();
  const { searchQuery, refreshTrigger } = useSearch('authors');

  const sortOptions = {
    lastNameAsc: { label: t('authorList.sort.lastNameAsc'), params: 'sort=lastName,asc&sort=firstName,asc' },
    lastNameDesc: { label: t('authorList.sort.lastNameDesc'), params: 'sort=lastName,desc&sort=firstName,desc' },
    firstNameAsc: { label: t('authorList.sort.firstNameAsc'), params: 'sort=firstName,asc&sort=lastName,asc' },
    firstNameDesc: { label: t('authorList.sort.firstNameDesc'), params: 'sort=firstName,desc&sort=lastName,desc' },
    bookCountAsc: { label: t('authorList.sort.bookCountAsc'), params: 'sort=bookCount,asc' },
    bookCountDesc: { label: t('authorList.sort.bookCountDesc'), params: 'sort=bookCount,desc' },
  };

  const nameColumnSortOptions = ['lastNameAsc', 'lastNameDesc', 'firstNameAsc', 'firstNameDesc'];

  const fetchAuthors = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const endpoint = '/api/authors/search';
      const sortParams = sortOptions[sortBy].params;
      const params = new URLSearchParams(sortParams);
      params.append('page', page.toString());
      params.append('size', size.toString());
      if (searchQuery) {
        params.append('query', searchQuery);
      }

      const response = await fetch(`${endpoint}?${params.toString()}`);
      if (!response.ok) {
        throw new Error('Failed to fetch authors');
      }
      const data = await response.json();
      setAuthors(data.content || []);
      setTotalPages(data.page?.totalPages || 0);
      setTotalElements(data.page?.totalElements || 0);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [page, size, sortBy, searchQuery, refreshTrigger]);

  useEffect(() => {
    fetchAuthors();
  }, [fetchAuthors]);

  // Reset page to 0 when search query changes
  useEffect(() => {
    setPage(0);
  }, [searchQuery]);



  const handleSortChange = (sortKey) => {
    setSortBy(sortKey);
    setDropdownOpen(false);
  };

  const handleSimpleSort = (field) => {
    const newSortBy = sortBy === `${field}Asc` ? `${field}Desc` : `${field}Asc`;
    setSortBy(newSortBy);
  };

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

  if (loading) {
    return (
      <div className="text-center text-gray-500">{t('common.loading')}</div>
    );
  }

  if (error) {
    return (
      <div className="text-center text-red-500">{t('common.error')}: {error}</div>
    );
  }
  if (authors.length === 0) {
    return (
      <div className="text-center text-gray-500">{t('authorList.noAuthorsFound')}</div>
    );
  }

  return (
    <div className="bg-white shadow-md rounded my-6">
      <table className="min-w-full table-auto">
        <thead>
          <tr className="bg-gray-200 text-gray-600 uppercase text-sm leading-normal">
            <th className="py-3 px-6 text-left">
              <div className="relative inline-block text-left">
                <button
                  onClick={() => setDropdownOpen(!isDropdownOpen)}
                  className="inline-flex justify-center w-full rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none"
                >
                  {sortOptions[sortBy].label}
                  <span className="ml-2">{sortBy.includes('Asc') ? ' ▲' : ' ▼'}</span>
                </button>
                {isDropdownOpen && (
                  <div className="origin-top-left absolute left-0 mt-2 w-56 rounded-md shadow-lg bg-white ring-1 ring-black ring-opacity-5 z-20">
                    <div className="py-1" role="menu" aria-orientation="vertical" aria-labelledby="options-menu">
                      {nameColumnSortOptions.map(key => (
                        <button
                          key={key}
                          onClick={() => handleSortChange(key)}
                          className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                          role="menuitem"
                        >
                          {sortOptions[key].label}
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </th>
            <th className="py-3 px-6 text-left cursor-pointer" onClick={() => handleSimpleSort('bookCount')}>
              {t('authorList.header.bookCount')}
              {sortBy.startsWith('bookCount') ? (sortBy.endsWith('Asc') ? ' ▲' : ' ▼') : ''}
            </th>
            <th className="py-3 px-6 text-center">{t('common.actions')}</th>
          </tr>
        </thead>
        <tbody className="text-gray-600 text-sm font-light">
          {authors.map((author) => (
            <tr key={author.id} className="border-b border-gray-200 hover:bg-gray-100">
              <td className="py-3 px-6 text-left whitespace-nowrap">
                <Link to={`/author/${author.id}`} className="author-link">
                  {author.firstName} {author.lastName}
                </Link>
              </td>
              <td className="py-3 px-6 text-left">{author.bookCount}</td>
              <td className="py-3 px-6 text-center whitespace-nowrap text-sm font-medium">
                <Link to={`/authors/${author.id}/edit`} className="text-indigo-600 hover:text-indigo-900 mr-2">{t('common.edit')}</Link>
                <button onClick={() => openConfirmDialog(author)} className="text-red-600 hover:text-red-900">{t('common.delete')}</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <Pagination
        page={page}
        size={size}
        totalPages={totalPages}
        totalElements={totalElements}
        onPageChange={setPage}
        onPageSizeChange={(newSize) => {
          setSize(newSize);
          setPage(0); // Reset to first page when page size changes
        }}
      />

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

export default PaginatedAuthorTable;