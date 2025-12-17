import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import Notification from './Notification';
import { useTranslation } from 'react-i18next';
import { useSearch } from './SearchContext';
import SearchBar from './SearchBar';
import ConfirmationDialog from './ConfirmationDialog';
import Pagination from './Pagination';

const BookTable = () => {
  const { t, i18n, ready } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [sortField, setSortField] = useState('title'); // Default sort field
  const [sortDirection, setSortDirection] = useState('asc'); // Default sort direction
  const [notification, setNotification] = useState(null);
  const { searchQuery } = useSearch();
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [bookToDelete, setBookToDelete] = useState(null);



  const fetchBooks = useCallback(async () => {
    setLoading(true);
    setNotification(null);
    try {
      const endpoint = searchQuery ? '/api/books/search' : '/api/books';
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
        sort: `${sortField},${sortDirection}`,
      });
      if (searchQuery) {
        params.append('query', searchQuery);
      }
      const response = await fetch(`${endpoint}?${params.toString()}`);
      if (!response.ok) {
        throw new Error('Failed to fetch books');
      }
      const data = await response.json();
      setBooks(data.content || []);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setNotification({ message: `${t('bookTable.error')}: ${err.message}`, type: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, size, sortField, sortDirection, searchQuery, t]);

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
  }, [ready, fetchBooks, searchQuery]);



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
        <h1 className="text-2xl font-bold">{t('bookTable.title')}</h1>
        <p className="text-center text-gray-500">{t('bookTable.loadingBooks')}</p>
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
          <div className="flex justify-between items-center mb-4">
            <h1 className="text-2xl font-bold">{t('bookTable.title')}</h1>
            <Link to="/books/add">
              <button className="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded">
                {t('bookTable.addBookButton')}
              </button>
            </Link>
          </div>
          <div className="w-full mb-4">
            <SearchBar />
          </div>
          <div className="bg-white shadow-md rounded">
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
          </div>
        </div>
  );
};

export default BookTable;
