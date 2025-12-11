import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import useMutation from './hooks/useMutation';
import AddPage from './AddPage';
import Form from './Form';

const createBook = async (bookData) => {
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

  return response.json();
}

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
    description: '',
    labels: []
  });
  const [authors, setAuthors] = useState([]);
  const [series, setSeries] = useState([]);

  const { mutate, isSaving, notification, setNotification } = useMutation(createBook, {
    onSuccess: () => {
      navigate('/', { state: { notification: { type: 'success', message: 'Book added successfully!' } } });
    }
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    if (name === 'labels') {
      setBook(prevBook => ({
        ...prevBook,
        labels: value.split(',').map(label => label.trim()).filter(label => label !== '')
      }));
    } else {
      setBook(prevBook => ({
        ...prevBook,
        [name]: value
      }));
    }
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

  const handleSave = () => {
    const bookData = {
      ...book,
      authorIds: book.authors.map(author => author.id),
      seriesId: book.series ? book.series.id : null,
      labels: book.labels
    };
    delete bookData.authors;
    delete bookData.series;
    mutate(bookData);
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
    <AddPage title={t('addBook.title')} notification={notification} setNotification={setNotification}>
      <Form onSave={handleSave} onCancel={handleCancel} isSaveDisabled={isSaveDisabled}>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="title">{t('addBook.form.title')}:</label>
          <input type="text" id="title" name="title" value={book.title} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="author">{t('addBook.form.author')}:</label>
          <select id="author" name="authorId" value={book.authors[0]?.id || ''} onChange={handleAuthorChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline">
            <option value="">{t('addBook.form.selectAuthor')}</option>
            {authors.map(author => (<option key={author.id} value={author.id}>{author.firstName} {author.lastName}</option>))}
          </select>
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
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="labels">{t('addBook.form.labels')}:</label>
          <input
            type="text"
            id="labels"
            name="labels"
            value={book.labels.join(', ')}
            onChange={handleChange}
            className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            placeholder={t('addBook.form.labelsPlaceholder')}
          />
        </div>
      </Form>
    </AddPage>
  );
};

export default AddBook;