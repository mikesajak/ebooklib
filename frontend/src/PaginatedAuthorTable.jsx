import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

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

  useEffect(() => {
    const fetchAuthors = async () => {
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
    };

    fetchAuthors();
  }, [page, size, sortField, sortDirection]);

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
                <button onClick={() => { /* handle delete */ }} className="text-red-600 hover:text-red-900">{t('common.delete')}</button>
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
    </div>
  );
};

export default PaginatedAuthorTable;