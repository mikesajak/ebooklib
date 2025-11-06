import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Notification from './Notification';
import { useTranslation } from 'react-i18next';

const AddBook = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [book, setBook] = useState({
    title: '',
    authors: [],
    series: null,
    volume: '',
    publicationDate: '',
    publisher: '',
    description: ''
  });
  const [authors, setAuthors] = useState([]);
  const [series, setSeries] = useState([]);
  const [notification, setNotification] = useState(null);
  const [isSaving, setIsSaving] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setBook(prevBook => ({
      ...prevBook,
      [name]: value
    }));
  };

  const handleAuthorChange = (e) => {
    const selectedAuthorId = e.target.value;
    const selectedAuthor = authors.find(author => author.id === selectedAuthorId);
    setBook(prevBook => ({
      ...prevBook,
      authors: selectedAuthor ? [selectedAuthor] : []
    }));
  };

  const handleSeriesChange = (e) => {
    const selectedSeriesId = e.target.value;
    const selectedSeries = series.find(s => s.id === selectedSeriesId);
    setBook(prevBook => ({
      ...prevBook,
      series: selectedSeries ? selectedSeries : null
    }));
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      const bookData = {
        ...book,
        authorIds: book.authors.map(author => author.id),
        seriesId: book.series ? book.series.id : null
      };
      delete bookData.authors;
      delete bookData.series;

      const response = await fetch('/api/books', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(bookData),
      });

      if (!response.ok) {
        let errorMessage = 'Failed to create book';
        try {
            const errorData = await response.json();
            errorMessage = errorData.message || errorMessage;
        } catch (e) {
            // ignore if response is not json
        }
        throw new Error(errorMessage);
      }

      navigate('/', { state: { notification: { type: 'success', message: 'Book added successfully!' } } });
    } catch (err) {
      setNotification({ type: 'error', message: err.message });
      setIsSaving(false);
    }
  };

  const handleCancel = () => {
    navigate('/');
  };

  useEffect(() => {
    const fetchAuthors = async () => {
      try {
        const response = await fetch('/api/authors');
        if (!response.ok) throw new Error('Failed to fetch authors');
        const data = await response.json();
        setAuthors(data.content);
      } catch (err) {
        console.error('Error fetching authors:', err);
      }
    };

    const fetchSeries = async () => {
      try {
        const response = await fetch('/api/series');
        if (!response.ok) throw new Error('Failed to fetch series');
        const data = await response.json();
        setSeries(data.content);
      } catch (err) {
        console.error('Error fetching series:', err);
      }
    };

    fetchAuthors();
    fetchSeries();
  }, []);

  const isTitleValid = book.title.trim() !== '';
  const isSaveDisabled = !isTitleValid || isSaving;

  return (
    <div className="container mx-auto p-4">
      {notification && (
        <Notification
          message={notification.message}
          type={notification.type}
          onClose={() => setNotification(null)}
        />
      )}
      <h1 className="text-2xl font-bold mb-4">{t('addBook.title')}</h1>
      <div className="bg-white border border-gray-300 rounded p-6 shadow">
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="title">{t('addBook.form.title')}:</label>
          <input type="text" id="title" name="title" value={book.title} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="author">{t('addBook.form.author')}:</label>
          <select id="author" name="authorId" value={book.authors[0]?.id || ''} onChange={handleAuthorChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline">
            <option value="">{t('addBook.form.selectAuthor')}</option>
            {authors.map(author => (<option key={author.id} value={author.id}>{author.firstName} {author.lastName}</option>))}          </select>
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="series">{t('addBook.form.series')}:</label>
          <select id="series" name="seriesId" value={book.series?.id || ''} onChange={handleSeriesChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline">
            <option value="">{t('addBook.form.selectSeries')}</option>
            {series.map(s => (<option key={s.id} value={s.id}>{s.title}</option>))}
          </select>
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="volume">{t('addBook.form.volume')}:</label>
          <input type="number" id="volume" name="volume" value={book.volume} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="publicationDate">{t('addBook.form.publicationDate')}:</label>
          <input type="date" id="publicationDate" name="publicationDate" value={book.publicationDate} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="publisher">{t('addBook.form.publisher')}:</label>
          <input type="text" id="publisher" name="publisher" value={book.publisher} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="description">{t('addBook.form.description')}:</label>
          <textarea id="description" name="description" value={book.description} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="flex justify-end mt-4">
          <button onClick={handleSave} disabled={isSaveDisabled} className={`font-bold py-2 px-4 rounded mr-2 ${isSaveDisabled ? 'bg-gray-400 text-white cursor-not-allowed' : 'bg-green-500 hover:bg-green-700 text-white'}`}>
            {t('common.save')}
          </button>
          <button onClick={handleCancel} className="bg-gray-500 hover:bg-gray-700 text-white font-bold py-2 px-4 rounded">
            {t('common.cancel')}
          </button>
        </div>
      </div>
    </div>
  );
};

export default AddBook;
