import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

const AuthorList = () => {
  const { t } = useTranslation();
  const [authors, setAuthors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchAuthors = async () => {
      try {
        const response = await fetch('/api/authors?page=0&size=100');
        if (!response.ok) {
          throw new Error('Failed to fetch authors');
        }
        const data = await response.json();
        setAuthors(data.content || []);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchAuthors();
  }, []);

  if (loading) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">{t('authorList.title')}</h1>
        <p className="text-center text-gray-500">{t('authorList.loading')}</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">{t('authorList.title')}</h1>
        <p className="text-center text-red-500">{t('common.error')}: {error}</p>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">{t('authorList.title')}</h1>
      {authors.length === 0 ? (
        <p className="text-gray-500">{t('authorList.noAuthorsFound')}</p>
      ) : (
        <ul className="list-disc list-inside bg-white border border-gray-300 rounded p-4 shadow">
          {authors.map((author) => (
            <li key={author.id} className="mb-2">
              <Link to={`/author/${author.id}`} className="author-link">
                {author.name}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default AuthorList;