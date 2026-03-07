import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { 
  FaCloudUploadAlt, FaFileImport, FaCheckDouble, FaUserTag, 
  FaImage, FaSearch, FaPencilAlt, FaInfoCircle, FaBook, FaLayerGroup, FaTags, FaFileAlt, FaCheck 
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
      {description && <p className="text-[10px] text-gray-400 font-bold uppercase tracking-wider">{description}</p>}
    </div>
  );

  const InputCard = ({ children, isDirty }) => (
    <div className={`p-6 rounded-2xl border-2 transition-all shadow-sm mb-10 ${isDirty ? 'bg-yellow-50 border-yellow-200 ring-4 ring-yellow-50' : 'bg-white border-gray-100'}`}>
      {children}
    </div>
  );

  if (loading) return <AddPage title={t(isEditMode ? 'addBook.editTitle' : 'addBook.title')}><p>{t('common.loading')}</p></AddPage>;

  return (
    <AddPage title={t(isEditMode ? 'addBook.editTitle' : 'addBook.title')} notification={notification} setNotification={setNotification}>
      
      {!isEditMode && !stagedUpload && (
        <div className="mb-10 bg-indigo-50/50 p-6 rounded-[2.5rem] border border-indigo-100 shadow-inner">
          <div className="flex items-center justify-between mb-4 px-2">
            <div className="flex items-center gap-3">
              <div className="bg-indigo-600 text-white p-2 rounded-lg shadow-md">
                <FaFileImport size={18} />
              </div>
              <h2 className="text-sm font-black text-indigo-900 uppercase tracking-widest">{t('import.title')}</h2>
            </div>
          </div>
          
          <form 
            className={`relative group border-2 border-dashed rounded-3xl p-8 transition-all flex flex-col items-center justify-center text-center
              ${dragActive ? 'border-indigo-500 bg-indigo-100 shadow-2xl' : 'border-indigo-200 bg-white hover:border-indigo-400 shadow-sm'}`}
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
            
            <div className="flex flex-col items-center gap-4">
              <div className={`p-5 rounded-full transition-all ${isUploading ? 'bg-indigo-600 text-white shadow-xl shadow-indigo-200' : 'bg-indigo-50 text-indigo-400 group-hover:bg-indigo-100'}`}>
                <FaCloudUploadAlt size={40} className={isUploading ? 'animate-bounce' : ''} />
              </div>
              <div>
                <p className="font-black text-gray-800 text-lg tracking-tight">
                  {isUploading ? t('import.uploading') : t('import.dropZoneTitle')}
                </p>
                <p className="text-xs text-gray-400 font-bold mt-1 uppercase tracking-tighter">{t('import.dropZoneSubtitle')}</p>
              </div>
            </div>
          </form>

          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mt-6">
            {[
              { icon: FaCheckDouble, label: t('addBook.form.metadata'), color: 'text-blue-500' },
              { icon: FaUserTag, label: t('addBook.form.authors'), color: 'text-emerald-500' },
              { icon: FaImage, label: t('addBook.form.covers'), color: 'text-purple-500' },
              { icon: FaSearch, label: t('addBook.form.matching'), color: 'text-orange-500' }
            ].map((feature, i) => (
              <div key={i} className="bg-white/80 backdrop-blur-sm p-3 rounded-2xl border border-indigo-50 flex items-center gap-3 shadow-sm">
                <feature.icon className={`${feature.color} text-sm`} />
                <span className="text-[10px] font-black text-gray-500 uppercase tracking-widest">{feature.label}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {!isEditMode && stagedUpload && (
        <div className="mb-12 bg-emerald-50/50 p-6 rounded-[2.5rem] border-2 border-emerald-100 flex justify-between items-center shadow-xl shadow-emerald-100/20 animate-fade-in">
          <div className="flex items-center gap-6">
            <div className="bg-emerald-600 text-white p-4 rounded-2xl shadow-lg shadow-emerald-100">
              <FaFileImport size={24} />
            </div>
            <div>
              <p className="font-black text-emerald-900 text-lg tracking-tight">File: {stagedUpload.fileName}</p>
              <p className="text-xs text-emerald-600 font-bold uppercase tracking-wider mt-1">{t('addBook.form.extractedHint')}</p>
            </div>
          </div>
          <button 
            onClick={() => setStagedUpload(null)}
            className="px-6 py-3 bg-white hover:bg-rose-50 text-rose-600 border border-rose-100 rounded-xl font-black text-[10px] uppercase tracking-widest shadow-sm transition-all transform active:scale-95"
          >
            {t('common.remove')}
          </button>
        </div>
      )}

      <Form onSave={handleSave} onCancel={() => navigate(isEditMode ? `/book/${id}` : '/')} isSaveDisabled={!book.title.trim() || isSaving || (isEditMode && !hasChanges())}>
        
        <SectionHeader icon={FaBook} title={t('addBook.form.metadata')} description="The core details of your book entry." />
        <InputCard isDirty={dirtyFields.has('title')}>
          <div className="flex items-center gap-2 mb-3">
            <label className="block text-gray-700 text-xs font-black uppercase tracking-widest" htmlFor="title">
              {t('addBook.form.title')}
            </label>
            {dirtyFields.has('title') && <FaPencilAlt className="text-yellow-600 text-[10px]" title="Manual Edit" />}
          </div>
          <input type="text" id="title" name="title" value={book.title} onChange={handleChange} className={`shadow-sm appearance-none border-2 rounded-xl w-full py-3 px-4 text-gray-700 leading-tight font-bold focus:outline-none focus:ring-4 focus:ring-indigo-500/10 focus:border-indigo-500 transition-all ${dirtyFields.has('title') ? 'border-yellow-300' : 'border-gray-100 bg-gray-50/30'}`} />
        </InputCard>
        
        <SectionHeader icon={FaLayerGroup} title={t('addBook.form.authors')} description="Link authors and organize into series." />
        <InputCard isDirty={dirtyFields.has('authors') || dirtyFields.has('series') || dirtyFields.has('volume')}>
          <div className="mb-8">
            <div className="flex items-center gap-2 mb-3">
              <label className="block text-gray-700 text-xs font-black uppercase tracking-widest">{t('addBook.form.author')}</label>
              {dirtyFields.has('authors') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
            </div>
            <div className="space-y-4">
              {book.authors.map((author, index) => (
                <div key={index} className="flex gap-3 items-start group animate-fade-in">
                  <div className="flex-grow">
                    <SearchableDropdown
                      id={`author-${index}`}
                      options={getFilteredAuthorOptions(index)}
                      value={author.id}
                      onChange={(id) => handleAuthorChange(index, id)}
                      placeholder={t('addBook.form.selectAuthor')}
                    />
                    {!author.id && author.lastName && (
                      <p className="text-[10px] text-emerald-600 font-black uppercase tracking-tighter mt-2 ml-2 flex items-center gap-1">
                        <FaFileAlt size={10} /> Will be created: {author.firstName} {author.lastName}
                      </p>
                    )}
                  </div>
                  <button type="button" onClick={() => handleRemoveAuthorField(index)} className="bg-rose-50 text-rose-400 hover:bg-rose-500 hover:text-white font-black p-3.5 rounded-xl transition-all shadow-sm">✖</button>
                </div>
              ))}
              <button type="button" onClick={handleAddAuthorField} className="bg-indigo-50 text-indigo-700 hover:bg-indigo-600 hover:text-white font-black py-3 px-8 rounded-xl text-[10px] uppercase tracking-[0.2em] transition-all flex items-center gap-2 shadow-sm border border-indigo-100 transform active:scale-95 mt-2">
                <FaPlus /> {t('addBook.form.addAuthor')}
              </button>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div>
              <div className="flex items-center gap-2 mb-3">
                <label className="block text-gray-700 text-xs font-black uppercase tracking-widest">{t('addBook.form.series')}</label>
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
              <div className="flex items-center gap-2 mb-3">
                <label className="block text-gray-700 text-xs font-black uppercase tracking-widest" htmlFor="volume">{t('addBook.form.volume')}</label>
                {dirtyFields.has('volume') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
              </div>
              <input 
                type="number" id="volume" name="volume" value={book.volume || ''} onChange={handleChange} 
                disabled={!book.series} 
                className={`shadow-sm appearance-none border-2 rounded-xl w-full py-3 px-4 text-gray-700 leading-tight font-bold focus:outline-none focus:ring-4 focus:ring-indigo-500/10 focus:border-indigo-500 transition-all ${!book.series ? 'bg-gray-100 cursor-not-allowed border-gray-200' : 'bg-gray-50/30 border-gray-100'}`} 
              />
            </div>
          </div>
        </InputCard>

        <SectionHeader icon={FaInfoCircle} title={t('addBook.section.cataloging', 'Cataloging Details')} description="Publication and additional cataloging data." />
        <InputCard isDirty={dirtyFields.has('publicationDate') || dirtyFields.has('publisher') || dirtyFields.has('description') || dirtyFields.has('labels')}>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8">
            <div>
              <div className="flex items-center gap-2 mb-3">
                <label className="block text-gray-700 text-xs font-black uppercase tracking-widest" htmlFor="publicationDate">{t('addBook.form.publicationDate')}</label>
                {dirtyFields.has('publicationDate') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
              </div>
              <input type="date" id="publicationDate" name="publicationDate" value={book.publicationDate || ''} onChange={handleChange} className="shadow-sm appearance-none border-2 border-gray-100 bg-gray-50/30 rounded-xl w-full py-3 px-4 text-gray-700 leading-tight font-bold focus:outline-none focus:ring-4 focus:ring-indigo-500/10 focus:border-indigo-500 transition-all" />
            </div>
            <div>
              <div className="flex items-center gap-2 mb-3">
                <label className="block text-gray-700 text-xs font-black uppercase tracking-widest" htmlFor="publisher">{t('addBook.form.publisher')}</label>
                {dirtyFields.has('publisher') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
              </div>
              <input type="text" id="publisher" name="publisher" value={book.publisher || ''} onChange={handleChange} className="shadow-sm appearance-none border-2 border-gray-100 bg-gray-50/30 rounded-xl w-full py-3 px-4 text-gray-700 leading-tight font-bold focus:outline-none focus:ring-4 focus:ring-indigo-500/10 focus:border-indigo-500 transition-all" />
            </div>
          </div>

          <div className="mb-8">
            <div className="flex items-center gap-2 mb-3">
              <label className="block text-gray-700 text-xs font-black uppercase tracking-widest" htmlFor="description">{t('addBook.form.description')}</label>
              {dirtyFields.has('description') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
            </div>
            <textarea id="description" name="description" value={book.description || ''} onChange={handleChange} rows="4" className="shadow-sm appearance-none border-2 border-gray-100 bg-gray-50/30 rounded-2xl w-full py-3 px-4 text-gray-700 leading-tight font-medium focus:outline-none focus:ring-4 focus:ring-indigo-500/10 focus:border-indigo-500 transition-all" />
          </div>

          <div>
            <div className="flex items-center gap-2 mb-3">
              <label className="block text-gray-700 text-xs font-black uppercase tracking-widest" htmlFor="labels">{t('addBook.form.labels')}</label>
              {dirtyFields.has('labels') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
            </div>
            <div className="flex items-center gap-4">
              <div className="bg-indigo-50 p-4 rounded-xl text-indigo-400 shadow-inner"><FaTags /></div>
              <input type="text" id="labels" name="labels" value={labelsString} onChange={handleChange} className="shadow-sm appearance-none border-2 border-gray-100 bg-gray-50/30 rounded-xl w-full py-3 px-4 text-gray-700 leading-tight font-bold focus:outline-none focus:ring-4 focus:ring-indigo-500/10 focus:border-indigo-500 transition-all" placeholder={t('addBook.form.labelsPlaceholder')} />
            </div>
            <p className="text-[10px] text-gray-400 mt-3 font-bold uppercase tracking-widest ml-[4.5rem]">{t('addBook.form.labelHelp')}</p>
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
