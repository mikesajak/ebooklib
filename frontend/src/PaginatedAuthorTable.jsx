import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import ConfirmationDialog from './ConfirmationDialog';
import Notification from './Notification';

const PaginatedAuthorTable = () => {
  const { t } = useTranslation();
  const [authors, setAuthors] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10); // Default page size
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [sortField, setSortField] = useState('lastName'); // Default sort field
  const [sortDirection, setSortDirection] = useState('asc'); // Default sort direction

  const [showConfirmDialog, setShowConfirmDialog] = useState(false);
  const [authorToDelete, setAuthorToDelete] = useState(null);
  const [affectedBooks, setAffectedBooks] = useState([]);
  const [notification, setNotification] = useState({ message: '', type: '', visible: false });

  const navigate = useNavigate();

  const fetchAuthors = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`/api/authors?page=${page}&size=${size}&sort=${sortField},${sortDirection}`);
      if (!response.ok) {
        throw new Error('Failed to fetch authors');
      }
      const data = await response.json();
      setAuthors(data.content || []);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [page, size, sortField, sortDirection]);

  useEffect(() => {
    fetchAuthors();
  }, [fetchAuthors]);

  const handlePreviousPage = () => {
    setPage(prevPage => Math.max(0, prevPage - 1));
  };
  const handleNextPage = () => {
    setPage(prevPage => Math.min(totalPages - 1, prevPage + 1));
  };

  const handlePageSizeChange = (event) => {
    setSize(Number(event.target.value));
    setPage(0); // Reset to first page when page size changes
  };

  const getSortIndicator = (field) => {
    if (sortField === field) {
      return sortDirection === 'asc' ? ' ▲' : ' ▼';
    }
    return '';
  };

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('asc'); // Default to ascending when changing sort field
    }
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
            <th className="py-3 px-6 text-left cursor-pointer" onClick={() => handleSort('firstName')}>{t('authorList.firstName')}{getSortIndicator('firstName')}</th>
            <th className="py-3 px-6 text-left cursor-pointer" onClick={() => handleSort('lastName')}>{t('authorList.lastName')}{getSortIndicator('lastName')}</th>
            <th className="py-3 px-6 text-center">{t('common.actions')}</th>
          </tr>
        </thead>
        <tbody className="text-gray-600 text-sm font-light">
          {authors.map((author) => (
            <tr key={author.id} className="border-b border-gray-200 hover:bg-gray-100">
              <td className="py-3 px-6 text-left whitespace-nowrap">
                <Link to={`/author/${author.id}`} className="author-link">
                  {author.firstName}
                </Link>
              </td>
              <td className="py-3 px-6 text-left">
                <Link to={`/author/${author.id}`} className="author-link">
                  {author.lastName}
                </Link>
              </td>

              <td className="py-3 px-6 text-center whitespace-nowrap text-sm font-medium">
                <Link to={`/authors/${author.id}/edit`} className="text-indigo-600 hover:text-indigo-900 mr-2">{t('common.edit')}</Link>
                <button onClick={() => openConfirmDialog(author)} className="text-red-600 hover:text-red-900">{t('common.delete')}</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="flex justify-between items-center p-4">
        <div>
          {t('common.page')} {page + 1} {t('common.of')} {totalPages} ({totalElements} {t('common.total')})
        </div>
        <div className="flex items-center space-x-2">
          <label htmlFor="pageSize" className="mr-2">{t('common.pageSize')}:</label>
          <select id="pageSize" value={size} onChange={handlePageSizeChange} className="border border-gray-300 rounded p-1">
            <option value="5">5</option>
            <option value="10">10</option>
            <option value="20">20</option>
            <option value="50">50</option>
          </select>
          <button
            onClick={handlePreviousPage}
            disabled={page === 0}
            className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded disabled:opacity-50"
          >
            {t('common.previous')}
          </button>
          <button
            onClick={handleNextPage}
            disabled={page === totalPages - 1}
            className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded disabled:opacity-50"
          >
            {t('common.next')}
          </button>
        </div>
      </div>

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