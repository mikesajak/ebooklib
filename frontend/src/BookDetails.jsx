import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import Notification from './Notification';
import { useTranslation } from 'react-i18next';

const BookDetails = () => {
  const { t } = useTranslation();
  const { id } = useParams();
  const [book, setBook] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [editedBook, setEditedBook] = useState(null);
  const [authors, setAuthors] = useState([]);
  const [series, setSeries] = useState([]);
  const [notification, setNotification] = useState(null);

  const handleChange = (e) => {
    const { name, value } = e.target;
    if (name === 'labels') {
      setEditedBook(prevBook => ({
        ...prevBook,
        labels: value.split(',').map(label => label.trim()).filter(label => label !== '')
      }));
    } else {
      setEditedBook(prevBook => ({
        ...prevBook,
        [name]: value
      }));
    }
  };

  const handleAuthorChange = (e) => {
    const selectedAuthorId = e.target.value;
    const selectedAuthor = authors.find(author => author.id === selectedAuthorId);
    setEditedBook(prevBook => ({
      ...prevBook,
      authors: selectedAuthor ? [selectedAuthor] : []
    }));
  };

  const handleSeriesChange = (e) => {
    const selectedSeriesId = e.target.value;
    const selectedSeries = series.find(s => s.id === selectedSeriesId);
    setEditedBook(prevBook => ({
      ...prevBook,
      series: selectedSeries ? selectedSeries : null
    }));
  };

  const fetchBook = async () => {
    try {
      const response = await fetch(`/api/books/${id}`);
      if (!response.ok) {
        throw new Error('Failed to fetch book details');
      }
      const data = await response.json();
      setBook(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    try {
      const bookData = {
        ...editedBook,
        authorIds: editedBook.authors.map(author => author.id),
        seriesId: editedBook.series ? editedBook.series.id : null
      };
      delete bookData.authors;
      delete bookData.series;

      const response = await fetch(`/api/books/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(bookData),
      });

      if (!response.ok) {
        let errorMessage = 'Failed to update book';
        try {
            const errorData = await response.json();
            errorMessage = errorData.message || errorMessage;
        } catch (e) {
            // ignore if response is not json
        }
        throw new Error(errorMessage);
      }

      const updatedBook = await response.json();
      setBook(updatedBook);
      setIsEditing(false);
      setNotification({ type: 'success', message: 'Book updated successfully!' });
    } catch (err) {
      setNotification({ type: 'error', message: err.message });
      fetchBook();
      setIsEditing(false);
    }
  };

  useEffect(() => {
    fetchBook();
  }, [id]);

  useEffect(() => {
    if (book) {
      setEditedBook({ ...book });
    }
  }, [book]);

  useEffect(() => {
    const fetchAuthors = async () => {
      try {
        const response = await fetch('/api/authors');
        if (!response.ok) {
          throw new Error('Failed to fetch authors');
        }
        const data = await response.json();
        setAuthors(data.content);
      } catch (err) {
        console.error('Error fetching authors:', err);
      }
    };
    fetchAuthors();
  }, []);

  useEffect(() => {
    const fetchSeries = async () => {
      try {
        const response = await fetch('/api/series');
        if (!response.ok) {
          throw new Error('Failed to fetch series');
        }
        const data = await response.json();
        setSeries(data.content);
      } catch (err) {
        console.error('Error fetching series:', err);
      }
    };
    fetchSeries();
  }, []);

  if (loading) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">{t('bookDetails.title')}</h1>
        <p className="text-center text-gray-500">{t('bookDetails.loading')}</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">{t('bookDetails.title')}</h1>
        <p className="text-center text-red-500">{t('common.error')}: {error}</p>
        <Link to="/" className="back-link">
          {t('common.backToList')}
        </Link>
      </div>
    );
  }

  const isTitleValid = editedBook?.title?.trim() !== '';

  const hasChanges = () => {
    if (!book || !editedBook) return false;

    const normalize = (val) => val || '';

    if (normalize(book.title) !== normalize(editedBook.title)) return true;
    if (String(normalize(book.volume)) !== String(normalize(editedBook.volume))) return true;
    if (normalize(book.publicationDate) !== normalize(editedBook.publicationDate)) return true;
    if (normalize(book.publisher) !== normalize(editedBook.publisher)) return true;
    if (normalize(book.description) !== normalize(editedBook.description)) return true;

    const bookAuthorIds = book.authors?.map(a => a.id).sort() || [];
    const editedBookAuthorIds = editedBook.authors?.map(a => a.id).sort() || [];
    if (JSON.stringify(bookAuthorIds) !== JSON.stringify(editedBookAuthorIds)) return true;

    if (normalize(book.series?.id) !== normalize(editedBook.series?.id)) return true;

    const bookLabels = book.labels?.sort() || [];
    const editedBookLabels = editedBook.labels?.sort() || [];
    if (JSON.stringify(bookLabels) !== JSON.stringify(editedBookLabels)) return true;

    return false;
  };

  const isSaveDisabled = !isTitleValid || !hasChanges();

  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">{t('bookDetails.title')}</h1>
      {notification && (
        <Notification
          message={notification.message}
          type={notification.type}
          onClose={() => setNotification(null)}
        />
      )}
      <div className="flex justify-end mb-4">
        <button
          onClick={() => setIsEditing(!isEditing)}
          className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
        >
          {isEditing ? t('common.cancel') : t('common.edit')}
        </button>
      </div>
      {isEditing ? (
        <div className="bg-white border border-gray-300 rounded p-6 shadow">
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="title">{t('bookDetails.form.title')}:</label>
            <input
              type="text"
              id="title"
              name="title"
              value={editedBook?.title || ''}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            />
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="author">{t('bookDetails.form.author')}:</label>
            <select
              id="author"
              name="authorId"
              value={editedBook?.authors?.[0]?.id || ''}
              onChange={handleAuthorChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            >
              <option value="">{t('bookDetails.form.selectAuthor')}</option>
              {authors.map(author => (
                <option key={author.id} value={author.id}>
                  {author.firstName} {author.lastName}
                </option>
              ))}
            </select>
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="series">{t('bookDetails.form.series')}:</label>
            <select
              id="series"
              name="seriesId"
              value={editedBook?.series?.id || ''}
              onChange={handleSeriesChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            >
              <option value="">{t('bookDetails.form.selectSeries')}</option>
              {series.map(s => (
                <option key={s.id} value={s.id}>
                  {s.title}
                </option>
              ))}
            </select>
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="volume">{t('bookDetails.form.volume')}:</label>
            <input
              type="number"
              id="volume"
              name="volume"
              value={editedBook?.volume || ''}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            />
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="creationDate">{t('bookDetails.form.creationDate')}:</label>
            <input
              type="date"
              id="creationDate"
              name="creationDate"
              value={editedBook?.creationDate || ''}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            />
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="publicationDate">{t('bookDetails.form.publicationDate')}:</label>
            <input
              type="date"
              id="publicationDate"
              name="publicationDate"
              value={editedBook?.publicationDate || ''}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            />
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="publisher">{t('bookDetails.form.publisher')}:</label>
            <input
              type="text"
              id="publisher"
              name="publisher"
              value={editedBook?.publisher || ''}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            />
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="description">{t('bookDetails.form.description')}:</label>
            <textarea
              id="description"
              name="description"
              value={editedBook?.description || ''}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            />
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="labels">{t('bookDetails.form.labels')}:</label>
            <input
              type="text"
              id="labels"
              name="labels"
              value={editedBook?.labels?.join(', ') || ''}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
              placeholder={t('bookDetails.form.labelsPlaceholder')}
            />
          </div>
          <div className="flex justify-end mt-4">
            <button
              onClick={handleSave}
              disabled={isSaveDisabled}
              className={`font-bold py-2 px-4 rounded mr-2 ${isSaveDisabled ? 'bg-gray-400 text-white cursor-not-allowed' : 'bg-green-500 hover:bg-green-700 text-white'}`}>
              {t('common.save')}
            </button>
            <button
              onClick={() => setIsEditing(false)}
              className="bg-gray-500 hover:bg-gray-700 text-white font-bold py-2 px-4 rounded"
            >
              {t('common.cancel')}
            </button>
          </div>
        </div>
      ) : (
        <div className="bg-white border border-gray-300 rounded p-6 shadow">
          <div className="mb-4">
            <strong>{t('bookDetails.display.title')}:</strong> {book.title}
          </div>
          <div className="mb-4">
            <strong>{t('bookDetails.display.authors')}:</strong> {book.authors.map((author, index) => (
              <span key={author.id}>
                  {author.firstName} {author.lastName}
                {index < book.authors.length - 1 ? ', ' : ''}
              </span>
            ))}
          </div>
          <div className="mb-4">
            <strong>{t('bookDetails.display.series')}:</strong> {book.series ? <Link to={`/series/${book.series.id}`} className="series-link">{book.series.title}</Link> : t('common.na')}
          </div>
          <div className="mb-4">
            <strong>{t('bookDetails.display.volume')}:</strong> {book.volume || t('common.na')}
          </div>
          <div className="mb-4">
            <strong>{t('bookDetails.display.creationDate')}:</strong> {book.creationDate || t('common.na')}
          </div>
          <div className="mb-4">
            <strong>{t('bookDetails.display.publicationDate')}:</strong> {book.publicationDate || t('common.na')}
          </div>
          <div className="mb-4">
            <strong>{t('bookDetails.display.publisher')}:</strong> {book.publisher || t('common.na')}
          </div>
          <div className="mb-4">
            <strong>{t('bookDetails.display.description')}:</strong> {book.description || t('common.na')}
          </div>
          <div className="mb-4">
            <strong>{t('bookDetails.display.labels')}:</strong> {book.labels && book.labels.length > 0 ? book.labels.join(', ') : t('common.na')}
          </div>
        </div>
      )}
      <Link to="/" className="back-link">
        {t('common.backToList')}
      </Link>
    </div>
  );
};

export default BookDetails;
