import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FaLayerGroup, FaPlus, FaEdit, FaTrash } from 'react-icons/fa';
import { useSearch } from './SearchContext';
import SearchBar from './SearchBar';
import Pagination from './Pagination';
import { fetchWithCsrf } from './api';

const SeriesList = () => {
  const { t, ready } = useTranslation();
  const [series, setSeries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(() => Number(localStorage.getItem('seriesListPageSize')) || 10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [sortField, setSortField] = useState('title');
  const [sortDirection, setSortDirection] = useState('asc');
  const { searchQuery, refreshTrigger } = useSearch('series');

  const seriesQueryTransformer = (input) => {
    const operatorRegex = /[=<>!();,]/;
    if (input && !operatorRegex.test(input)) {
      return `title=like="${input}"`;
    }
    return input;
  };

  const fetchSeries = useCallback(async () => {
    setLoading(true);
    try {
      const endpoint = '/api/series/search';
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
        throw new Error(`Failed to fetch series: ${response.status} ${response.statusText}`);
      }
      const data = await response.json();
      setSeries(data.content || []);
      setTotalPages(data.page?.totalPages || 0);
      setTotalElements(data.page?.totalElements || 0);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [page, size, sortField, sortDirection, searchQuery, refreshTrigger]);

  useEffect(() => {
    fetchSeries();
  }, [fetchSeries]);

  // Reset page to 0 when search query changes
  useEffect(() => {
    setPage(0);
  }, [searchQuery]);

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

  if (!ready) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-amber-100 border-t-amber-600"></div>
        <p className="text-sm font-black text-gray-400 uppercase tracking-widest">{t('common.loading')}</p>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4 max-w-7xl">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-6 gap-4">
        <div className="flex items-center gap-3">
          <div className="bg-amber-600 text-white p-3 rounded-xl shadow-md">
            <FaLayerGroup className="text-xl" />
          </div>
          <div>
            <h1 className="text-2xl font-extrabold text-gray-900 tracking-tight">{t('seriesList.title')}</h1>
            <p className="text-xs text-gray-500 font-medium">{t('seriesList.seriesCount', { count: totalElements })}</p>
          </div>
        </div>

        <Link to="/series/add">
          <button className="bg-amber-600 hover:bg-amber-700 text-white font-black text-sm uppercase tracking-wider py-3 px-8 rounded-2xl shadow-xl shadow-amber-100 transition-all flex items-center gap-2 transform hover:-translate-y-1 active:scale-95">
            <FaPlus />
            {t('seriesList.addSeries')}
          </button>
        </Link>
      </div>

      <div className="w-full mb-6">
        <SearchBar scope="series" queryTransformer={seriesQueryTransformer} />
      </div>

      <div className="bg-white shadow-xl rounded-2xl overflow-hidden border border-gray-100">
        <div className="overflow-x-auto">
          <table className="min-w-full table-auto">
            <thead>
              <tr className="bg-gray-50/50 border-b border-gray-200 text-gray-500 uppercase text-[10px] font-black tracking-widest">
                <th className="py-4 px-6 text-left cursor-pointer hover:text-amber-600 transition-colors" onClick={() => handleSort('title')}>
                  {t('bookTable.header.title')}{getSortIndicator('title')}
                </th>
                <th className="py-4 px-6 text-center">{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 bg-white">
              {loading ? (
                <tr>
                  <td colSpan="2" className="px-6 py-12 whitespace-nowrap text-center text-sm text-gray-400 italic">
                    <div className="flex flex-col items-center gap-2">
                      <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-amber-600"></div>
                      {t('seriesList.loading')}
                    </div>
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan="2" className="px-6 py-12 whitespace-nowrap text-center text-red-500 italic">
                    {t('common.error')}: {error}
                  </td>
                </tr>
              ) : series.length === 0 ? (
                <tr>
                  <td colSpan="2" className="px-6 py-12 whitespace-nowrap text-center text-sm text-gray-400 italic">{t('seriesList.noSeriesFound')}</td>
                </tr>
              ) : (
                series.map((s) => (
                  <tr key={s.id} className="group hover:bg-amber-50/30 transition-all duration-200">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <Link to={`/series/${s.id}`} className="series-link group-hover:text-amber-700 font-bold text-gray-700">
                        {s.title}
                      </Link>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-center">
                      <div className="flex justify-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                        <Link 
                          to={`/series/${s.id}/edit`} 
                          className="p-2.5 text-indigo-600 hover:bg-indigo-50 rounded-xl transition-all"
                          title={t('common.edit')}
                        >
                          <FaEdit size={16} />
                        </Link>
                        <button 
                          onClick={() => { /* handle delete */ }} 
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

        <div className="bg-gray-50/50 border-t border-gray-100 p-4">
          <Pagination
            page={page}
            size={size}
            totalPages={totalPages}
            totalElements={totalElements}
            onPageChange={setPage}
            onPageSizeChange={(newSize) => {
              setSize(newSize);
              localStorage.setItem('seriesListPageSize', newSize.toString());
              setPage(0); // Reset to first page when page size changes
            }}
            theme="amber"
          />
        </div>
      </div>
    </div>
  );
};

export default SeriesList;
