import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

const AuthorDetails = () => {
  const { t } = useTranslation();
  const { id } = useParams();
  const [author, setAuthor] = useState(null);
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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
        <Link to="/" className="back-link">
          {t('common.backToList')}
        </Link>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4">
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
          <Link to={`/authors/${id}/edit`} className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded">
            {t('common.edit')}
          </Link>
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

      <Link to="/" className="back-link">
        {t('common.backToList')}
      </Link>
    </div>
  );
};

export default AuthorDetails;
