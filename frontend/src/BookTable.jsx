import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { FaBook, FaPlus, FaEdit, FaTrash, FaEllipsisV } from 'react-icons/fa';
import Notification from './Notification';
import { useTranslation } from 'react-i18next';
import { useSearch } from './SearchContext';
import SearchBar from './SearchBar';
import ConfirmationDialog from './ConfirmationDialog';
import Pagination from './Pagination';
import { fetchWithCsrf } from './api';
const getFormatBadgeStyle = (formatType) => {
  const fmt = (formatType || '').toUpperCase();
  switch (fmt) {
    case 'EPUB':
      return 'bg-emerald-100 text-emerald-800 border-emerald-200';
    case 'PDF':
      return 'bg-rose-100 text-rose-800 border-rose-200';
    case 'MOBI':
    case 'AZW3':
    case 'AZW':
      return 'bg-amber-100 text-amber-800 border-amber-200';
    case 'CBZ':
    case 'CBR':
      return 'bg-purple-100 text-purple-800 border-purple-200';
    case 'MP3':
    case 'M4B':
      return 'bg-sky-100 text-sky-800 border-sky-200';
    default:
      return 'bg-indigo-100 text-indigo-800 border-indigo-200';
  }
};

const formatBytes = (bytes) => {
  if (!bytes || bytes === 0) return '';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
};

