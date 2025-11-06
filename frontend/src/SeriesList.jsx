import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

const SeriesList = () => {
  const { t } = useTranslation();
  const [series, setSeries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchSeries = async () => {
      try {
        const response = await fetch('/api/series?page=0&size=100');
        if (!response.ok) {
          throw new Error('Failed to fetch series');
        }
        const data = await response.json();
        setSeries(data.content || []);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchSeries();
  }, []);

  if (loading) {
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

  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">{t('seriesList.title')}</h1>
      {series.length === 0 ? (
        <p className="text-gray-500">{t('seriesList.noSeriesFound')}</p>
      ) : (
        <ul className="list-disc list-inside bg-white border border-gray-300 rounded p-4 shadow">
          {series.map((s) => (
            <li key={s.id} className="mb-2">
              <Link to={`/series/${s.id}`} className="series-link">
                {s.title}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default SeriesList;