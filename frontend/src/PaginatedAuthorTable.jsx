import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FaEdit, FaTrash, FaChevronDown, FaUserTag } from 'react-icons/fa';
import { useSearch } from './SearchContext';
import ConfirmationDialog from './ConfirmationDialog';
import Pagination from './Pagination';
import { fetchWithCsrf } from './api';

const PaginatedAuthorTable = () => {
  const { t } = useTranslation();
  const [authors, setAuthors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(() => Number(localStorage.getItem('authorListPageSize')) || 10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [sortBy, setSortBy] = useState('lastNameAsc');
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
  };

  const nameColumnSortOptions = ['lastNameAsc', 'lastNameDesc', 'firstNameAsc', 'firstNameDesc'];

  const authorQueryTransformer = (input) => {
    const operatorRegex = /[=<>!();,]/;
    if (input && !operatorRegex.test(input)) {
      return `name=like="${input}"`;
    }
    return input;
  };

  const fetchAuthors = useCallback(async () => {
    try {
      setLoading(true);
      const endpoint = '/api/authors/search';
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
      });
      params.append('sort', 'lastName,asc');
      params.append('sort', 'firstName,asc');

      const sortParams = sortOptions[sortBy].params.split('&');
      const finalParams = new URLSearchParams();
      finalParams.append('page', page.toString());
      finalParams.append('size', size.toString());
      sortParams.forEach(p => {
        const [k, v] = p.split('=');
        finalParams.append(k, v);
      });

      if (searchQuery) {
        finalParams.append('query', authorQueryTransformer(searchQuery));
      }

      const response = await fetchWithCsrf(`${endpoint}?${finalParams.toString()}`);
      if (!response.ok) throw new Error('Failed to fetch authors');
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

  const handleSortChange = (optionKey) => {
    setSortBy(optionKey);
    setDropdownOpen(false);
    setPage(0);
  };

  const openConfirmDialog = async (author) => {
    setAuthorToDelete(author);
    try {
      const response = await fetchWithCsrf(`/api/authors/${author.id}/books`);
      if (response.ok) {
        const books = await response.json();
        setAffectedBooks(Array.isArray(books) ? books : (books.content || []));
      }
    } catch (error) {
      console.error('Error fetching affected books:', error);
    }
    setShowConfirmDialog(true);
  };

  const closeConfirmDialog = () => {
    setShowConfirmDialog(false);
    setAuthorToDelete(null);
    setAffectedBooks([]);
  };

  const executeDelete = async () => {
    if (authorToDelete) {
      try {
        const response = await fetchWithCsrf(`/api/authors/${authorToDelete.id}`, { method: 'DELETE' });
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

  if (loading && authors.length === 0) {
    return <div className="p-8 text-center text-gray-500 italic">{t('common.loading')}</div>;
  }

  return (
    <div className="bg-white overflow-hidden">
      <div className="overflow-x-auto">
        <table className="min-w-full table-auto">
          <thead>
            <tr className="bg-gray-50/50 border-b border-gray-200 text-gray-500 uppercase text-[10px] font-black tracking-widest">
              <th className="py-4 px-6 text-left">
                <div className="relative inline-block text-left">
                  <button 
                    onClick={() => setDropdownOpen(!isDropdownOpen)}
                    className="inline-flex items-center gap-2 rounded-lg border border-gray-200 shadow-sm px-3 py-1.5 bg-white text-[10px] font-black text-gray-700 hover:bg-gray-50 focus:outline-none transition-all uppercase tracking-tighter"
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
                            className="block w-full text-left px-4 py-2 text-xs font-bold text-gray-600 hover:bg-indigo-50 hover:text-indigo-700 transition-colors"
                          >
                            {sortOptions[key].label}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              </th>
              <th className="py-4 px-6 text-left">{t('authorList.header.bookCount')}</th>
              <th className="py-4 px-6 text-center">{t('common.actions')}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 bg-white">
            {authors.map((author) => (
              <tr key={author.id} className="group hover:bg-indigo-50/30 transition-all duration-200">
                <td className="py-4 px-6 text-left whitespace-nowrap">
                  <div className="flex items-center gap-3">
                    <div className="bg-gray-100 p-2 rounded-lg text-gray-400 group-hover:bg-white group-hover:text-indigo-500 transition-all">
                      <FaUserTag size={12} />
                    </div>
                    <Link to={`/author/${author.id}`} className="author-link group-hover:text-indigo-700 font-bold text-gray-700">
                      {author.firstName} {author.lastName}
                    </Link>
                  </div>
                </td>
                <td className="py-4 px-6 text-left">
                  <span className="bg-gray-100 px-3 py-1 rounded-full border border-gray-200 text-[10px] font-black text-gray-600">
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
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="bg-gray-50/50 border-t border-gray-100 p-4">
        <Pagination
          page={page}
          size={size}
          totalPages={totalPages}
          totalElements={totalElements}
          onPageChange={setPage}
          onPageSizeChange={(newSize) => {
            setSize(newSize);
            localStorage.setItem('authorListPageSize', newSize.toString());
            setPage(0);
          }}
          theme="indigo"
        />
      </div>

      {showConfirmDialog && (
        <ConfirmationDialog
          title={t('authorList.confirmDeleteTitle')}
          message={
            <div>
              <p className="mb-4">{t('authorList.confirmDeleteMessage', { authorName: `${authorToDelete?.firstName} ${authorToDelete?.lastName}` })}</p>
              {affectedBooks.length > 0 && (
                <div className="bg-amber-50 border border-amber-100 p-4 rounded-xl text-amber-800 text-sm">
                  <p className="font-bold mb-2">{t('authorList.affectedBooks')}:</p>
                  <ul className="list-disc list-inside max-h-32 overflow-y-auto">
                    {affectedBooks.map(book => (
                      <li key={book.id}>{book.title}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          }
          onConfirm={executeDelete}
          onCancel={closeConfirmDialog}
          confirmButtonText={t('common.delete')}
          cancelButtonText={t('common.cancel')}
        />
      )}
    </div>
  );
};

export default PaginatedAuthorTable;
