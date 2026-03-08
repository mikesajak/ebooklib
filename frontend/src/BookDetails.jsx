import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { FaBook, FaEdit, FaTrash, FaInfoCircle, FaLayerGroup, FaTags, FaArrowLeft, FaFileAlt, FaChevronRight, FaCalendarAlt, FaBuilding, FaBookmark } from 'react-icons/fa';
import { useTranslation } from 'react-i18next';
import Notification from './Notification';
import ConfirmationDialog from './ConfirmationDialog';
import BookFormats from './BookFormats';
import { fetchWithCsrf } from './api';

const BookDetails = () => {
  const { id } = useParams();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [book, setBook] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notification, setNotification] = useState({ message: '', type: '', visible: false });
  const [showBookDeleteConfirmDialog, setShowBookDeleteConfirmDialog] = useState(false);
  const formatsRef = useRef(null);

  const fetchBookDetails = async () => {
    try {
      const response = await fetchWithCsrf(`/api/books/${id}`);
      if (!response.ok) throw new Error('Failed to fetch book details');
      const data = await response.json();
      setBook(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookDetails();
  }, [id]);

  const handleDeleteBookClick = () => setShowBookDeleteConfirmDialog(true);

  const executeBookDelete = async () => {
    try {
      const response = await fetchWithCsrf(`/api/books/${id}`, { method: 'DELETE' });
      if (!response.ok) throw new Error('Failed to delete book');
      navigate('/', { state: { notification: { message: t('bookTable.deleteSuccess'), type: 'success' } } });
    } catch (err) {
      setNotification({ message: `${t('bookTable.deleteFailure')}: ${err.message}`, type: 'error', visible: true });
      setShowBookDeleteConfirmDialog(false);
    }
  };

  const handleFormatsUpdate = () => {
    fetchBookDetails();
  };

  if (loading) return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
      <div className="animate-spin rounded-full h-12 w-12 border-4 border-indigo-100 border-t-indigo-600"></div>
      <p className="text-sm font-black text-gray-400 uppercase tracking-widest">{t('common.loading')}</p>
    </div>
  );
  if (error) return <div className="container mx-auto p-8 text-center text-rose-600 font-bold bg-rose-50 rounded-2xl border border-rose-100">{t('common.error')}: {error}</div>;
  if (!book) return <div className="container mx-auto p-8 text-center text-gray-500 font-bold">{t('bookTable.noBooksFound')}</div>;

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <header className="flex flex-col md:flex-row md:items-center justify-between gap-6 mb-12">
        <div className="flex items-center gap-6 animate-slide-in-left">
          <button 
            onClick={() => navigate('/')}
            className="p-4 bg-white text-gray-400 hover:text-indigo-600 border border-gray-200 rounded-2xl transition-all shadow-sm hover:shadow-md active:scale-95 group"
          >
            <FaArrowLeft size={20} className="transform group-hover:-translate-x-1 transition-transform" />
          </button>
          
          <div className="w-20 h-28 bg-indigo-600 text-white rounded-2xl shadow-xl shadow-indigo-100 flex items-center justify-center relative overflow-hidden group shrink-0">
            {book.coverId ? (
              <img src={`/api/books/${book.id}/cover`} alt={book.title} className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" />
            ) : (
              <FaBook size={32} className="opacity-50" />
            )}
            <div className="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent"></div>
          </div>
          
          <div className="flex-grow">
            <div className="flex flex-wrap gap-2 mb-2">
              {book.authors.map(author => (
                <Link key={author.id} to={`/author/${author.id}`} className="text-xs font-black text-indigo-600 hover:text-indigo-800 uppercase tracking-widest bg-indigo-50 px-2 py-1 rounded-md transition-colors">
                  {author.firstName} {author.lastName}
                </Link>
              ))}
            </div>
            <h1 className="text-4xl font-black text-gray-900 tracking-tighter">{book.title}</h1>
          </div>
        </div>

        <div className="flex gap-3">
          <Link 
            to={`/books/${id}/edit`} 
            className="bg-indigo-600 hover:bg-indigo-700 text-white font-black text-xs uppercase tracking-widest py-3 px-8 rounded-2xl shadow-xl shadow-indigo-100 transition-all flex items-center gap-2 transform hover:-translate-y-1 active:scale-95"
          >
            <FaEdit />
            {t('common.edit')}
          </Link>
          <button 
            onClick={handleDeleteBookClick} 
            className="bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-100 font-black text-xs uppercase tracking-widest py-3 px-8 rounded-2xl shadow-xl shadow-rose-50 transition-all flex items-center gap-2 transform hover:-translate-y-1 active:scale-95"
          >
            <FaTrash />
            {t('common.delete')}
          </button>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-12">
        <div className="lg:col-span-2 space-y-12">
          <div className="bg-white p-8 rounded-[2.5rem] border border-gray-100 shadow-2xl shadow-gray-200/50 flex flex-col gap-8 relative overflow-hidden group">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              <div className="space-y-1">
                <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest flex items-center gap-2"><FaBuilding className="text-indigo-400" /> {t('bookDetails.publisher', 'Publisher')}</p>
                <p className="text-gray-800 font-bold text-sm">{book.publisher || <span className="text-gray-300 italic">{t('common.notAvailable')}</span>}</p>
              </div>
              <div className="space-y-1">
                <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest flex items-center gap-2"><FaCalendarAlt className="text-indigo-400" /> {t('bookDetails.publicationDate', 'Published')}</p>
                <p className="text-gray-800 font-bold text-sm">{book.publicationDate || <span className="text-gray-300 italic">{t('common.notAvailable')}</span>}</p>
              </div>
              <div className="space-y-1">
                <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest flex items-center gap-2"><FaLayerGroup className="text-amber-500" /> {t('bookDetails.series', 'Series')}</p>
                {book.series ? (
                  <Link to={`/series/${book.series.id}`} className="text-amber-600 hover:text-amber-800 font-black text-sm tracking-tight flex items-center gap-1 group/link transition-colors bg-amber-50/50 px-2 py-0.5 rounded-md border border-amber-100/50">
                    {book.series.title} <FaChevronRight size={8} className="transform group-hover/link:translate-x-0.5 transition-transform" />
                  </Link>
                ) : <span className="text-gray-300 italic text-sm">{t('common.notAvailable')}</span>}
              </div>
              <div className="space-y-1">
                <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest flex items-center gap-2"><FaBookmark className="text-indigo-400" /> {t('bookDetails.volume', 'Volume')}</p>
                <p className="text-gray-800 font-bold text-sm">{book.volume || <span className="text-gray-300 italic">−</span>}</p>
              </div>
            </div>

            <div className="border-t border-gray-50 pt-8">
              <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-4 flex items-center gap-2"><FaInfoCircle className="text-indigo-400" /> {t('bookDetails.description', 'Description')}</p>
              <div className="text-gray-600 leading-relaxed text-sm font-medium bg-gray-50/50 p-6 rounded-3xl border border-gray-100">
                {book.description || <span className="text-gray-300 italic">{t('bookDetails.noDescription', 'No description provided for this book.')}</span>}
              </div>
            </div>

            <div className="border-t border-gray-50 pt-8">
              <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-4 flex items-center gap-2"><FaTags className="text-indigo-400" /> {t('bookDetails.labels', 'Labels')}</p>
              <div className="flex flex-wrap gap-2">
                {book.labels && book.labels.length > 0 ? book.labels.map(label => (
                  <span key={label} className="bg-indigo-50 text-indigo-600 text-[10px] font-black px-3 py-1 rounded-full border border-indigo-100 uppercase tracking-tighter shadow-sm hover:shadow-md transition-all cursor-default">
                    {label}
                  </span>
                )) : <span className="text-gray-300 italic text-sm">{t('bookDetails.noLabels', 'No labels')}</span>}
              </div>
            </div>
          </div>
        </div>

        <div className="lg:col-span-1 space-y-8">
          <div className="bg-white p-8 rounded-[2.5rem] border border-gray-100 shadow-2xl shadow-gray-200/50 relative overflow-hidden group">
            <h2 className="text-xs font-black text-gray-400 uppercase tracking-[0.3em] mb-6 flex items-center gap-2"><FaFileAlt className="text-indigo-400" /> {t('bookDetails.availableFormats', 'Available Formats')}</h2>
            <BookFormats bookId={id} formats={book.formats} onUpdate={handleFormatsUpdate} />
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

      {showBookDeleteConfirmDialog && (
        <ConfirmationDialog
          title={t('bookDetails.confirmBookDeleteTitle', 'Delete Book')}
          message={t('bookDetails.confirmBookDelete', { bookTitle: book.title })}
          onConfirm={executeBookDelete}
          onCancel={() => setShowBookDeleteConfirmDialog(false)}
          confirmButtonText={t('common.delete')}
          cancelButtonText={t('common.cancel')}
        />
      )}
    </div>
  );
};

export default BookDetails;
