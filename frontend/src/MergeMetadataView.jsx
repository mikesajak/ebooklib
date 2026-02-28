import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import SearchableDropdown from './SearchableDropdown';

const MergeMetadataView = ({
  stagedUpload,
  existingBook,
  authorOptions,
  seriesOptions,
  onMergedDataChange
}) => {
  const { t } = useTranslation();
  const extracted = stagedUpload.metadata || {};
  const validation = extracted.validation || {};

  const resolveInitialAuthors = () => {
    if (existingBook) {
      return {
        authorIds: existingBook.authors.map(a => a.id),
        authorNames: []
      };
    }
    
    // For new books, try to resolve extracted names to existing IDs
    const extractedNames = extracted.authors || [];
    const ids = [];
    const names = [];
    
    extractedNames.forEach(name => {
      const normalizedName = name.toLowerCase().trim();
      const match = authorOptions.find(opt => opt.name.toLowerCase().trim() === normalizedName);
      if (match) ids.push(match.id);
      else names.push(name);
    });
    
    return { authorIds: ids, authorNames: names };
  };

  const initialAuthors = resolveInitialAuthors();

  const [mergedData, setMergedData] = useState({
    title: extracted.title || (existingBook ? existingBook.title : ''),
    authorIds: initialAuthors.authorIds,
    authorNames: initialAuthors.authorNames,
    publisher: extracted.publisher || (existingBook ? existingBook.publisher : ''),
    publicationDate: extracted.publicationDate || (existingBook ? (existingBook.publicationDate ? existingBook.publicationDate.split('T')[0] : '') : ''),
    description: extracted.description || (existingBook ? existingBook.description : ''),
    seriesId: existingBook ? existingBook.series?.id : null,
    volume: existingBook ? existingBook.volume : null,
    labels: existingBook ? existingBook.labels : [],
    updateCover: false
  });

  const [selectedSources, setSelectedIds] = useState({
    title: extracted.title && (!existingBook || extracted.title !== existingBook.title) ? 'extracted' : 'existing',
    publisher: extracted.publisher && (!existingBook || extracted.publisher !== existingBook.publisher) ? 'extracted' : 'existing',
    publicationDate: extracted.publicationDate && (!existingBook || extracted.publicationDate !== existingBook.publicationDate) ? 'extracted' : 'existing',
    description: extracted.description && (!existingBook || extracted.description !== existingBook.description) ? 'extracted' : 'existing',
    cover: false
  });

  useEffect(() => {
    onMergedDataChange(mergedData);
  }, [mergedData, onMergedDataChange]);

  const toggleField = (field, source) => {
    setSelectedIds(prev => ({ ...prev, [field]: source }));
    
    let value;
    if (source === 'extracted') {
      value = extracted[field];
    } else {
      value = existingBook ? existingBook[field] : '';
      if (field === 'publicationDate' && value) value = value.split('T')[0];
    }

    setMergedData(prev => ({ ...prev, [field]: value }));
  };

  const handleAuthorChange = (index, selectedAuthorId) => {
    setMergedData(prev => {
      const newAuthorIds = [...prev.authorIds];
      if (selectedAuthorId) {
        newAuthorIds[index] = selectedAuthorId;
      }
      return { ...prev, authorIds: newAuthorIds };
    });
  };

  const handleAddAuthorField = () => {
    setMergedData(prev => ({
      ...prev,
      authorIds: [...prev.authorIds, '']
    }));
  };

  const handleRemoveAuthorField = (index) => {
    setMergedData(prev => {
      const newAuthorIds = [...prev.authorIds];
      newAuthorIds.splice(index, 1);
      return { ...prev, authorIds: newAuthorIds };
    });
  };

  const handleRemoveNewAuthor = (index) => {
    setMergedData(prev => {
      const newNames = [...prev.authorNames];
      newNames.splice(index, 1);
      return { ...prev, authorNames: newNames };
    });
  };

  const handleSeriesChange = (selectedSeriesId) => {
    setMergedData(prev => ({
      ...prev,
      seriesId: selectedSeriesId || null,
      volume: selectedSeriesId ? prev.volume : null
    }));
  };

  const handleVolumeChange = (e) => {
    const val = e.target.value;
    setMergedData(prev => ({
      ...prev,
      volume: val ? parseInt(val, 10) : null
    }));
  };

  const handleCoverToggle = () => {
    const newValue = !selectedSources.cover;
    setSelectedIds(prev => ({ ...prev, cover: newValue }));
    setMergedData(prev => ({ ...prev, updateCover: newValue }));
  };

  const FieldComparison = ({ label, field, existingValue, extractedValue }) => {
    const currentSource = selectedSources[field];

    return (
      <div className="mb-4 border-b pb-2">
        <label className="block text-sm font-bold text-gray-700 mb-1">{label}</label>
        <div className="flex gap-4">
          <div 
            className={`flex-1 p-2 rounded border cursor-pointer transition-colors ${currentSource === 'existing' ? 'bg-blue-100 border-blue-500' : 'bg-gray-50 border-gray-200'}`}
            onClick={() => toggleField(field, 'existing')}
          >
            <div className="text-xs text-gray-500 uppercase">{t('import.review.existingValue')}</div>
            <div className={`${!existingValue ? 'italic text-gray-400' : ''}`}>
              {existingValue || t('common.na')}
            </div>
          </div>

          {extractedValue && (
            <div 
              className={`flex-1 p-2 rounded border cursor-pointer transition-colors ${currentSource === 'extracted' ? 'bg-green-100 border-green-500' : 'bg-gray-50 border-gray-200'}`}
              onClick={() => toggleField(field, 'extracted')}
            >
              <div className="text-xs text-gray-500 uppercase">{t('import.review.extractedValue')}</div>
              <div>{extractedValue}</div>
            </div>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="merge-metadata-view max-h-[60vh] overflow-y-auto pr-2">
      <div className="mb-6 p-3 bg-yellow-50 border-l-4 border-yellow-400 text-sm">
        <p className="font-bold">{t('import.review.statusTitle')}:</p>
        {existingBook ? (
          <p>
            {validation.titleMatch && validation.authorMatch 
              ? t('import.review.statusMatch') 
              : t('import.review.statusMismatch')}
          </p>
        ) : (
          <p>{t('import.review.statusNewBook')}</p>
        )}
      </div>

      <FieldComparison 
        label={t('addBook.form.title')} 
        field="title" 
        existingValue={existingBook?.title} 
        extractedValue={extracted.title} 
      />

      <div className="mb-4 border-b pb-4">
        <label className="block text-sm font-bold text-gray-700 mb-2">{t('addBook.form.author')}</label>
        <div className="flex flex-col md:flex-row gap-4">
          <div className="flex-1">
            <div className="text-xs text-gray-500 uppercase mb-2">{t('import.review.extractedValue')}</div>
            <div className="p-2 bg-gray-50 rounded border border-gray-200 min-h-[40px]">
              {extracted.authors?.join(', ') || t('common.na')}
            </div>
          </div>
          <div className="flex-[2]">
            <div className="text-xs text-gray-500 uppercase mb-2">{t('import.review.assignedAuthors')}</div>
            
            {/* New Authors from file */}
            {mergedData.authorNames.map((name, index) => (
              <div key={`new-${index}`} className="flex mb-2 gap-2 items-center bg-green-50 p-2 rounded border border-green-200">
                <div className="flex-grow text-sm font-medium text-green-800">
                  <span className="text-xs bg-green-200 px-1 rounded mr-2 uppercase tracking-tighter">New</span>
                  {name}
                </div>
                <button
                  type="button"
                  onClick={() => handleRemoveNewAuthor(index)}
                  className="text-red-500 hover:text-red-700"
                >
                  ✖
                </button>
              </div>
            ))}

            {/* Existing Authors */}
            {mergedData.authorIds.map((authorId, index) => (
              <div key={`existing-${index}`} className="flex mb-2 gap-2 items-start">
                <div className="flex-grow">
                  <SearchableDropdown
                    id={`author-${index}`}
                    options={authorOptions}
                    value={authorId}
                    onChange={(id) => handleAuthorChange(index, id)}
                    placeholder={t('addBook.form.selectAuthor')}
                  />
                </div>
                <button
                  type="button"
                  onClick={() => handleRemoveAuthorField(index)}
                  className="bg-red-100 text-red-700 hover:bg-red-700 hover:text-white font-bold py-2 px-3 rounded h-fit"
                >
                  ✖
                </button>
              </div>
            ))}
            <button
              type="button"
              onClick={handleAddAuthorField}
              className="bg-blue-100 text-blue-700 hover:bg-blue-700 hover:text-white font-bold py-1 px-3 rounded text-sm"
            >
              + {t('addBook.form.addAuthor')}
            </button>
          </div>
        </div>
      </div>

      <div className="mb-4 border-b pb-4">
        <label className="block text-sm font-bold text-gray-700 mb-2">{t('addBook.form.series')}</label>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <SearchableDropdown
            id="series"
            options={seriesOptions}
            value={mergedData.seriesId || ''}
            onChange={handleSeriesChange}
            placeholder={t('addBook.form.selectSeries')}
          />
          <div>
            <input
              type="number"
              value={mergedData.volume || ''}
              onChange={handleVolumeChange}
              disabled={!mergedData.seriesId}
              placeholder={t('addBook.form.volume')}
              className={`shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline ${!mergedData.seriesId ? 'bg-gray-100 cursor-not-allowed' : ''}`}
            />
          </div>
        </div>
      </div>

      <FieldComparison 
        label={t('addBook.form.publisher')} 
        field="publisher" 
        existingValue={existingBook?.publisher} 
        extractedValue={extracted.publisher} 
      />

      <FieldComparison 
        label={t('addBook.form.publicationDate')} 
        field="publicationDate" 
        existingValue={existingBook?.publicationDate} 
        extractedValue={extracted.publicationDate} 
      />

      <div className="mb-4 border-b pb-2">
        <label className="block text-sm font-bold text-gray-700 mb-1">{t('import.review.description')}</label>
        <div className="flex flex-col gap-2">
          {existingBook?.description && (
            <div 
              className={`p-2 rounded border cursor-pointer text-sm ${selectedSources.description === 'existing' ? 'bg-blue-100 border-blue-500' : 'bg-gray-50 border-gray-200'}`}
              onClick={() => toggleField('description', 'existing')}
            >
              <div className="text-xs text-gray-500 uppercase mb-1">{t('import.review.existingValue')}</div>
              <div className="line-clamp-3">{existingBook.description}</div>
            </div>
          )}
          {extracted.description && (
            <div 
              className={`p-2 rounded border cursor-pointer text-sm ${selectedSources.description === 'extracted' ? 'bg-green-100 border-green-500' : 'bg-gray-50 border-gray-200'}`}
              onClick={() => toggleField('description', 'extracted')}
            >
              <div className="text-xs text-gray-500 uppercase mb-1">{t('import.review.extractedValue')}</div>
              <div className="line-clamp-3">{extracted.description}</div>
            </div>
          )}
        </div>
      </div>

      {extracted.coverStorageKey && (
        <div className="mb-4">
          <label className="flex items-center gap-2 cursor-pointer p-3 bg-green-50 rounded border border-green-200">
            <input 
              type="checkbox" 
              checked={selectedSources.cover} 
              onChange={handleCoverToggle}
              className="w-5 h-5 text-green-600"
            />
            <div>
              <span className="font-bold text-gray-700">{t('import.review.useExtractedCover')}</span>
              <div className="mt-2">
                <img 
                  src={`/api/import/staged/${stagedUpload.id}/cover`} 
                  alt="Extracted Cover" 
                  className="h-32 object-contain rounded shadow-sm border bg-white"
                />
              </div>
            </div>
          </label>
        </div>
      )}
    </div>
  );
};

export default MergeMetadataView;
