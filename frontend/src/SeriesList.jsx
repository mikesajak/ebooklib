import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

const SeriesList = () => {
  const { t, ready } = useTranslation();
  const [series, setSeries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [sortField, setSortField] = useState('title');
  const [sortDirection, setSortDirection] = useState('asc');

  console.log("SeriesList render. Ready:", ready, "Loading:", loading, "Error:", error);

  const fetchSeries = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch(`/api/series?page=${page}&size=${size}&sort=${sortField},${sortDirection}`);
      if (!response.ok) {
        throw new Error(`Failed to fetch series: ${response.status} ${response.statusText}`);
      }
      const data = await response.json();
      setSeries(data.content || []);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [page, size, sortField, sortDirection]);

  useEffect(() => {
    fetchSeries();
  }, [fetchSeries]);

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('asc'); // Default to ascending when changing sort field
    }
  };

  if (loading || !ready) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">{t('seriesList.title')}</h1>
        <p className="text-center text-gray-500">{t('seriesList.loading')}</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">{t('seriesList.title')}</h1>
        <p className="text-center text-red-500">{t('common.error')}: {error}</p>
      </div>
    );
  }

  const getSortIndicator = (field) => {
    if (sortField === field) {
      return sortDirection === 'asc' ? ' ▲' : ' ▼';
    }
    return '';
  };

  return (
    <div className="container mx-auto p-4">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold">{t('seriesList.title')}</h1>
        <Link to="/series/add">
          <button className="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded">
            {t('seriesList.addSeries')}
          </button>
        </Link>
      </div>
      {series.length === 0 ? (
        <p className="text-gray-500">{t('seriesList.noSeriesFound')}</p>
      ) : (
        <table className="min-w-full table-auto">
          <thead>
            <tr className="bg-gray-200 text-gray-600 uppercase text-sm leading-normal">
              <th className="py-3 px-6 text-left" onClick={() => handleSort('title')}>{t('seriesList.header.title')}{getSortIndicator('title')}</th>
              <th className="py-3 px-6 text-center">{t('common.actions')}</th>
            </tr>
          </thead>
          <tbody>
            {series.map((s, index) => (
              <tr key={s.id} className={index % 2 === 0 ? 'bg-gray-50' : 'bg-white'}>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 border-b border-gray-200">
                  <Link to={`/series/${s.id}`} className="series-link">
                    {s.title}
                  </Link>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium border-b border-gray-200">
                  {/* Action buttons will go here */}
                  <Link to={`/series/${s.id}/edit`} className="text-indigo-600 hover:text-indigo-900 mr-2">{t('common.edit')}</Link>
                  <button onClick={() => { /* handle delete */ }} className="text-red-600 hover:text-red-900">{t('common.delete')}</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

export default SeriesList;