import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { FaLayerGroup, FaEdit, FaTrash, FaInfoCircle, FaArrowLeft, FaBook, FaChevronRight } from 'react-icons/fa';
import { useTranslation } from 'react-i18next';
import Notification from './Notification';
import ConfirmationDialog from './ConfirmationDialog';
import { fetchWithCsrf } from './api';

const SeriesDetails = () => {
  const { id } = useParams();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [series, setSeries] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notification, setNotification] = useState({ message: '', type: '', visible: false });
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  useEffect(() => {
    const fetchSeries = async () => {
      try {
        const response = await fetchWithCsrf(`/api/series/${id}`);
        if (!response.ok) throw new Error('Failed to fetch series details');
        const data = await response.json();
        setSeries(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    fetchSeries();
  }, [id]);

  const handleDeleteClick = () => setShowDeleteConfirm(true);

  const executeDelete = async () => {
    try {
      const response = await fetchWithCsrf(`/api/series/${id}`, { method: 'DELETE' });
      if (!response.ok) throw new Error('Failed to delete series');
      navigate('/series', { state: { notification: { message: t('seriesList.seriesDeleted'), type: 'success' } } });
    } catch (err) {
      setNotification({ message: `${t('seriesList.errorDeletingSeries')}: ${err.message}`, type: 'error', visible: true });
      setShowDeleteConfirm(false);
    }
  };

  if (loading) return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
      <div className="animate-spin rounded-full h-12 w-12 border-4 border-amber-100 border-t-amber-600"></div>
      <p className="text-sm font-black text-gray-400 uppercase tracking-widest">{t('common.loading')}</p>
    </div>
  );
  if (error) return <div className="container mx-auto p-8 text-center text-rose-600 font-bold bg-rose-50 rounded-2xl border border-rose-100">{t('common.error')}: {error}</div>;
  if (!series) return <div className="container mx-auto p-8 text-center text-gray-500 font-bold">{t('seriesList.seriesNotFound', 'Series not found')}</div>;

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <header className="flex flex-col md:flex-row md:items-center justify-between gap-6 mb-12">
        <div className="flex items-center gap-6 animate-slide-in-left">
          <button 
            onClick={() => navigate('/series')}
            className="p-4 bg-white text-gray-400 hover:text-amber-600 border border-gray-200 rounded-2xl transition-all shadow-sm hover:shadow-md active:scale-95 group"
          >
            <FaArrowLeft size={20} className="transform group-hover:-translate-x-1 transition-transform" />
          </button>
          
          <div className="w-20 h-20 bg-amber-600 text-white rounded-3xl shadow-xl shadow-amber-100 flex items-center justify-center relative overflow-hidden group">
            <div className="absolute inset-0 bg-white/20 transform -skew-x-12 translate-x-full group-hover:-translate-x-full transition-transform duration-1000"></div>
            <FaLayerGroup size={32} />
          </div>
          
          <div className="flex-grow">
            <p className="text-xs text-amber-600 font-black uppercase tracking-widest mt-1">{t('seriesDetails.entityLabel', 'Book Series')}</p>
            <h1 className="text-4xl font-black text-gray-900 tracking-tighter mt-1">{series.title}</h1>
          </div>
        </div>

        <div className="flex gap-3">
          <Link 
            to={`/series/${id}/edit`} 
            className="bg-amber-600 hover:bg-amber-700 text-white font-black text-xs uppercase tracking-widest py-3 px-8 rounded-2xl shadow-xl shadow-amber-100 transition-all flex items-center gap-2 transform hover:-translate-y-1 active:scale-95"
          >
            <FaEdit />
            {t('common.edit')}
          </Link>
          <button 
            onClick={handleDeleteClick} 
            className="bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-100 font-black text-xs uppercase tracking-widest py-3 px-8 rounded-2xl shadow-xl shadow-rose-50 transition-all flex items-center gap-2 transform hover:-translate-y-1 active:scale-95"
          >
            <FaTrash />
            {t('common.delete')}
          </button>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-12">
        <div className="lg:col-span-2 space-y-12">
          <div className="bg-white p-8 rounded-[2.5rem] border border-gray-100 shadow-2xl shadow-gray-200/50 flex flex-col gap-6 relative overflow-hidden group">
            <div>
              <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-3 flex items-center gap-2"><FaInfoCircle className="text-amber-400" /> {t('seriesDetails.description', 'Description')}</p>
              <div className="text-gray-600 leading-relaxed text-sm font-medium bg-gray-50/50 p-6 rounded-2xl border border-gray-100">
                {series.description || <span className="text-gray-300 italic">{t('seriesDetails.noDescription', 'No description available for this series.')}</span>}
              </div>
            </div>
          </div>
        </div>

        <div className="lg:col-span-1 space-y-6">
          <div className="flex items-center justify-between px-4">
            <h2 className="text-xs font-black text-gray-400 uppercase tracking-[0.3em] flex items-center gap-2"><FaBook className="text-amber-400" /> {t('seriesDetails.booksInSeries', 'Books in Series')}</h2>
            <span className="text-[10px] font-black text-amber-600 bg-amber-50 px-2 py-1 rounded-md">{series.books?.length || 0}</span>
          </div>
          
          <div className="space-y-4">
            {series.books && series.books.length > 0 ? (
              series.books.map((book) => (
                <Link key={book.id} to={`/book/${book.id}`} className="block group/book">
                  <div className="bg-white p-5 rounded-2xl border border-gray-100 shadow-xl shadow-gray-100/50 flex items-center justify-between group-hover/book:border-amber-200 transition-all group-hover/book:-translate-y-1">
                    <div className="flex items-center gap-4">
                      <div className="p-3 rounded-xl bg-amber-50 text-amber-600 group-hover/book:bg-amber-600 group-hover/book:text-white transition-all">
                        <FaBook size={16} />
                      </div>
                      <div>
                        <span className="text-sm font-black text-gray-800 tracking-tight block">{book.title}</span>
                        {book.volume && <span className="text-[10px] font-black text-amber-400 uppercase">Volume {book.volume}</span>}
                      </div>
                    </div>
                    <FaChevronRight className="text-gray-300 group-hover/book:text-amber-600 transition-colors" />
                  </div>
                </Link>
              ))
            ) : (
              <div className="bg-gray-50/50 rounded-2xl border border-dashed border-gray-200 p-8 text-center">
                <p className="text-gray-400 text-sm italic">{t('seriesDetails.noBooks', 'No books found in this series.')}</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {notification.visible && (
        <Notification
          message={notification.message}
          type={notification.type}
          onClose={() => setNotification({ ...notification, visible: false })}
        />
      )}

      {showDeleteConfirm && (
        <ConfirmationDialog
          title={t('seriesList.confirmDeleteTitle', 'Confirm Delete')}
          message={t('seriesList.confirmDeleteMessage', { seriesTitle: series.title })}
          onConfirm={executeDelete}
          onCancel={() => setShowDeleteConfirm(false)}
          confirmButtonText={t('common.delete')}
          cancelButtonText={t('common.cancel')}
        />
      )}
    </div>
  );
};

export default SeriesDetails;
