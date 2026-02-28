import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FaCloudUploadAlt, FaFileImport } from 'react-icons/fa';
import useMutation from './hooks/useMutation';
import AddPage from './AddPage';
import Form from './Form';
import SearchableDropdown from './SearchableDropdown';
import Notification from './Notification';
import ImportReviewDialog from './ImportReviewDialog';
import { fetchWithCsrf } from './api';

const saveBook = async (bookData, isEditMode, bookId) => {
  const method = isEditMode ? 'PUT' : 'POST';
  const url = isEditMode ? `/api/books/${bookId}` : '/api/books';

  const response = await fetchWithCsrf(url, {
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
    } catch (e) {}
    throw new Error(errorMessage);
  }

  return response.json();
}

const AddBook = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { id } = useParams();
  const isEditMode = !!id;

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
  const [originalBook, setOriginalBook] = useState(null);
  const [authors, setAuthors] = useState([]);
  const [series, setSeries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [labelsString, setLabelsString] = useState('');

  // Import related state
  const [isUploading, setIsUploading] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const [stagedUpload, setStagedUpload] = useState(null);
  const [isFinalizing, setIsFinalizing] = useState(false);

  const { mutate, isSaving, notification, setNotification } = useMutation(
    (bookData) => saveBook(bookData, isEditMode, id),
    {
      onSuccess: (savedBook) => {
        navigate(`/book/${savedBook.id}`, { 
          state: { 
            notification: { 
              type: 'success', 
              message: t(isEditMode ? 'addBook.updateSuccess' : 'addBook.addSuccess') 
            } 
          } 
        });
      }
    }
  );

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [authorsResponse, seriesResponse, bookResponse] = await Promise.all([
          fetchWithCsrf('/api/authors?size=1000&sort=firstName,asc&sort=lastName,asc'),
          fetchWithCsrf('/api/series?size=1000&sort=title,asc'),
          isEditMode ? fetchWithCsrf(`/api/books/${id}`) : Promise.resolve(null)
        ]);

        const authorsData = await authorsResponse.json();
        setAuthors(authorsData.content || []);

        const seriesData = await seriesResponse.json();
        setSeries(seriesData.content || []);

        if (isEditMode && bookResponse) {
          if (!bookResponse.ok) throw new Error('Failed to fetch book details');
          const bookData = await bookResponse.json();
          const initialBook = {
            ...bookData,
            publicationDate: bookData.publicationDate ? bookData.publicationDate.split('T')[0] : '',
            labels: bookData.labels || []
          };
          setBook(initialBook);
          setLabelsString((bookData.labels || []).join(', '));
          setOriginalBook(initialBook);
        }
      } catch (err) {
        setNotification({ type: 'error', message: err.message });
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [id, isEditMode, setNotification]);

  const handleFile = async (file) => {
    if (!file) return;
    setIsUploading(true);
    setNotification(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetchWithCsrf('/api/import/upload', {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) throw new Error(t('import.uploadError'));

      const data = await response.json();
      setStagedUpload(data);
      
      // Auto-populate form from extracted metadata
      if (data.metadata) {
        setBook(prev => ({
          ...prev,
          title: data.metadata.title || prev.title,
          publisher: data.metadata.publisher || prev.publisher,
          publicationDate: data.metadata.publicationDate ? data.metadata.publicationDate.split('T')[0] : prev.publicationDate,
          description: data.metadata.description || prev.description,
        }));
      }
    } catch (err) {
      setNotification({ type: 'error', message: err.message });
    } finally {
      setIsUploading(false);
    }
  };

  const onFileSelect = (e) => {
    if (e.target.files && e.target.files[0]) handleFile(e.target.files[0]);
  };

  const handleDrag = (e) => {
    e.preventDefault(); e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") setDragActive(true);
    else if (e.type === "dragleave") setDragActive(false);
  };

  const handleDrop = (e) => {
    e.preventDefault(); e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) handleFile(e.dataTransfer.files[0]);
  };

  const handleFinalize = async (mergedData) => {
    setIsFinalizing(true);
    try {
      const response = await fetchWithCsrf('/api/import/finalize', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(mergedData),
      });

      if (!response.ok) throw new Error(t('import.finalizeError'));

      const finalizedBook = await response.json();
      navigate(`/book/${finalizedBook.id}`, { 
        state: { notification: { type: 'success', message: t('import.success', { title: finalizedBook.title }) } } 
      });
    } catch (err) {
      setNotification({ type: 'error', message: err.message });
      setIsFinalizing(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    if (name === 'labels') {
      setLabelsString(value);
      const labelsArray = value.split(',').map(label => label.trim()).filter(label => label !== '');
      setBook(prev => ({ ...prev, labels: labelsArray }));
    } else {
      setBook(prev => ({ ...prev, [name]: value }));
    }
  };

  const handleAuthorChange = (index, selectedAuthorId) => {
    const selectedAuthor = authors.find(author => author.id === selectedAuthorId);
    setBook(prev => {
      const newAuthors = [...prev.authors];
      if (selectedAuthor) newAuthors[index] = selectedAuthor;
      return { ...prev, authors: newAuthors };
    });
  };

  const handleAddAuthorField = () => {
    setBook(prev => ({
      ...prev,
      authors: [...prev.authors, { id: '', firstName: '', lastName: '' }]
    }));
  };

  const handleRemoveAuthorField = (index) => {
    setBook(prev => {
      const newAuthors = [...prev.authors];
      newAuthors.splice(index, 1);
      return { ...prev, authors: newAuthors };
    });
  };

  const handleSeriesChange = (selectedSeriesId) => {
    const selectedSeries = series.find(s => s.id === selectedSeriesId);
    setBook(prev => ({
      ...prev,
      series: selectedSeries || null,
      volume: selectedSeries ? prev.volume : ''
    }));
  };

  const handleSave = () => {
    const bookData = { ...book };
    bookData.authorIds = book.authors.map(a => a.id).filter(id => !!id);
    delete bookData.authors;
    if (bookData.series) {
      bookData.seriesId = bookData.series.id;
      delete bookData.series;
    }
    mutate(bookData);
  };

  const authorOptions = useMemo(() => authors.map(a => ({ id: a.id, name: `${a.firstName} ${a.lastName}` })), [authors]);
  const seriesOptions = useMemo(() => series.map(s => ({ id: s.id, name: s.title })), [series]);

  const getFilteredAuthorOptions = (currentIndex) => {
    const selectedIds = book.authors.filter((_, i) => i !== currentIndex).map(a => a.id).filter(id => !!id);
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
    const originalAuthorIds = originalBook.authors?.map(a => a.id).sort().join(',') || '';
    const currentAuthorIds = book.authors?.filter(a => !!a.id).map(a => a.id).sort().join(',') || '';
    if (originalAuthorIds !== currentAuthorIds) return true;
    if (normalize(originalBook.series?.id) !== normalize(book.series?.id)) return true;
    const originalLabels = originalBook.labels?.sort().join(',') || '';
    const currentLabels = book.labels?.sort().join(',') || '';
    if (originalLabels !== currentLabels) return true;
    return false;
  };

  if (loading) return <AddPage title={t(isEditMode ? 'addBook.editTitle' : 'addBook.title')}><p>{t('common.loading')}</p></AddPage>;

  return (
    <AddPage title={t(isEditMode ? 'addBook.editTitle' : 'addBook.title')} notification={notification} setNotification={setNotification}>
      
      {!isEditMode && !stagedUpload && (
        <div className="mb-8 bg-blue-50 p-6 rounded-xl border border-blue-100 shadow-sm">
          <div className="flex items-center gap-3 mb-4">
            <FaFileImport className="text-2xl text-blue-600" />
            <h2 className="text-lg font-bold text-gray-800">{t('import.title')}</h2>
          </div>
          
          <form 
            className={`relative group border-2 border-dashed rounded-xl p-8 transition-all flex flex-col items-center justify-center text-center
              ${dragActive ? 'border-blue-500 bg-blue-100' : 'border-blue-200 bg-white hover:border-blue-400'}`}
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
          >
            <input
              type="file"
              id="file-upload"
              className="absolute inset-0 w-full h-full opacity-0 cursor-pointer disabled:cursor-not-allowed"
              onChange={onFileSelect}
              accept=".epub,.pdf,.mobi,.azw3"
              disabled={isUploading}
            />
            
            <FaCloudUploadAlt className={`text-4xl mb-2 ${isUploading ? 'animate-bounce text-blue-600' : 'text-blue-400'}`} />
            <div className="text-sm">
              <p className="font-bold text-gray-700">
                {isUploading ? t('import.uploading') : t('import.dropZoneTitle')}
              </p>
              <p className="text-gray-500">{t('import.dropZoneSubtitle')}</p>
            </div>
          </form>
        </div>
      )}

      {!isEditMode && stagedUpload && (
        <div className="mb-8 bg-green-50 p-4 rounded-lg border border-green-200 flex justify-between items-center shadow-sm animate-fade-in">
          <div className="flex items-center gap-3">
            <div className="bg-green-500 text-white p-2 rounded-full">
              <FaFileImport />
            </div>
            <div>
              <p className="font-bold text-green-800">File attached: {stagedUpload.fileName}</p>
              <p className="text-xs text-green-600">Form populated from extracted metadata. You can still review and edit below.</p>
            </div>
          </div>
          <button 
            onClick={() => {
              setStagedUpload(null);
              // We don't reset the form fields so the user doesn't lose manual edits
            }}
            className="text-sm bg-white hover:bg-red-50 text-red-600 border border-red-200 px-3 py-1 rounded shadow-sm transition-colors"
          >
            Remove file
          </button>
        </div>
      )}

      <Form onSave={handleSave} onCancel={() => navigate(isEditMode ? `/book/${id}` : '/')} isSaveDisabled={!book.title.trim() || isSaving || (isEditMode && !hasChanges())}>
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
              <button type="button" onClick={() => handleRemoveAuthorField(index)} className="bg-red-100 text-red-700 hover:bg-red-700 hover:text-white font-bold py-2 px-3 rounded h-fit">✖</button>
            </div>
          ))}
          <button type="button" onClick={handleAddAuthorField} className="bg-blue-100 text-blue-700 hover:bg-blue-700 hover:text-white font-bold py-1 px-3 rounded text-sm">+ {t('addBook.form.addAuthor')}</button>
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
          <input type="number" id="volume" name="volume" value={book.volume || ''} onChange={handleChange} disabled={!book.series} className={`shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline ${!book.series ? 'bg-gray-100 cursor-not-allowed' : ''}`} />
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
          <input type="text" id="labels" name="labels" value={labelsString} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" placeholder={t('addBook.form.labelsPlaceholder')} />
        </div>
      </Form>

      {stagedUpload && (
        <ImportReviewDialog
          stagedUpload={stagedUpload}
          authorOptions={authorOptions}
          seriesOptions={seriesOptions}
          onCancel={() => setStagedUpload(null)}
          onConfirm={handleFinalize}
          isProcessing={isFinalizing}
        />
      )}
    </AddPage>
  );
};

export default AddBook;
