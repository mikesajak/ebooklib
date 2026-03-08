import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { FaBook, FaPlus, FaEdit, FaTrash } from 'react-icons/fa';
import Notification from './Notification';
import { useTranslation } from 'react-i18next';
import { useSearch } from './SearchContext';
import SearchBar from './SearchBar';
import ConfirmationDialog from './ConfirmationDialog';
import Pagination from './Pagination';
import { fetchWithCsrf } from './api';

const BookTable = () => {
  const { t, ready } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(() => Number(localStorage.getItem('bookListPageSize')) || 10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [sortField, setSortField] = useState('title'); // Default sort field
  const [sortDirection, setSortDirection] = useState('asc'); // Default sort direction
  const [notification, setNotification] = useState(null);
  const { searchQuery, refreshTrigger } = useSearch('books');
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [bookToDelete, setBookToDelete] = useState(null);

  const bookQueryTransformer = (input) => {
    const operatorRegex = /[=<>!();,]/;
    if (input && !operatorRegex.test(input)) {
      return `title=like="${input}"`;
    }
    return input;
  };

  const fetchBooks = useCallback(async () => {
    setLoading(true);
    setNotification(null);
    try {
      const endpoint = '/api/books/search';
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
        sort: `${sortField},${sortDirection}`,
      });
      if (searchQuery) {
        params.append('query', searchQuery);
      }
      const response = await fetchWithCsrf(`${endpoint}?${params.toString()}`);
      if (!response.ok) {
        throw new Error('Failed to fetch books');
      }
      const data = await response.json();
      setBooks(data.content || []);
      setTotalPages(data.page?.totalPages || 0);
      setTotalElements(data.page?.totalElements || 0);
    } catch (err) {
      setNotification({ message: `${t('bookTable.error')}: ${err.message}`, type: 'error' });
    } finally {
      setLoading(false);
    }
  }, [page, size, sortField, sortDirection, searchQuery, refreshTrigger, t]);

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
  }, [ready, fetchBooks, searchQuery, refreshTrigger]);

  // Reset page to 0 when search query changes
  useEffect(() => {
    setPage(0);
  }, [searchQuery]);

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
      const response = await fetchWithCsrf(`/api/books/${bookId}`, {
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
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-indigo-100 border-t-indigo-600"></div>
        <p className="text-sm font-black text-gray-400 uppercase tracking-widest">{t('common.loading')}</p>
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
    <div className="container mx-auto p-4 max-w-7xl">
      {notification && (
        <Notification
          message={notification.message}
          type={notification.type}
          onClose={() => setNotification(null)}
        />
      )}
      
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-6 gap-4">
        <div className="flex items-center gap-3">
          <div className="bg-indigo-600 text-white p-3 rounded-xl shadow-md">
            <FaBook className="text-xl" />
          </div>
          <div>
            <h1 className="text-2xl font-extrabold text-gray-900 tracking-tight">{t('bookTable.title')}</h1>
            <p className="text-xs text-gray-500 font-medium">{t('bookTable.booksCount', '{{count}} books in library', { count: totalElements })}</p>
          </div>
        </div>
        
        <Link to="/books/add">
          <button className="bg-indigo-600 hover:bg-indigo-700 text-white font-black text-sm uppercase tracking-wider py-3 px-8 rounded-2xl shadow-xl shadow-indigo-100 transition-all flex items-center gap-2 transform hover:-translate-y-1 active:scale-95">
            <FaPlus />
            {t('bookTable.addBookButton')}
          </button>
        </Link>
      </div>

      <div className="w-full mb-6">
        <SearchBar scope="books" queryTransformer={bookQueryTransformer} />
      </div>
      
      <div className="bg-white shadow-xl rounded-2xl overflow-hidden border border-gray-100">
        <div className="overflow-x-auto">
          <table className="min-w-full table-auto">
            <thead className="bg-gray-50/50 border-b border-gray-200">
              <tr className="text-gray-500 uppercase text-[10px] font-black tracking-widest">
                <th className="py-4 px-6 text-left cursor-pointer hover:text-indigo-600 transition-colors" onClick={() => handleSort('title')}>
                  {t('bookTable.header.title')}{getSortIndicator('title')}
                </th>
                <th className="py-4 px-6 text-left">{t('bookTable.header.authors')}</th>
                <th className="py-4 px-6 text-left cursor-pointer hover:text-indigo-600 transition-colors" onClick={() => handleSort('series.title')}>
                  {t('bookTable.header.series')}{getSortIndicator('series.title')}
                </th>
                <th className="py-4 px-6 text-left cursor-pointer hover:text-indigo-600 transition-colors" onClick={() => handleSort('volume')}>
                  {t('bookTable.header.volume')}{getSortIndicator('volume')}
                </th>
                <th className="py-4 px-6 text-left cursor-pointer hover:text-indigo-600 transition-colors" onClick={() => handleSort('publicationDate')}>
                  {t('bookTable.header.publicationDate', 'Publication Date')}{getSortIndicator('publicationDate')}
                </th>
                <th className="py-4 px-6 text-left cursor-pointer hover:text-indigo-600 transition-colors" onClick={() => handleSort('publisher')}>
                  {t('bookTable.header.publisher', 'Publisher')}{getSortIndicator('publisher')}
                </th>
                <th className="py-4 px-6 text-left">{t('bookTable.header.labels')}</th>
                <th className="py-4 px-6 text-center">{t('bookTable.header.actions')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 bg-white">
              {loading ? (
                <tr>
                  <td colSpan="8" className="px-6 py-12 whitespace-nowrap text-center text-sm text-gray-400 italic">
                    <div className="flex flex-col items-center gap-2">
                      <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-indigo-600"></div>
                      {t('bookTable.loadingBooks')}
                    </div>
                  </td>
                </tr>
              ) : books.length === 0 ? (
                <tr>
                  <td colSpan="8" className="px-6 py-12 whitespace-nowrap text-center text-sm text-gray-400 italic">{t('bookTable.noBooksFound')}</td>
                </tr>
              ) : (
                books.map((book) => (
                  <tr key={book.id} className="group hover:bg-indigo-50/30 transition-all duration-200">
                    <td className="py-4 px-6 text-left">
                      <Link to={`/book/${book.id}`} className="book-link group-hover:text-indigo-700">
                        {book.title}
                      </Link>
                    </td>
                    <td className="py-4 px-6 text-left whitespace-nowrap">
                      {book.authors.map((author) => (
                        <div key={author.id} className="text-xs mb-1 last:mb-0">
                          <Link to={`/author/${author.id}`} className="author-link hover:underline">
                            {author.firstName} {author.lastName}
                          </Link>
                        </div>
                      ))}
                    </td>
                    <td className="py-4 px-6 text-left whitespace-nowrap text-sm text-gray-600">
                      {book.series ? (
                        <Link to={`/series/${book.series.id}`} className="series-link hover:underline">
                          {book.series.title}
                        </Link>
                      ) : <span className="text-gray-300 italic">{t('common.na')}</span>}
                    </td>
                    <td className="py-4 px-6 text-left text-sm text-gray-600">{book.volume || <span className="text-gray-300">−</span>}</td>
                    <td className="py-4 px-6 text-left text-sm text-gray-600 whitespace-nowrap">{book.publicationDate || <span className="text-gray-300">−</span>}</td>
                    <td className="py-4 px-6 text-left text-sm text-gray-600">{book.publisher || <span className="text-gray-300">−</span>}</td>
                    <td className="py-4 px-6 text-left">
                      <div className="flex flex-wrap gap-1">
                        {book.labels && book.labels.length > 0 ? book.labels.map(label => (
                          <span key={label} className="bg-gray-100 text-gray-600 text-[10px] font-bold px-2 py-0.5 rounded-full uppercase tracking-tighter border border-gray-200">
                            {label}
                          </span>
                        )) : <span className="text-gray-300 text-sm">−</span>}
                      </div>
                    </td>
                    <td className="py-4 px-6 text-center whitespace-nowrap">
                      <div className="flex justify-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                        <Link 
                          to={`/books/${book.id}/edit`} 
                          className="p-2.5 text-indigo-600 hover:bg-indigo-50 rounded-xl transition-all"
                          title={t('common.edit')}
                        >
                          <FaEdit size={16} />
                        </Link>
                        <button 
                          onClick={() => handleDeleteClick(book)} 
                          className="p-2.5 text-rose-600 hover:bg-rose-50 rounded-xl transition-all"
                          title={t('common.delete')}
                        >
                          <FaTrash size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

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

        <div className="bg-gray-50/50 border-t border-gray-100 p-4">
          <Pagination
            page={page}
            size={size}
            totalPages={totalPages}
            totalElements={totalElements}
            onPageChange={setPage}
            onPageSizeChange={(newSize) => {
              setSize(newSize);
              localStorage.setItem('bookListPageSize', newSize.toString());
              setPage(0); // Reset to first page when page size changes
            }}
            theme="indigo"
          />
        </div>
      </div>
    </div>
  );
};

export default BookTable;
