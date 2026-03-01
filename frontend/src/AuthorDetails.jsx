import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { FaUserTag, FaEdit, FaTrash, FaInfoCircle, FaArrowLeft, FaCalendarAlt, FaHistory, FaBook } from 'react-icons/fa';
import { useTranslation } from 'react-i18next';
import Notification from './Notification';
import ConfirmationDialog from './ConfirmationDialog';
import { fetchWithCsrf } from './api';

const AuthorDetails = () => {
  const { t } = useTranslation();
  const { id } = useParams();
  const [author, setAuthor] = useState(null);
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notification, setNotification] = useState(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [authorResponse, booksResponse] = await Promise.all([
          fetchWithCsrf(`/api/authors/${id}`),
          fetchWithCsrf(`/api/authors/${id}/books`)
        ]);

        if (!authorResponse.ok) throw new Error('Failed to fetch author details');
        if (!booksResponse.ok) throw new Error('Failed to fetch author books');

        const authorData = await authorResponse.json();
        const booksData = await booksResponse.json();

        setAuthor(authorData);
        setBooks(Array.isArray(booksData) ? booksData : (booksData.content || []));
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [id]);

  const handleDeleteClick = () => setShowDeleteConfirm(true);

  const executeDelete = async () => {
    try {
      const response = await fetchWithCsrf(`/api/authors/${id}`, { method: 'DELETE' });
      if (!response.ok) throw new Error('Failed to delete author');
      navigate('/authors', { state: { notification: { message: t('authorList.authorDeleted'), type: 'success' } } });
    } catch (err) {
      setNotification({ message: err.message, type: 'error' });
      setShowDeleteConfirm(false);
    }
  };

  const SectionHeader = ({ icon: Icon, title }) => (
    <div className="flex items-center gap-2 text-emerald-900 mb-4 border-b border-emerald-100 pb-2">
      <Icon className="text-emerald-500" />
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
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-emerald-600 mx-auto mb-4"></div>
        <p className="text-gray-500 italic">{t('common.loading')}</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto p-4 max-w-7xl">
        <div className="bg-red-50 border-l-4 border-red-500 p-4 rounded-r-xl">
          <p className="text-red-700 font-bold">{t('common.error')}: {error}</p>
          <Link to="/authors" className="text-red-600 hover:underline text-sm mt-2 inline-block flex items-center gap-1">
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
          <Link to="/authors" className="bg-white p-3 rounded-xl border border-gray-200 text-gray-400 hover:text-emerald-600 hover:border-emerald-100 shadow-sm transition-all">
            <FaArrowLeft />
          </Link>
          <div className="bg-emerald-600 text-white p-4 rounded-2xl shadow-lg">
            <FaUserTag className="text-2xl" />
          </div>
          <div>
            <h1 className="text-3xl font-black text-gray-900 tracking-tight leading-tight">{author.firstName} {author.lastName}</h1>
            <p className="text-xs text-emerald-600 font-bold uppercase tracking-widest mt-1">Author Entity</p>
          </div>
        </div>

        <div className="flex gap-3">
          <Link 
            to={`/authors/${id}/edit`} 
            className="bg-emerald-600 hover:bg-emerald-700 text-white font-bold py-2.5 px-6 rounded-xl shadow-md transition-all flex items-center gap-2"
          >
            <FaEdit />
            {t('common.edit')}
          </Link>
          <button 
            onClick={handleDeleteClick} 
            className="bg-white hover:bg-red-50 text-red-600 border border-red-100 font-bold py-2.5 px-6 rounded-xl shadow-sm transition-all flex items-center gap-2"
          >
            <FaTrash />
            {t('common.delete')}
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-8">
          <DetailCard>
            <SectionHeader icon={FaInfoCircle} title="Biography & Details" />
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-y-6 gap-x-12 mb-8">
              <div>
                <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1 flex items-center gap-1.5">
                  <FaCalendarAlt className="text-[9px]" /> Birth Date
                </p>
                <p className="text-gray-800 font-medium">{author.birthDate || <span className="text-gray-300 italic">Not available</span>}</p>
              </div>
              <div>
                <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1 flex items-center gap-1.5">
                  <FaHistory className="text-[9px]" /> Death Date
                </p>
                <p className="text-gray-800 font-medium">{author.deathDate || <span className="text-gray-300 italic">Not available</span>}</p>
              </div>
            </div>

            <div className="pt-6 border-t border-gray-50">
              <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-2">Short Bio</p>
              <div className="text-gray-600 leading-relaxed text-sm whitespace-pre-wrap bg-gray-50 p-4 rounded-xl border border-gray-100 italic">
                {author.bio || <span className="text-gray-300">No biographical information available for this author.</span>}
              </div>
            </div>
          </DetailCard>
        </div>

        <div className="space-y-8">
          <DetailCard className="ring-2 ring-emerald-50 border-emerald-100">
            <SectionHeader icon={FaBook} title={`Books by this Author (${books.length})`} />
            <div className="space-y-3">
              {books.length === 0 ? (
                <p className="text-gray-400 text-sm italic py-4 text-center">No books found in library.</p>
              ) : (
                books.map(book => (
                  <Link 
                    key={book.id} 
                    to={`/book/${book.id}`}
                    className="flex items-center gap-3 p-3 rounded-xl border border-gray-100 hover:border-emerald-200 hover:bg-emerald-50 transition-all group"
                  >
                    <div className="bg-gray-100 text-gray-400 p-2 rounded group-hover:bg-white group-hover:text-emerald-500 transition-colors">
                      <FaBook className="text-xs" />
                    </div>
                    <span className="text-sm font-bold text-gray-700 group-hover:text-emerald-900 transition-colors">{book.title}</span>
                  </Link>
                ))
              )}
            </div>
          </DetailCard>
        </div>
      </div>

      {showDeleteConfirm && (
        <ConfirmationDialog
          title={t('authorList.confirmDeleteTitle')}
          message={t('authorList.confirmDeleteMessage', { authorName: `${author.firstName} ${author.lastName}` })}
          onConfirm={executeDelete}
          onCancel={() => setShowDeleteConfirm(false)}
          confirmButtonText={t('common.delete')}
          cancelButtonText={t('common.cancel')}
        />
      )}
    </div>
  );
};

export default AuthorDetails;
