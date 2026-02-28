import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';

const MergeMetadataView = ({
  stagedUpload,
  existingBook,
  onMergedDataChange
}) => {
  const { t } = useTranslation();
  const extracted = stagedUpload.metadata || {};
  const validation = extracted.validation || {};

  const [mergedData, setMergedData] = useState({
    title: extracted.title || (existingBook ? existingBook.title : ''),
    authorIds: existingBook ? existingBook.authors.map(a => a.id) : [],
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

  const handleCoverToggle = () => {
    const newValue = !selectedSources.cover;
    setSelectedIds(prev => ({ ...prev, cover: newValue }));
    setMergedData(prev => ({ ...prev, updateCover: newValue }));
  };

  const FieldComparison = ({ label, field, existingValue, extractedValue }) => {
    const isDifferent = existingValue !== extractedValue && extractedValue;
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

      {/* Authors are special because they need ID mapping, for now just show names as reference */}
      <div className="mb-4 border-b pb-2">
        <label className="block text-sm font-bold text-gray-700 mb-1">{t('addBook.form.author')}</label>
        <div className="flex gap-4">
          <div className="flex-1 p-2 bg-blue-50 rounded border border-blue-200">
            <div className="text-xs text-gray-500 uppercase">{t('import.review.existingValue')}</div>
            <div>{existingBook?.authors.map(a => `${a.firstName} ${a.lastName}`).join(', ') || t('common.na')}</div>
          </div>
          {extracted.authors && (
            <div className="flex-1 p-2 bg-gray-50 rounded border border-gray-200">
              <div className="text-xs text-gray-500 uppercase">{t('import.review.extractedValue')}</div>
              <div>{extracted.authors.join(', ')}</div>
            </div>
          )}
        </div>
        <p className="text-xs text-gray-500 mt-1 italic">{t('import.review.authorNote')}</p>
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
