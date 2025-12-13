import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import Notification from './Notification';
import { useTranslation } from 'react-i18next';
import ConfirmationDialog from './ConfirmationDialog';

const BookTable = () => {
  const { t, i18n, ready } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [sortField, setSortField] = useState('title'); // Default sort field
  const [sortDirection, setSortDirection] = useState('asc'); // Default sort direction
  const [notification, setNotification] = useState(null);
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [bookToDelete, setBookToDelete] = useState(null);



  const fetchBooks = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch(`/api/books?page=${page}&size=${size}&sort=${sortField},${sortDirection}`);
      if (!response.ok) {
        throw new Error('Failed to fetch books');
      }
      const data = await response.json();
      setBooks(data.content || []);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [page, size, sortField, sortDirection]);

  useEffect(() => {
    if (location.state && location.state.notification) {
      setNotification(location.state.notification);
      // Clear location state to prevent notification from re-appearing on refresh
      window.history.replaceState({}, document.title)
    }
  }, [location.state]);

  useEffect(() => {
    if (ready) {
      fetchBooks();
    }
  }, [ready, fetchBooks]);



  const handleDeleteClick = (book) => {
    setBookToDelete(book);
    setShowConfirmation(true);
  };

  const handleConfirmDelete = async () => {
    if (bookToDelete) {
      await deleteBook(bookToDelete.id);
      setShowConfirmation(false);
      setBookToDelete(null);
    }
  };

  const deleteBook = async (bookId) => {
    try {
      const response = await fetch(`/api/books/${bookId}`, {
        method: 'DELETE',
      });

      if (!response.ok) {
        throw new Error('Failed to delete book');
      }

      setNotification({ message: t('bookTable.deleteSuccess'), type: 'success' });
      // Refresh the book list by refetching
      fetchBooks();
    } catch (err) {
      setError(err.message);
      setNotification({ message: t('bookTable.deleteFailure'), type: 'error' });
    }
  };

  const handleCancelDelete = () => {
    setShowConfirmation(false);
    setBookToDelete(null);
  };



  if (!ready) {
    return <div>Loading translations...</div>;
  }

  if (loading) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">{t('bookTable.title')}</h1>
        <p className="text-center text-gray-500">{t('bookTable.loadingBooks')}</p>
      </div>
    );
  }  if (error) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">{t('bookTable.title')}</h1>
        <p className="text-center text-red-500">{t('bookTable.error')}: {error}</p>
      </div>
    );
  }

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('asc'); // Default to ascending when changing sort field
    }
  };

  const getSortIndicator = (field) => {
    if (sortField === field) {
      return sortDirection === 'asc' ? ' ▲' : ' ▼';
    }
    return '';
  };

  return (
        <div className="container mx-auto p-4">
          {notification && (
            <Notification
              message={notification.message}
              type={notification.type}
              onClose={() => setNotification(null)}
            />
          )}
          <div className="bg-white shadow-md rounded my-6">
            <div className="flex justify-between items-center mb-4">
              <h1 className="text-2xl font-bold">{t('bookTable.title')}</h1>
              <Link to="/books/add">
                <button className="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded">
                  {t('bookTable.addBookButton')}
                </button>
              </Link>
            </div>
            <table className="min-w-full table-auto">
              <thead>
                <tr className="bg-gray-200 text-gray-600 uppercase text-sm leading-normal">
                  <th className="py-3 px-6 text-left" onClick={() => handleSort('title')}>{t('bookTable.header.title')}{getSortIndicator('title')}</th>
                  <th className="py-3 px-6 text-left">{t('bookTable.header.authors')}</th>
                  <th className="py-3 px-6 text-left" onClick={() => handleSort('series.title')}>{t('bookTable.header.series')}{getSortIndicator('series.title')}</th>
                  <th className="py-3 px-6 text-left" onClick={() => handleSort('volume')}>{t('bookTable.header.volume')}{getSortIndicator('volume')}</th>
                  <th className="py-3 px-6 text-left" onClick={() => handleSort('publicationDate')}>Publication Date{getSortIndicator('publicationDate')}</th>
                  <th className="py-3 px-6 text-left" onClick={() => handleSort('publisher')}>Publisher{getSortIndicator('publisher')}</th>
                  <th className="py-3 px-6 text-left">{t('bookTable.header.labels')}</th>
                  <th className="py-3 px-6 text-center">{t('bookTable.header.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {books.length === 0 ? (
                  <tr>
                                  <td colSpan="8" className="px-6 py-4 whitespace-nowrap text-center text-sm text-gray-500">{t('bookTable.noBooksFound')}</td>
                  </tr>
                ) : (
                  books.map((book, index) => (
                    <tr key={book.id} className="border-b border-gray-200 hover:bg-gray-100">
                      <td className="py-3 px-6 text-left whitespace-nowrap">
                        <Link
                          to={`/book/${book.id}`}
                          className="book-link"
                        >
                          {book.title}
                        </Link>
                      </td>
                      <td className="py-3 px-6 text-left whitespace-nowrap">
                        {book.authors.map((author) => (
                          <div key={author.id} style={{marginBottom: '0.25rem'}}>
                            ▪ <Link
                              to={`/author/${author.id}`}
                              className="author-link"
                            >
                              {author.firstName} {author.lastName}
                            </Link>
                          </div>
                        ))}
                      </td>
                      <td className="py-3 px-6 text-left whitespace-nowrap">
                        {book.series ? (
                          <Link
                            to={`/series/${book.series.id}`}
                            className="series-link"
                          >
                            {book.series.title}
                          </Link>
                        ) : t('common.na')}
                      </td>
                      <td className="py-3 px-6 text-left whitespace-nowrap">{book.volume || t('common.na')}</td>
                      <td className="py-3 px-6 text-left whitespace-nowrap">{book.publicationDate || t('common.na')}</td>
                      <td className="py-3 px-6 text-left whitespace-nowrap">{book.publisher || t('common.na')}</td>
                      <td className="py-3 px-6 text-left whitespace-nowrap">{book.labels && book.labels.length > 0 ? book.labels.join(', ') : t('common.na')}</td>
                      <td className="py-3 px-6 text-center whitespace-nowrap text-sm font-medium">
                        <Link to={`/books/${book.id}/edit`} className="text-indigo-600 hover:text-indigo-900 mr-2">{t('common.edit')}</Link>
                        <button onClick={() => handleDeleteClick(book)} className="text-red-600 hover:text-red-900">{t('common.delete')}</button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
    
            {showConfirmation && (
              <ConfirmationDialog
                title={t('bookTable.deleteConfirmation.title')}
                message={t('bookTable.deleteConfirmation.message', { bookTitle: bookToDelete?.title })}
                onCancel={handleCancelDelete}
                onConfirm={handleConfirmDelete}
                confirmButtonText={t('bookTable.deleteConfirmation.delete')}
                cancelButtonText={t('bookTable.deleteConfirmation.cancel')}
              />
            )}
    
            <div className="flex justify-between items-center p-4">
              <div>
                {t('common.page')} {page + 1} {t('common.of')} {totalPages} ({totalElements} {t('common.total')})
              </div>

              <div className="flex items-center space-x-2">
                <label htmlFor="pageSize" className="mr-2">{t('common.pageSize')}:</label>
                <select id="pageSize" value={size} onChange={(e) => { setSize(Number(e.target.value)); setPage(0); }} className="border border-gray-300 rounded p-1">
                  <option value="5">5</option>
                  <option value="10">10</option>
                  <option value="20">20</option>
                  <option value="50">50</option>
                </select>

                <button
                  onClick={() => setPage(page - 1)}
                  disabled={page === 0}
                  className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded disabled:opacity-50">
                  {t('common.previous')}
                </button>

                <button
                  onClick={() => setPage(page + 1)}
                  disabled={page === totalPages - 1}
                  className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded disabled:opacity-50">
                  {t('common.next')}
                </button>
              </div>
            </div>
          </div>
        </div>
  );
};

export default BookTable;
