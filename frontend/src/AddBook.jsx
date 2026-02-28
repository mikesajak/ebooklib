import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { 
  FaCloudUploadAlt, FaFileImport, FaCheckDouble, FaUserTag, 
  FaImage, FaSearch, FaPencilAlt, FaInfoCircle, FaBook, FaLayerGroup, FaTags 
} from 'react-icons/fa';
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
  const [dirtyFields, setDirtyFields] = useState(new Set());

  const markDirty = (fieldName) => {
    setDirtyFields(prev => {
      const next = new Set(prev);
      next.add(fieldName);
      return next;
    });
  };

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
        // Intelligent Author Mapping
        const extractedAuthorNames = data.metadata.authors || [];
        const resolvedAuthors = extractedAuthorNames.map(name => {
          const normalizedName = name.toLowerCase().trim();
          // Find existing author that matches the extracted name (simple heuristic)
          const match = authors.find(a => 
            `${a.firstName} ${a.lastName}`.toLowerCase().trim() === normalizedName ||
            `${a.lastName} ${a.firstName}`.toLowerCase().trim() === normalizedName
          );
          return match || { id: '', firstName: '', lastName: name }; // Fallback to raw name if no ID match
        });

        setBook(prev => ({
          ...prev,
          title: (!dirtyFields.has('title') && data.metadata.title) ? data.metadata.title : prev.title,
          authors: (resolvedAuthors.length > 0 && !dirtyFields.has('authors')) ? resolvedAuthors : prev.authors,
          publisher: (!dirtyFields.has('publisher') && data.metadata.publisher) ? data.metadata.publisher : prev.publisher,
          publicationDate: (!dirtyFields.has('publicationDate') && data.metadata.publicationDate) ? data.metadata.publicationDate.split('T')[0] : prev.publicationDate,
          description: (!dirtyFields.has('description') && data.metadata.description) ? data.metadata.description : prev.description,
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
      // Split authors into IDs and Names for the backend
      const authorIds = mergedData.authorIds?.filter(id => !!id) || [];
      const authorNames = mergedData.authorNames || [];

      const response = await fetchWithCsrf('/api/import/finalize', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ...mergedData,
          authorIds: authorIds,
          authorNames: authorNames
        }),
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
    markDirty(name);
    if (name === 'labels') {
      setLabelsString(value);
      const labelsArray = value.split(',').map(label => label.trim()).filter(label => label !== '');
      setBook(prev => ({ ...prev, labels: labelsArray }));
    } else {
      setBook(prev => ({ ...prev, [name]: value }));
    }
  };

  const handleAuthorChange = (index, selectedAuthorId) => {
    markDirty('authors');
    const selectedAuthor = authors.find(author => author.id === selectedAuthorId);
    setBook(prev => {
      const newAuthors = [...prev.authors];
      if (selectedAuthor) newAuthors[index] = selectedAuthor;
      return { ...prev, authors: newAuthors };
    });
  };

  const handleAddAuthorField = () => {
    markDirty('authors');
    setBook(prev => ({
      ...prev,
      authors: [...prev.authors, { id: '', firstName: '', lastName: '' }]
    }));
  };

  const handleRemoveAuthorField = (index) => {
    markDirty('authors');
    setBook(prev => {
      const newAuthors = [...prev.authors];
      newAuthors.splice(index, 1);
      return { ...prev, authors: newAuthors };
    });
  };

  const handleSeriesChange = (selectedSeriesId) => {
    markDirty('series');
    const selectedSeries = series.find(s => s.id === selectedSeriesId);
    setBook(prev => ({
      ...prev,
      series: selectedSeries || null,
      volume: selectedSeries ? prev.volume : ''
    }));
  };

  const handleSave = () => {
    const bookData = { ...book };
    
    // Split authors into existing IDs and new names
    bookData.authorIds = book.authors.filter(a => !!a.id).map(a => a.id);
    bookData.authorNames = book.authors.filter(a => !a.id && (a.firstName || a.lastName))
                                     .map(a => `${a.firstName} ${a.lastName}`.trim());
    
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

  const SectionHeader = ({ icon: Icon, title, description }) => (
    <div className="mb-4">
      <div className="flex items-center gap-2 text-indigo-900 mb-1">
        <Icon className="text-indigo-500" />
        <h3 className="font-extrabold uppercase text-xs tracking-widest">{title}</h3>
      </div>
      {description && <p className="text-xs text-gray-500">{description}</p>}
    </div>
  );

  const InputCard = ({ children, isDirty }) => (
    <div className={`p-4 rounded-xl border-2 transition-all shadow-sm mb-6 ${isDirty ? 'bg-yellow-50 border-yellow-200 ring-1 ring-yellow-100' : 'bg-white border-gray-100'}`}>
      {children}
    </div>
  );

  if (loading) return <AddPage title={t(isEditMode ? 'addBook.editTitle' : 'addBook.title')}><p>{t('common.loading')}</p></AddPage>;

  return (
    <AddPage title={t(isEditMode ? 'addBook.editTitle' : 'addBook.title')} notification={notification} setNotification={setNotification}>
      
      {!isEditMode && !stagedUpload && (
        <div className="mb-6 bg-indigo-50 p-4 rounded-2xl border border-indigo-100 shadow-sm">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <FaFileImport className="text-xl text-indigo-600" />
              <h2 className="text-base font-bold text-gray-800">{t('import.title')}</h2>
            </div>
          </div>
          
          <form 
            className={`relative group border-2 border-dashed rounded-xl p-4 transition-all flex flex-col items-center justify-center text-center
              ${dragActive ? 'border-indigo-500 bg-indigo-100' : 'border-indigo-200 bg-white hover:border-indigo-400 shadow-inner'}`}
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
            
            <div className="flex items-center gap-4">
              <FaCloudUploadAlt className={`text-3xl ${isUploading ? 'animate-bounce text-indigo-600' : 'text-indigo-400'}`} />
              <div className="text-left">
                <p className="font-bold text-gray-700 text-sm leading-none">
                  {isUploading ? t('import.uploading') : t('import.dropZoneTitle')}
                </p>
                <p className="text-[11px] text-gray-500 mt-1">{t('import.dropZoneSubtitle')}</p>
              </div>
            </div>
          </form>

          <div className="grid grid-cols-2 lg:grid-cols-4 gap-2 mt-4">
            <div className="bg-white p-2 rounded-lg border border-indigo-50 flex items-center gap-2">
              <FaCheckDouble className="text-blue-500 text-xs" />
              <span className="text-[10px] font-bold text-gray-500 uppercase tracking-tighter">Metadata</span>
            </div>
            <div className="bg-white p-2 rounded-lg border border-indigo-50 flex items-center gap-2">
              <FaUserTag className="text-green-500 text-xs" />
              <span className="text-[10px] font-bold text-gray-500 uppercase tracking-tighter">Authors</span>
            </div>
            <div className="bg-white p-2 rounded-lg border border-indigo-50 flex items-center gap-2">
              <FaImage className="text-purple-500 text-xs" />
              <span className="text-[10px] font-bold text-gray-500 uppercase tracking-tighter">Covers</span>
            </div>
            <div className="bg-white p-2 rounded-lg border border-indigo-50 flex items-center gap-2">
              <FaSearch className="text-orange-500 text-xs" />
              <span className="text-[10px] font-bold text-gray-500 uppercase tracking-tighter">Matching</span>
            </div>
          </div>
        </div>
      )}

      {!isEditMode && stagedUpload && (
        <div className="mb-10 bg-green-50 p-5 rounded-2xl border-2 border-green-200 flex justify-between items-center shadow-md animate-fade-in">
          <div className="flex items-center gap-4">
            <div className="bg-green-500 text-white p-3 rounded-xl shadow-sm">
              <FaFileImport className="text-xl" />
            </div>
            <div>
              <p className="font-extrabold text-green-900">File attached: {stagedUpload.fileName}</p>
              <p className="text-sm text-green-700">Form populated from extracted metadata. You can still review and edit below.</p>
            </div>
          </div>
          <button 
            onClick={() => setStagedUpload(null)}
            className="text-sm bg-white hover:bg-red-50 text-red-600 border border-red-200 px-4 py-2 rounded-xl font-bold shadow-sm transition-all"
          >
            Remove file
          </button>
        </div>
      )}

      <Form onSave={handleSave} onCancel={() => navigate(isEditMode ? `/book/${id}` : '/')} isSaveDisabled={!book.title.trim() || isSaving || (isEditMode && !hasChanges())}>
        
        <SectionHeader icon={FaBook} title="Basic Information" description="The core details of your book entry." />
        <InputCard isDirty={dirtyFields.has('title')}>
          <div className="flex items-center gap-2 mb-2">
            <label className="block text-gray-700 text-sm font-bold" htmlFor="title">
              {t('addBook.form.title')}
            </label>
            {dirtyFields.has('title') && <FaPencilAlt className="text-yellow-600 text-[10px]" title="Manual Edit" />}
          </div>
          <input type="text" id="title" name="title" value={book.title} onChange={handleChange} className={`shadow-sm appearance-none border rounded-lg w-full py-2.5 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all ${dirtyFields.has('title') ? 'border-yellow-300' : 'border-gray-300'}`} />
        </InputCard>
        
        <SectionHeader icon={FaLayerGroup} title="Authors & Series" description="Link authors and organize into series." />
        <InputCard isDirty={dirtyFields.has('authors') || dirtyFields.has('series') || dirtyFields.has('volume')}>
          <div className="mb-6">
            <div className="flex items-center gap-2 mb-2">
              <label className="block text-gray-700 text-sm font-bold">{t('addBook.form.author')}</label>
              {dirtyFields.has('authors') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
            </div>
            <div className="space-y-3">
              {book.authors.map((author, index) => (
                <div key={index} className="flex gap-2 items-start group">
                  <div className="flex-grow">
                    <SearchableDropdown
                      id={`author-${index}`}
                      options={getFilteredAuthorOptions(index)}
                      value={author.id}
                      onChange={(id) => handleAuthorChange(index, id)}
                      placeholder={t('addBook.form.selectAuthor')}
                    />
                    {!author.id && author.lastName && (
                      <p className="text-[10px] text-green-600 font-bold mt-1 ml-1 flex items-center gap-1">
                        <FaFileAlt /> Will be created: {author.firstName} {author.lastName}
                      </p>
                    )}
                  </div>
                  <button type="button" onClick={() => handleRemoveAuthorField(index)} className="bg-red-50 text-red-400 hover:bg-red-500 hover:text-white font-bold p-2.5 rounded-lg transition-colors">✖</button>
                </div>
              ))}
              <button type="button" onClick={handleAddAuthorField} className="bg-indigo-50 text-indigo-700 hover:bg-indigo-600 hover:text-white font-bold py-2 px-4 rounded-xl text-xs transition-all flex items-center gap-2 shadow-sm border border-indigo-100">
                + {t('addBook.form.addAuthor')}
              </button>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <div className="flex items-center gap-2 mb-2">
                <label className="block text-gray-700 text-sm font-bold">{t('addBook.form.series')}</label>
                {dirtyFields.has('series') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
              </div>
              <SearchableDropdown
                id="series"
                options={seriesOptions}
                value={book.series?.id || ''}
                onChange={handleSeriesChange}
                placeholder={t('addBook.form.selectSeries')}
              />
            </div>
            <div>
              <div className="flex items-center gap-2 mb-2">
                <label className="block text-gray-700 text-sm font-bold" htmlFor="volume">{t('addBook.form.volume')}</label>
                {dirtyFields.has('volume') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
              </div>
              <input 
                type="number" id="volume" name="volume" value={book.volume || ''} onChange={handleChange} 
                disabled={!book.series} 
                className={`shadow-sm appearance-none border rounded-lg w-full py-2.5 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all ${!book.series ? 'bg-gray-100 cursor-not-allowed border-gray-200' : 'bg-white border-gray-300'}`} 
              />
            </div>
          </div>
        </InputCard>

        <SectionHeader icon={FaInfoCircle} title="Metadata Details" description="Publication and additional cataloging data." />
        <InputCard isDirty={dirtyFields.has('publicationDate') || dirtyFields.has('publisher') || dirtyFields.has('description') || dirtyFields.has('labels')}>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
            <div>
              <div className="flex items-center gap-2 mb-2">
                <label className="block text-gray-700 text-sm font-bold" htmlFor="publicationDate">{t('addBook.form.publicationDate')}</label>
                {dirtyFields.has('publicationDate') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
              </div>
              <input type="date" id="publicationDate" name="publicationDate" value={book.publicationDate || ''} onChange={handleChange} className="shadow-sm appearance-none border border-gray-300 rounded-lg w-full py-2.5 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" />
            </div>
            <div>
              <div className="flex items-center gap-2 mb-2">
                <label className="block text-gray-700 text-sm font-bold" htmlFor="publisher">{t('addBook.form.publisher')}</label>
                {dirtyFields.has('publisher') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
              </div>
              <input type="text" id="publisher" name="publisher" value={book.publisher || ''} onChange={handleChange} className="shadow-sm appearance-none border border-gray-300 rounded-lg w-full py-2.5 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" />
            </div>
          </div>

          <div className="mb-6">
            <div className="flex items-center gap-2 mb-2">
              <label className="block text-gray-700 text-sm font-bold" htmlFor="description">{t('addBook.form.description')}</label>
              {dirtyFields.has('description') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
            </div>
            <textarea id="description" name="description" value={book.description || ''} onChange={handleChange} rows="4" className="shadow-sm appearance-none border border-gray-300 rounded-lg w-full py-2.5 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" />
          </div>

          <div>
            <div className="flex items-center gap-2 mb-2">
              <label className="block text-gray-700 text-sm font-bold" htmlFor="labels">{t('addBook.form.labels')}</label>
              {dirtyFields.has('labels') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
            </div>
            <div className="flex items-center gap-3">
              <div className="bg-gray-100 p-3 rounded-lg text-gray-400"><FaTags /></div>
              <input type="text" id="labels" name="labels" value={labelsString} onChange={handleChange} className="shadow-sm appearance-none border border-gray-300 rounded-lg w-full py-2.5 px-3 text-gray-700 leading-tight focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" placeholder={t('addBook.form.labelsPlaceholder')} />
            </div>
            <p className="text-[10px] text-gray-400 mt-2 italic ml-12">Separate labels with commas (e.g. Fiction, Fantasy, Hardcover)</p>
          </div>
        </InputCard>
      </Form>

      {stagedUpload && (
        <ImportReviewDialog
          stagedUpload={stagedUpload}
          draftBook={book}
          dirtyFields={dirtyFields}
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