const BookTable = () => {
  const { t, ready } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const tableContainerRef = useRef(null);
  const [containerWidth, setContainerWidth] = useState(1200);
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
  const [activeActionMenuId, setActiveActionMenuId] = useState(null);

  useEffect(() => {
    const handleOutsideClick = (e) => {
      if (!e.target.closest('.action-menu-container')) {
        setActiveActionMenuId(null);
      }
    };
    document.addEventListener('click', handleOutsideClick);
    return () => document.removeEventListener('click', handleOutsideClick);
  }, []);

  useEffect(() => {
    if (!tableContainerRef.current) return;
    const observer = new ResizeObserver((entries) => {
      for (let entry of entries) {
        if (entry.contentRect) {
          setContainerWidth(entry.contentRect.width);
        }
      }
    });
    observer.observe(tableContainerRef.current);
    return () => observer.disconnect();
  }, []);

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
    <div className="container mx-auto p-4 max-w-[1380px]">
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
            <p className="text-xs text-gray-500 font-medium">{t('bookTable.booksCount', { count: totalElements })}</p>
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
      
      <div ref={tableContainerRef} className="bg-white shadow-xl rounded-2xl overflow-hidden border border-gray-100">
        <div className="overflow-x-auto">
          {(() => {
            const showVolumeColumn = containerWidth >= 1300;     // Priority 1: Volume merges into Series
            const showPublisherColumn = containerWidth >= 1200;   // Priority 2: Publisher folds into Title sub-line
            const showPubDateColumn = containerWidth >= 1050;     // Priority 3: Pub Date folds into Title sub-line
            const showLabelsColumn = containerWidth >= 940;       // Priority 4: Labels hide
            const showFormatsColumn = containerWidth >= 750;      // Priority 5: Formats fold into Title sub-line
            const visibleColCount = 4 + (showVolumeColumn ? 1 : 0) + (showPubDateColumn ? 1 : 0) + (showPublisherColumn ? 1 : 0) + (showLabelsColumn ? 1 : 0) + (showFormatsColumn ? 1 : 0);

            return (
              <table className="w-full table-auto text-sm">
                <thead className="bg-gray-50/80 border-b border-gray-200">
                  <tr className="text-gray-500 uppercase text-[10px] font-black tracking-widest">
                    <th className="py-3 px-3 text-left cursor-pointer hover:text-indigo-600 transition-colors w-1/4 max-w-[260px]" onClick={() => handleSort('title')}>
                      {t('bookTable.header.title')}{getSortIndicator('title')}
                    </th>
                    <th className="py-3 px-3 text-left w-1/6 max-w-[180px]">{t('bookTable.header.authors')}</th>
                    <th className="py-3 px-3 text-left cursor-pointer hover:text-indigo-600 transition-colors w-1/6 max-w-[180px]" onClick={() => handleSort('series.title')}>
                      {t('bookTable.header.series')}{getSortIndicator('series.title')}
                    </th>
                    {showVolumeColumn && (
                      <th className="py-3 px-3 text-left cursor-pointer hover:text-indigo-600 transition-colors w-16" onClick={() => handleSort('volume')}>
                        {t('bookTable.header.volume')}{getSortIndicator('volume')}
                      </th>
                    )}
                    {showPubDateColumn && (
                      <th className="py-3 px-3 text-left cursor-pointer hover:text-indigo-600 transition-colors w-28" onClick={() => handleSort('publicationDate')}>
                        {t('bookTable.header.publicationDate', 'Publication Date')}{getSortIndicator('publicationDate')}
                      </th>
                    )}
                    {showPublisherColumn && (
                      <th className="py-3 px-3 text-left cursor-pointer hover:text-indigo-600 transition-colors w-32" onClick={() => handleSort('publisher')}>
                        {t('bookTable.header.publisher', 'Publisher')}{getSortIndicator('publisher')}
                      </th>
                    )}
                    {showLabelsColumn && <th className="py-3 px-3 text-left w-28">{t('bookTable.header.labels')}</th>}
                    {showFormatsColumn && <th className="py-3 px-3 text-left w-24">{t('bookTable.header.formats', 'Formats')}</th>}
                    <th className="py-3 px-2 text-center w-10 whitespace-nowrap"></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 bg-white">
                  {loading ? (
                    <tr>
                      <td colSpan={visibleColCount} className="px-4 py-12 whitespace-nowrap text-center text-sm text-gray-400 italic">
                        <div className="flex flex-col items-center gap-2">
                          <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-indigo-600"></div>
                          {t('bookTable.loadingBooks')}
                        </div>
                      </td>
                    </tr>
                  ) : books.length === 0 ? (
                    <tr>
                      <td colSpan={visibleColCount} className="px-4 py-12 whitespace-nowrap text-center text-sm text-gray-400 italic">{t('bookTable.noBooksFound')}</td>
                    </tr>
                  ) : (
                    books.map((book) => (
                      <tr key={book.id} className="group hover:bg-indigo-50/30 transition-all duration-200">
                        <td className="py-3 px-3 text-left max-w-[260px]">
                          <Link to={`/book/${book.id}`} className="book-link font-bold text-gray-900 group-hover:text-indigo-700 block truncate" title={book.title}>
                            {book.title}
                          </Link>
                          <div className="text-[11px] text-gray-400 font-medium flex flex-wrap gap-x-2 gap-y-0.5 items-center mt-0.5">
                            {!showPublisherColumn && book.publisher && <span>{book.publisher}</span>}
                            {!showPubDateColumn && book.publicationDate && <span>{!showPublisherColumn && book.publisher ? '· ' : ''}{book.publicationDate}</span>}
                          </div>
                          {!showFormatsColumn && book.formats && book.formats.length > 0 && (
                            <div className="flex flex-wrap gap-1 mt-1">
                              {book.formats.map((fmt) => (
                                <span
                                  key={fmt.id || fmt.formatType}
                                  className={`${getFormatBadgeStyle(fmt.formatType)} text-[9px] font-extrabold px-1.5 py-0.5 rounded-full uppercase tracking-tight border shadow-xs`}
                                  title={fmt.fileName ? `${fmt.fileName}${fmt.size ? ` (${formatBytes(fmt.size)})` : ''}` : formatBytes(fmt.size)}
                                >
                                  {fmt.formatType}
                                </span>
                              ))}
                            </div>
                          )}
                        </td>
                        <td className="py-3 px-3 text-left max-w-[180px] truncate">
                          {book.authors.map((author) => (
                            <div key={author.id} className="text-xs mb-0.5 last:mb-0 truncate" title={`${author.firstName} ${author.lastName}`}>
                              <Link to={`/author/${author.id}`} className="author-link hover:underline">
                                {author.firstName} {author.lastName}
                              </Link>
                            </div>
                          ))}
                        </td>
                        <td className="py-3 px-3 text-left max-w-[180px] truncate text-sm text-gray-600">
                          {book.series ? (
                            <Link to={`/series/${book.series.id}`} className="series-link hover:underline truncate block" title={book.volume ? `${book.series.title} (#${book.volume})` : book.series.title}>
                              {book.series.title}
                              {!showVolumeColumn && book.volume && <span className="font-medium text-gray-400 ml-1">(#{book.volume})</span>}
                            </Link>
                          ) : !showVolumeColumn && book.volume ? (
                            <span className="text-gray-600 font-medium">Vol. {book.volume}</span>
                          ) : (
                            <span className="text-gray-300 italic">{t('common.na')}</span>
                          )}
                        </td>
                        {showVolumeColumn && (
                          <td className="py-3 px-3 text-left text-sm text-gray-600">{book.volume || <span className="text-gray-300">−</span>}</td>
                        )}
                        {showPubDateColumn && (
                          <td className="py-3 px-3 text-left text-sm text-gray-600 whitespace-nowrap">{book.publicationDate || <span className="text-gray-300">−</span>}</td>
                        )}
                        {showPublisherColumn && (
                          <td className="py-3 px-3 text-left text-sm text-gray-600 truncate max-w-[140px]" title={book.publisher}>{book.publisher || <span className="text-gray-300">−</span>}</td>
                        )}
                        {showLabelsColumn && (
                          <td className="py-3 px-3 text-left">
                            <div className="flex flex-wrap gap-1">
                              {book.labels && book.labels.length > 0 ? book.labels.map(label => (
                                <span key={label} className="bg-gray-100 text-gray-600 text-[10px] font-bold px-1.5 py-0.5 rounded-full uppercase tracking-tighter border border-gray-200">
                                  {label}
                                </span>
                              )) : <span className="text-gray-300 text-sm">−</span>}
                            </div>
                          </td>
                        )}
                        {showFormatsColumn && (
                          <td className="py-3 px-3 text-left">
                            <div className="flex flex-wrap gap-1">
                              {book.formats && book.formats.length > 0 ? (
                                book.formats.map((fmt) => (
                                  <span
                                    key={fmt.id || fmt.formatType}
                                    className={`${getFormatBadgeStyle(fmt.formatType)} text-[10px] font-extrabold px-1.5 py-0.5 rounded-full uppercase tracking-tight border shadow-xs`}
                                    title={fmt.fileName ? `${fmt.fileName}${fmt.size ? ` (${formatBytes(fmt.size)})` : ''}` : formatBytes(fmt.size)}
                                  >
                                    {fmt.formatType}
                                  </span>
                                ))
                              ) : (
                                <span className="text-gray-300 text-sm">−</span>
                              )}
                            </div>
                          </td>
                        )}
                        <td className="py-3 px-2 text-center whitespace-nowrap w-10 relative action-menu-container">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setActiveActionMenuId(activeActionMenuId === book.id ? null : book.id);
                            }}
                            className={`p-1.5 rounded-lg text-gray-500 hover:text-indigo-600 hover:bg-gray-100 transition-all ${
                              activeActionMenuId === book.id ? 'bg-indigo-50 text-indigo-600 opacity-100' : 'opacity-0 group-hover:opacity-100 focus:opacity-100'
                            }`}
                            title={t('bookTable.header.actions')}
                          >
                            <FaEllipsisV size={13} />
                          </button>

                          {activeActionMenuId === book.id && (
                            <div className="absolute right-2 top-full mt-1 z-30 bg-white border border-gray-100 shadow-xl rounded-xl py-1 w-32 text-left animate-fade-in divide-y divide-gray-50">
                              <Link
                                to={`/books/${book.id}/edit`}
                                className="flex items-center gap-2 px-3 py-2 text-xs font-semibold text-gray-700 hover:bg-indigo-50 hover:text-indigo-600 transition-colors"
                                onClick={() => setActiveActionMenuId(null)}
                              >
                                <FaEdit size={13} className="text-indigo-500" />
                                {t('common.edit')}
                              </Link>
                              <button
                                onClick={() => {
                                  setActiveActionMenuId(null);
                                  handleDeleteClick(book);
                                }}
                                className="w-full flex items-center gap-2 px-3 py-2 text-xs font-semibold text-rose-600 hover:bg-rose-50 transition-colors text-left"
                              >
                                <FaTrash size={13} className="text-rose-500" />
                                {t('common.delete')}
                              </button>
                            </div>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            );
          })()}
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
