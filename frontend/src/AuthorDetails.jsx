import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Notification from './Notification';
import ConfirmationDialog from './ConfirmationDialog';

const AuthorDetails = () => {
  const { t } = useTranslation();
  const { id } = useParams();
  const [author, setAuthor] = useState(null);
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showDeleteConfirmDialog, setShowDeleteConfirmDialog] = useState(false);
  const [notification, setNotification] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchAuthorAndBooks = async () => {
      try {
        const authorResponse = await fetch(`/api/authors/${id}`);
        if (!authorResponse.ok) {
          throw new Error('Failed to fetch author details');
        }
        const authorData = await authorResponse.json();
        setAuthor(authorData);

        const booksResponse = await fetch(`/api/authors/${id}/books?page=0&size=100`);
        if (!booksResponse.ok) {
          throw new Error('Failed to fetch author books');
        }
        const booksData = await booksResponse.json();
        setBooks(booksData.content || []);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchAuthorAndBooks();
  }, [id]);

  if (loading) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">{t('authorDetails.title')}</h1>
        <p className="text-center text-gray-500">{t('authorDetails.loading')}</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">{t('authorDetails.title')}</h1>
        <p className="text-center text-red-500">{t('common.error')}: {error}</p>
        <Link to="/authors" className="back-link">
          {t('common.backToList')}
        </Link>
      </div>
    );
  }

  const handleDeleteAuthor = () => {
    setShowDeleteConfirmDialog(true);
  };

  const executeDeleteAuthor = async () => {
    setShowDeleteConfirmDialog(false);
    try {
      const response = await fetch(`/api/authors/${id}`, {
        method: 'DELETE',
      });
      if (!response.ok) {
        let errorMessage = 'Failed to delete author';
        try {
          const errorData = await response.json();
          errorMessage = errorData.message || errorMessage;
        } catch (e) {
          // ignore if response is not json
        }
        throw new Error(errorMessage);
      }
      setNotification({ type: 'success', message: 'Author deleted successfully!' });
      navigate('/authors');
    } catch (err) {
      setNotification({ type: 'error', message: err.message });
    }
  };

  return (
    <div className="container mx-auto p-4">
      {notification && (
        <Notification
          message={notification.message}
          type={notification.type}
          onClose={() => setNotification(null)}
        />
      )}
      <h1 className="text-2xl font-bold mb-4">{t('authorDetails.title')}</h1>
      <div className="bg-white border border-gray-300 rounded p-6 shadow mb-6">
        <div className="mb-4">
          <strong>{t('authorDetails.name')}:</strong> {author.firstName} {author.lastName}
        </div>
        <div className="mb-4">
          <strong>{t('authorDetails.bio')}:</strong> {author.bio || t('common.na')}
        </div>
        <div className="mb-4">
          <strong>{t('authorDetails.birthDate')}:</strong> {author.birthDate || t('common.na')}
        </div>
        <div className="mb-4">
          <strong>{t('authorDetails.deathDate')}:</strong> {author.deathDate || t('common.na')}
        </div>
        <div className="flex justify-end mt-4">
          <Link to={`/authors/${id}/edit`} className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded mr-2">
            {t('common.edit')}
          </Link>
          <button
            onClick={handleDeleteAuthor}
            className="bg-red-100 text-red-700 hover:bg-red-700 hover:text-white font-bold py-2 px-4 rounded"
          >
            {t('common.delete')}
          </button>
        </div>
      </div>

      <h2 className="text-xl font-semibold mb-4">{t('authorDetails.booksByAuthor')}</h2>
      {books.length === 0 ? (
        <p className="text-gray-500">{t('authorDetails.noBooksFound')}</p>
      ) : (
        <ul className="list-disc list-inside bg-white border border-gray-300 rounded p-4 shadow">
          {books.map((book) => (
            <li key={book.id} className="mb-2">
              <Link to={`/book/${book.id}`} className="book-link">
                {book.title}
              </Link>
            </li>
          ))}
        </ul>
      )}

      <Link to="/authors" className="back-link">
        {t('common.backToList')}
      </Link>

      {showDeleteConfirmDialog && (
        <ConfirmationDialog
          message={
            <div className="text-center">
              <p className="mb-2">{t('authorDetails.confirmDeleteAuthor', { authorName: `${author.firstName} ${author.lastName}` })}</p>
              {books.length > 0 && (
                <>
                  <p className="mb-2">{t('authorDetails.affectedBooks')}:</p>
                  <ul className="list-disc list-inside text-left mx-auto max-w-xs">
                    {books.map((book) => (
                      <li key={book.id}>{book.title}</li>
                    ))}
                  </ul>
                </>
              )}
              <p className="mt-2">{t('authorDetails.warningDeleteAuthor')}</p>
            </div>
          }
          onConfirm={executeDeleteAuthor}
          onCancel={() => setShowDeleteConfirmDialog(false)}
        />
      )}
    </div>
  );
};

export default AuthorDetails;
