import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import useMutation from './hooks/useMutation';
import AddPage from './AddPage';
import Form from './Form';
import SearchableDropdown from './SearchableDropdown';

const saveBook = async (bookData, isEditMode, bookId) => {
  const method = isEditMode ? 'PUT' : 'POST';
  const url = isEditMode ? `/api/books/${bookId}` : '/api/books';

  const response = await fetch(url, {
    method: method,
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(bookData),
  });

  if (!response.ok) {
    let errorMessage = 'Failed to save book';
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
  const { id } = useParams(); // Get book ID from URL
  const isEditMode = !!id; // Determine if in edit mode
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
  const [originalBook, setOriginalBook] = useState(null); // Store original book data for comparison
  const [authors, setAuthors] = useState([]);
  const [series, setSeries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [labelsString, setLabelsString] = useState('');

  const { mutate, isSaving, notification, setNotification } = useMutation(
    (bookData) => saveBook(bookData, isEditMode, id),
    {
      onSuccess: (savedBook) => {
        navigate(`/book/${savedBook.id}`, { state: { notification: { type: 'success', message: t(isEditMode ? 'addBook.updateSuccess' : 'addBook.addSuccess') } } });
      }
    }
  );

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [authorsResponse, seriesResponse, bookResponse] = await Promise.all([
          fetch('/api/authors?size=1000&sort=firstName,asc&sort=lastName,asc'),
          fetch('/api/series?size=1000&sort=title,asc'),
          isEditMode ? fetch(`/api/books/${id}`) : Promise.resolve(null)
        ]);

        const authorsData = await authorsResponse.json();
        setAuthors(authorsData.content || []);

        const seriesData = await seriesResponse.json();
        setSeries(seriesData.content || []);

        if (isEditMode) {
          if (!bookResponse.ok) {
            throw new Error('Failed to fetch book details');
          }
          const bookData = await bookResponse.json();
          setBook({
            ...bookData,
            // Format publicationDate for input[type="date"]
            publicationDate: bookData.publicationDate ? bookData.publicationDate.split('T')[0] : '',
            labels: bookData.labels || []
          });
          setLabelsString((bookData.labels || []).join(', '));
          setOriginalBook({
            ...bookData,
            publicationDate: bookData.publicationDate ? bookData.publicationDate.split('T')[0] : '',
            labels: bookData.labels || []
          });
        }
      } catch (err) {
        setNotification({ type: 'error', message: err.message });
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [id, isEditMode, setNotification]);


  const handleChange = (e) => {
    const { name, value } = e.target;
    if (name === 'labels') {
      setLabelsString(value);
      const labelsArray = value.split(',').map(label => label.trim()).filter(label => label !== '');
      setBook(prevBook => ({
        ...prevBook,
        labels: labelsArray
      }));
    } else {
      setBook(prevBook => ({
        ...prevBook,
        [name]: value
      }));
    }
  };

  const handleAuthorChange = (index, selectedAuthorId) => {
    const selectedAuthor = authors.find(author => author.id === selectedAuthorId);
    setBook(prevBook => {
      const newAuthors = [...prevBook.authors];
      if (selectedAuthor) {
        newAuthors[index] = selectedAuthor;
      }
      return { ...prevBook, authors: newAuthors };
    });
  };

  const handleAddAuthorField = () => {
    const lastAuthor = book.authors[book.authors.length - 1];
    if (!lastAuthor || lastAuthor.id) {
      setBook(prevBook => ({
        ...prevBook,
        authors: [...prevBook.authors, { id: '', firstName: '', lastName: '' }]
      }));
    }
  };

  const handleRemoveAuthorField = (index) => {
    setBook(prevBook => {
      const newAuthors = [...prevBook.authors];
      newAuthors.splice(index, 1);
      return { ...prevBook, authors: newAuthors };
    });
  };

  const handleSeriesChange = (selectedSeriesId) => {
    const selectedSeries = series.find(s => s.id === selectedSeriesId);
    setBook(prevBook => ({
      ...prevBook,
      series: selectedSeries ? selectedSeries : null
    }));
  };

  const handleSave = () => {
    const bookData = { ...book };
    // Ensure authorIds is an array and seriesId is properly set for the API
    bookData.authorIds = book.authors.map(author => author.id).filter(id => !!id);
    delete bookData.authors;

    if (bookData.series) {
      bookData.seriesId = bookData.series.id;
      delete bookData.series;
    }

    mutate(bookData);
  };

  const handleCancel = () => {
    navigate(isEditMode ? `/book/${id}` : '/');
  };

  const isTitleValid = book.title.trim() !== '';

  const authorOptions = useMemo(() =>
    authors.map(a => ({ id: a.id, name: `${a.firstName} ${a.lastName}` })),
    [authors]
  );

  const seriesOptions = useMemo(() =>
    series.map(s => ({ id: s.id, name: s.title })),
    [series]
  );

  const getFilteredAuthorOptions = (currentIndex) => {
    const selectedIds = book.authors
      .filter((_, i) => i !== currentIndex)
      .map(a => a.id)
      .filter(id => !!id);
    return authorOptions.filter(opt => !selectedIds.includes(opt.id));
  };

  const hasChanges = () => {
    if (!originalBook || !book) return false;

    const normalize = (val) => val || '';

    if (normalize(originalBook.title) !== normalize(book.title)) return true;
    if (String(normalize(originalBook.volume)) !== String(normalize(book.volume))) return true;
    if (normalize(originalBook.publicationDate) !== normalize(book.publicationDate)) return true;
    if (normalize(originalBook.publisher) !== normalize(book.publisher)) return true;
    if (normalize(originalBook.description) !== normalize(book.description)) return true;

    const originalAuthorIds = originalBook.authors?.map(a => a.id).sort() || [];
    const currentAuthorIds = book.authors?.filter(a => !!a.id).map(a => a.id).sort() || [];
    if (JSON.stringify(originalAuthorIds) !== JSON.stringify(currentAuthorIds)) return true;

    if (normalize(originalBook.series?.id) !== normalize(book.series?.id)) return true;

    const originalLabels = originalBook.labels?.sort() || [];
    const currentLabels = book.labels?.sort() || [];
    if (JSON.stringify(originalLabels) !== JSON.stringify(currentLabels)) return true;

    return false;
  };

  const isSaveDisabled = !isTitleValid || isSaving || (isEditMode && !hasChanges());

  if (loading) {
    return (
      <AddPage title={t(isEditMode ? 'addBook.editTitle' : 'addBook.title')} notification={notification} setNotification={setNotification}>
        <p>{t('common.loading')}</p>
      </AddPage>
    );
  }

  return (
    <AddPage title={t(isEditMode ? 'addBook.editTitle' : 'addBook.title')} notification={notification} setNotification={setNotification}>
      <Form onSave={handleSave} onCancel={handleCancel} isSaveDisabled={isSaveDisabled}>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="title">{t('addBook.form.title')}:</label>
          <input type="text" id="title" name="title" value={book.title} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2">{t('addBook.form.author')}:</label>
          {book.authors.map((author, index) => (
            <div key={index} className="flex mb-2 gap-2 items-start">
              <div className="flex-grow">
                <SearchableDropdown
                  id={`author-${index}`}
                  options={getFilteredAuthorOptions(index)}
                  value={author.id}
                  onChange={(id) => handleAuthorChange(index, id)}
                  placeholder={t('addBook.form.selectAuthor')}
                />
              </div>
              <button
                type="button"
                onClick={() => handleRemoveAuthorField(index)}
                className="bg-red-100 text-red-700 hover:bg-red-700 hover:text-white font-bold py-2 px-3 rounded h-fit mt-0"
                title={t('common.remove')}
              >
                ✖
              </button>
            </div>
          ))}
          <button
            type="button"
            onClick={handleAddAuthorField}
            disabled={book.authors.some(a => !a.id)}
            className="bg-blue-100 text-blue-700 hover:bg-blue-700 hover:text-white font-bold py-1 px-3 rounded text-sm disabled:opacity-50"
          >
            + {t('addBook.form.addAuthor')}
          </button>
        </div>

        <div className="mb-4">
          <SearchableDropdown
            id="series"
            label={t('addBook.form.series')}
            options={seriesOptions}
            value={book.series?.id || ''}
            onChange={handleSeriesChange}
            placeholder={t('addBook.form.selectSeries')}
          />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="volume">{t('addBook.form.volume')}:</label>
          <input type="number" id="volume" name="volume" value={book.volume || ''} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="publicationDate">{t('addBook.form.publicationDate')}:</label>
          <input type="date" id="publicationDate" name="publicationDate" value={book.publicationDate || ''} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="publisher">{t('addBook.form.publisher')}:</label>
          <input type="text" id="publisher" name="publisher" value={book.publisher || ''} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="description">{t('addBook.form.description')}:</label>
          <textarea id="description" name="description" value={book.description || ''} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="labels">{t('addBook.form.labels')}:</label>
          <input
            type="text"
            id="labels"
            name="labels"
            value={labelsString}
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