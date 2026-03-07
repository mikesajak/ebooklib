import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FaEdit, FaTrash, FaChevronDown } from 'react-icons/fa';
import ConfirmationDialog from './ConfirmationDialog';
import Notification from './Notification';
import Pagination from './Pagination';
import { useSearch } from './SearchContext';
import { fetchWithCsrf } from './api';

const PaginatedAuthorTable = () => {
  const { t } = useTranslation();
  const [authors, setAuthors] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(() => Number(localStorage.getItem('authorListPageSize')) || 10); // Default page size
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

      const response = await fetchWithCsrf(`${endpoint}?${params.toString()}`);
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

  if (loading) {
    return (
      <div className="py-12 flex flex-col items-center gap-2 text-gray-400 italic">
        <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-emerald-600"></div>
        {t('common.loading')}
      </div>
    );
  }

  if (error) {
    return (
      <div className="py-12 text-center text-red-500 italic">{t('common.error')}: {error}</div>
    );
  }
  if (authors.length === 0) {
    return (
      <div className="py-12 text-center text-gray-400 italic">{t('authorList.noAuthorsFound')}</div>
    );
  }

  return (
    <div className="bg-white overflow-hidden">
      <table className="min-w-full table-auto">
        <thead>
          <tr className="bg-gray-50 border-b border-gray-200 text-gray-600 uppercase text-[10px] font-black tracking-widest">
            <th className="py-4 px-6 text-left">
              <div className="relative inline-block text-left">
                <button
                  onClick={() => setDropdownOpen(!isDropdownOpen)}
                  className="inline-flex items-center gap-2 rounded-lg border border-gray-200 shadow-sm px-3 py-1.5 bg-white text-[10px] font-bold text-gray-700 hover:bg-gray-50 focus:outline-none transition-all"
                >
                  {sortOptions[sortBy].label}
                  <FaChevronDown className={`text-[8px] transition-transform duration-200 ${isDropdownOpen ? 'rotate-180' : ''}`} />
                </button>
                {isDropdownOpen && (
                  <div className="origin-top-left absolute left-0 mt-2 w-56 rounded-xl shadow-xl bg-white ring-1 ring-black ring-opacity-5 z-20 border border-gray-100 overflow-hidden">
                    <div className="py-1">
                      {nameColumnSortOptions.map(key => (
                        <button
                          key={key}
                          onClick={() => handleSortChange(key)}
                          className="block w-full text-left px-4 py-2 text-xs font-bold text-gray-600 hover:bg-emerald-50 hover:text-emerald-700 transition-colors"
                        >
                          {sortOptions[key].label}
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </th>
            <th className="py-4 px-6 text-left cursor-pointer hover:text-emerald-600 transition-colors" onClick={() => handleSimpleSort('bookCount')}>
              {t('authorList.header.bookCount')}
              <span className="ml-1 opacity-50">{sortBy.startsWith('bookCount') ? (sortBy.endsWith('Asc') ? ' ▲' : ' ▼') : ''}</span>
            </th>
            <th className="py-4 px-6 text-center">{t('common.actions')}</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {authors.map((author) => (
            <tr key={author.id} className="hover:bg-emerald-50/50 transition-colors group">
              <td className="py-4 px-6 text-left whitespace-nowrap">
                <Link to={`/author/${author.id}`} className="author-link group-hover:text-emerald-700 font-bold">
                  {author.firstName} {author.lastName}
                </Link>
              </td>
              <td className="py-4 px-6 text-left">
                <span className="bg-gray-100 px-2 py-0.5 rounded-full border border-gray-200 text-xs font-medium text-gray-600">
                  {author.bookCount}
                </span>
              </td>
              <td className="py-4 px-6 text-center whitespace-nowrap">
                <div className="flex justify-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <Link 
                    to={`/authors/${author.id}/edit`} 
                    className="p-2.5 text-indigo-600 hover:bg-indigo-50 rounded-xl transition-all"
                    title={t('common.edit')}
                  >
                    <FaEdit size={16} />
                  </Link>
                  <button 
                    onClick={() => openConfirmDialog(author)} 
                    className="p-2.5 text-rose-600 hover:bg-rose-50 rounded-xl transition-all"
                    title={t('common.delete')}
                  >
                    <FaTrash size={16} />
                  </button>
                </div>              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="bg-gray-50 border-t border-gray-100 p-4">
        <Pagination
          page={page}
          size={size}
          totalPages={totalPages}
          totalElements={totalElements}
          onPageChange={setPage}
          onPageSizeChange={(newSize) => {
            setSize(newSize);
            localStorage.setItem('authorListPageSize', newSize.toString());
            setPage(0); // Reset to first page when page size changes
          }}
          theme="emerald"
        />
      </div>

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
