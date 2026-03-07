import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { FaBook, FaEdit, FaTrash, FaInfoCircle, FaLayerGroup, FaTags, FaArrowLeft, FaFileAlt } from 'react-icons/fa';
import Notification from './Notification';
import ConfirmationDialog from './ConfirmationDialog';
import BookFormats from './BookFormats';
import { fetchWithCsrf } from './api';
import { useTranslation } from 'react-i18next';

const BookDetails = () => {
  const { t } = useTranslation();
  const { id } = useParams();
  const [book, setBook] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notification, setNotification] = useState(null);
  const [showBookDeleteConfirmDialog, setShowBookDeleteConfirmDialog] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchBook = async () => {
      try {
        setLoading(true);
        const response = await fetchWithCsrf(`/api/books/${id}`);
        if (!response.ok) {
          throw new Error(t('bookDetails.errorFetching'));
        }
        const data = await response.json();
        setBook(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchBook();
  }, [id, t]);

  const handleDeleteBookClick = () => {
    setShowBookDeleteConfirmDialog(true);
  };

  const executeBookDelete = async () => {
    try {
      const response = await fetchWithCsrf(`/api/books/${id}`, {
        method: 'DELETE',
      });

      if (!response.ok) {
        throw new Error(t('bookDetails.deleteFailure'));
      }

      navigate('/', { state: { notification: { message: t('bookDetails.deleteSuccess'), type: 'success' } } });
    } catch (err) {
      setNotification({ message: err.message, type: 'error' });
      setShowBookDeleteConfirmDialog(false);
    }
  };

  const SectionHeader = ({ icon: Icon, title }) => (
    <div className="flex items-center gap-2 text-indigo-900 mb-4 border-b border-indigo-100 pb-2">
      <Icon className="text-indigo-500" />
      <h3 className="font-extrabold uppercase text-xs tracking-widest">{title}</h3>
    </div>
  );

  const DetailCard = ({ children, className = "" }) => (
    <div className={`bg-white p-6 rounded-2xl border border-gray-100 shadow-sm ${className}`}>
      {children}
    </div>
  );

  if (loading) {
    return (
      <div className="container mx-auto p-4 max-w-7xl text-center py-20">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto mb-4"></div>
        <p className="text-gray-500 italic">{t('common.loading')}</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto p-4 max-w-7xl">
        <div className="bg-red-50 border-l-4 border-red-500 p-4 rounded-r-xl">
          <p className="text-red-700 font-bold">{t('common.error')}: {error}</p>
          <Link to="/" className="text-red-600 hover:underline text-sm mt-2 inline-block flex items-center gap-1">
            <FaArrowLeft /> {t('common.backToList')}
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4 max-w-7xl">
      {notification && (
        <Notification
          message={notification.message}
          type={notification.type}
          onClose={() => setNotification(null)}
        />
      )}

      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4">
        <div className="flex items-center gap-4">
          <Link to="/" className="bg-white p-3 rounded-xl border border-gray-200 text-gray-400 hover:text-indigo-600 hover:border-indigo-100 shadow-sm transition-all">
            <FaArrowLeft />
          </Link>
          <div className="bg-indigo-600 text-white p-4 rounded-2xl shadow-lg">
            <FaBook className="text-2xl" />
          </div>
          <div>
            <h1 className="text-3xl font-black text-gray-900 tracking-tight leading-tight">{book.title}</h1>
            <div className="flex flex-wrap gap-2 mt-1">
              {book.authors.map(author => (
                <Link key={author.id} to={`/author/${author.id}`} className="text-emerald-600 font-bold hover:underline text-sm">
                  {author.firstName} {author.lastName}
                </Link>
              ))}
            </div>
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
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-8">
          <DetailCard>
            <SectionHeader icon={FaInfoCircle} title="Book Metadata" />
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-y-6 gap-x-12">
              <div>
                <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1">Publisher</p>
                <p className="text-gray-800 font-medium">{book.publisher || <span className="text-gray-300 italic">None set</span>}</p>
              </div>
              <div>
                <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1">Publication Date</p>
                <p className="text-gray-800 font-medium">{book.publicationDate || <span className="text-gray-300 italic">None set</span>}</p>
              </div>
              <div>
                <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1">Series</p>
                <p className="text-gray-800 font-medium">
                  {book.series ? (
                    <Link to={`/series/${book.series.id}`} className="text-amber-600 hover:underline flex items-center gap-1.5">
                      <FaLayerGroup className="text-[10px]" /> {book.series.title}
                    </Link>
                  ) : <span className="text-gray-300 italic">None</span>}
                </p>
              </div>
              <div>
                <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1">Volume</p>
                <p className="text-gray-800 font-medium">{book.volume || <span className="text-gray-300 italic">None</span>}</p>
              </div>
            </div>

            <div className="mt-8 pt-6 border-t border-gray-50">
              <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-2">Description</p>
              <div className="text-gray-600 leading-relaxed text-sm whitespace-pre-wrap bg-gray-50 p-4 rounded-xl border border-gray-100">
                {book.description || <span className="text-gray-300 italic">No description provided for this book.</span>}
              </div>
            </div>

            <div className="mt-8 pt-6 border-t border-gray-50">
              <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-3">Labels</p>
              <div className="flex flex-wrap gap-2">
                {book.labels && book.labels.length > 0 ? book.labels.map(label => (
                  <span key={label} className="bg-indigo-50 text-indigo-700 text-[10px] font-bold px-3 py-1 rounded-full uppercase tracking-tight border border-indigo-100 flex items-center gap-1.5 shadow-sm">
                    <FaTags className="text-[9px] opacity-50" /> {label}
                  </span>
                )) : <span className="text-gray-300 text-sm italic">No labels</span>}
              </div>
            </div>
          </DetailCard>
        </div>

        <div className="space-y-8">
          <DetailCard className="ring-2 ring-indigo-50 border-indigo-100">
            <SectionHeader icon={FaFileAlt} title="Available Formats" />
            <BookFormats book={book} onNotification={setNotification} />
          </DetailCard>
        </div>
      </div>

      {showBookDeleteConfirmDialog && (
        <ConfirmationDialog
          message={t('bookDetails.confirmBookDelete')}
          onConfirm={executeBookDelete}
          onCancel={() => setShowBookDeleteConfirmDialog(false)}
        />
      )}
    </div>
  );
};

export default BookDetails;
