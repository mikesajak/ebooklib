import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { FaDatabase, FaFileAlt, FaPencilAlt } from 'react-icons/fa';
import SearchableDropdown from './SearchableDropdown';

const MergeMetadataView = ({
  stagedUpload,
  existingBook,
  draftBook,
  authorOptions,
  seriesOptions,
  onMergedDataChange
}) => {
  const { t } = useTranslation();
  const extracted = stagedUpload.metadata || {};
  const validation = stagedUpload.validation || {}; // Use validation from stagedUpload root

  const resolveInitialAuthors = () => {
    // Priority for manual entry if it exists and has authors
    const draftAuthors = draftBook?.authors || [];
    if (draftAuthors.length > 0 && draftAuthors.some(a => a.id || a.lastName)) {
      return {
        authorIds: draftAuthors.filter(a => !!a.id).map(a => a.id),
        authorNames: draftAuthors.filter(a => !a.id && (a.firstName || a.lastName)).map(a => `${a.firstName} ${a.lastName}`.trim())
      };
    }

    if (existingBook) {
      return {
        authorIds: existingBook.authors.map(a => a.id),
        authorNames: []
      };
    }
    
    // Fallback to extracted names
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

  // Helper to determine initial source for a field
  const getInitialSource = (field) => {
    if (draftBook && draftBook[field] && draftBook[field].toString().trim() !== '') return 'draft';
    if (extracted && extracted[field] && extracted[field].toString().trim() !== '') return 'extracted';
    if (existingBook && existingBook[field]) return 'existing';
    return 'extracted';
  };

  const [mergedData, setMergedData] = useState({
    title: draftBook?.title || extracted.title || (existingBook ? existingBook.title : ''),
    authorIds: initialAuthors.authorIds,
    authorNames: initialAuthors.authorNames,
    publisher: draftBook?.publisher || extracted.publisher || (existingBook ? existingBook.publisher : ''),
    publicationDate: draftBook?.publicationDate || extracted.publicationDate || (existingBook ? (existingBook.publicationDate ? existingBook.publicationDate.split('T')[0] : '') : ''),
    description: draftBook?.description || extracted.description || (existingBook ? existingBook.description : ''),
    seriesId: draftBook?.series?.id || (existingBook ? existingBook.series?.id : null),
    volume: draftBook?.volume || (existingBook ? existingBook.volume : null),
    labels: (draftBook?.labels?.length > 0 ? draftBook.labels : (existingBook ? existingBook.labels : [])),
    updateCover: false
  });

  const [selectedSources, setSelectedSources] = useState({
    title: getInitialSource('title'),
    publisher: getInitialSource('publisher'),
    publicationDate: getInitialSource('publicationDate'),
    description: getInitialSource('description'),
    cover: false
  });

  useEffect(() => {
    onMergedDataChange(mergedData);
  }, [mergedData, onMergedDataChange]);

  const toggleField = (field, source) => {
    setSelectedSources(prev => ({ ...prev, [field]: source }));
    
    let value;
    if (source === 'extracted') {
      value = extracted[field];
    } else if (source === 'draft') {
      value = draftBook ? (field === 'series' ? draftBook.series?.id : draftBook[field]) : '';
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
    setSelectedSources(prev => ({ ...prev, cover: newValue }));
    setMergedData(prev => ({ ...prev, updateCover: newValue }));
  };

  const SourceButton = ({ source, label, value, field, icon: Icon, colorClass }) => {
    if (!value && source !== 'existing') return null;
    const isSelected = selectedSources[field] === source;

    return (
      <div 
        className={`flex-1 p-2 rounded border cursor-pointer transition-all flex flex-col gap-1
          ${isSelected ? `ring-2 ring-offset-1 ${colorClass} border-transparent shadow-sm` : 'bg-white border-gray-200 hover:border-gray-300'}`}
        onClick={() => toggleField(field, source)}
      >
        <div className={`flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wider ${isSelected ? 'text-gray-900' : 'text-gray-400'}`}>
          <Icon className={isSelected ? '' : 'opacity-50'} />
          {label}
        </div>
        <div className={`text-sm break-words ${!value ? 'italic text-gray-400' : 'text-gray-700'}`}>
          {value || t('common.na')}
        </div>
      </div>
    );
  };

  const FieldComparison = ({ label, field, existingValue, extractedValue, draftValue }) => {
    return (
      <div className="mb-6 last:mb-2">
        <label className="block text-sm font-bold text-gray-700 mb-2">{label}</label>
        <div className="flex flex-col sm:flex-row gap-3">
          <SourceButton 
            source="existing" 
            label="Library" 
            value={existingValue} 
            field={field} 
            icon={FaDatabase} 
            colorClass="bg-blue-100 ring-blue-500" 
          />
          <SourceButton 
            source="draft" 
            label="Form (Manual)" 
            value={draftValue} 
            field={field} 
            icon={FaPencilAlt} 
            colorClass="bg-yellow-100 ring-yellow-500" 
          />
          <SourceButton 
            source="extracted" 
            label="Ebook File" 
            value={extractedValue} 
            field={field} 
            icon={FaFileAlt} 
            colorClass="bg-green-100 ring-green-500" 
          />
        </div>
      </div>
    );
  };

  return (
    <div className="merge-metadata-view max-h-[65vh] overflow-y-auto pr-3 custom-scrollbar">
      <div className="mb-6 p-4 bg-indigo-50 border-l-4 border-indigo-500 rounded-r-lg shadow-sm text-sm">
        <h4 className="font-bold text-indigo-900 mb-1">{t('import.review.statusTitle')}</h4>
        {existingBook ? (
          <div className="flex items-center gap-2 text-indigo-800">
            <FaDatabase />
            <span>
              Matching with: <span className="font-bold">{existingBook.title}</span>
              {validation.titleMatch && validation.authorMatch 
                ? ` (${t('import.review.statusMatch')})` 
                : ` (${t('import.review.statusMismatch')})`}
            </span>
          </div>
        ) : (
          <div className="flex items-center gap-2 text-indigo-800">
            <FaPencilAlt />
            <span>{t('import.review.statusNewBook')}</span>
          </div>
        )}
      </div>

      <FieldComparison 
        label={t('addBook.form.title')} 
        field="title" 
        existingValue={existingBook?.title} 
        extractedValue={extracted.title} 
        draftValue={draftBook?.title}
      />

      <div className="mb-6 pb-2">
        <label className="block text-sm font-bold text-gray-700 mb-2">{t('addBook.form.author')}</label>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 bg-gray-50 p-3 rounded-lg border border-gray-200">
          <div className="flex flex-col gap-1">
            <div className="text-[10px] font-bold text-gray-400 uppercase flex items-center gap-1"><FaFileAlt /> Extracted</div>
            <div className="text-sm text-gray-600">{extracted.authors?.join(', ') || t('common.na')}</div>
          </div>
          <div className="lg:col-span-2">
            <div className="text-[10px] font-bold text-gray-400 uppercase mb-2 flex items-center gap-1"><FaPencilAlt /> Final Assigned List</div>
            
            {/* New Authors */}
            {mergedData.authorNames.map((name, index) => (
              <div key={`new-${index}`} className="flex mb-2 gap-2 items-center bg-green-50 p-2 rounded border border-green-200 shadow-sm animate-fade-in">
                <div className="flex-grow text-sm font-medium text-green-800">
                  <span className="text-[9px] bg-green-200 px-1 rounded mr-2 uppercase font-bold">New</span>
                  {name}
                </div>
                <button type="button" onClick={() => handleRemoveNewAuthor(index)} className="text-red-400 hover:text-red-600 transition-colors">✖</button>
              </div>
            ))}

            {/* Existing Authors */}
            {mergedData.authorIds.map((authorId, index) => (
              <div key={`existing-${index}`} className="flex mb-2 gap-2 items-start animate-fade-in">
                <div className="flex-grow">
                  <SearchableDropdown
                    id={`author-${index}`}
                    options={authorOptions}
                    value={authorId}
                    onChange={(id) => handleAuthorChange(index, id)}
                    placeholder={t('addBook.form.selectAuthor')}
                  />
                </div>
                <button type="button" onClick={() => handleRemoveAuthorField(index)} className="bg-red-50 text-red-400 hover:bg-red-500 hover:text-white font-bold py-2 px-3 rounded h-fit transition-colors">✖</button>
              </div>
            ))}
            <button
              type="button"
              onClick={handleAddAuthorField}
              className="mt-1 bg-white text-indigo-600 hover:bg-indigo-600 hover:text-white border border-indigo-200 font-bold py-1.5 px-4 rounded-full text-xs transition-all shadow-sm flex items-center gap-1 w-fit"
            >
              + {t('addBook.form.addAuthor')}
            </button>
          </div>
        </div>
      </div>

      <div className="mb-6">
        <label className="block text-sm font-bold text-gray-700 mb-2">{t('addBook.form.series')}</label>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-gray-50 p-3 rounded-lg border border-gray-200">
          <SearchableDropdown
            id="series"
            options={seriesOptions}
            value={mergedData.seriesId || ''}
            onChange={handleSeriesChange}
            placeholder={t('addBook.form.selectSeries')}
          />
          <div className="relative">
            <input
              type="number"
              value={mergedData.volume || ''}
              onChange={handleVolumeChange}
              disabled={!mergedData.seriesId}
              placeholder={t('addBook.form.volume')}
              className={`shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline ${!mergedData.seriesId ? 'bg-gray-100 cursor-not-allowed' : 'bg-white'}`}
            />
            {!mergedData.seriesId && <div className="absolute inset-0 bg-gray-100 opacity-20 cursor-not-allowed"></div>}
          </div>
        </div>
      </div>

      <FieldComparison 
        label={t('addBook.form.publisher')} 
        field="publisher" 
        existingValue={existingBook?.publisher} 
        extractedValue={extracted.publisher} 
        draftValue={draftBook?.publisher}
      />

      <FieldComparison 
        label={t('addBook.form.publicationDate')} 
        field="publicationDate" 
        existingValue={existingBook?.publicationDate} 
        extractedValue={extracted.publicationDate} 
        draftValue={draftBook?.publicationDate}
      />

      <div className="mb-6">
        <label className="block text-sm font-bold text-gray-700 mb-2">{t('import.review.description')}</label>
        <div className="flex flex-col gap-3">
          <SourceButton 
            source="existing" 
            label="Library" 
            value={existingBook?.description} 
            field="description" 
            icon={FaDatabase} 
            colorClass="bg-blue-100 ring-blue-500" 
          />
          <SourceButton 
            source="draft" 
            label="Form (Manual)" 
            value={draftBook?.description} 
            field="description" 
            icon={FaPencilAlt} 
            colorClass="bg-yellow-100 ring-yellow-500" 
          />
          <SourceButton 
            source="extracted" 
            label="Ebook File" 
            value={extracted.description} 
            field="description" 
            icon={FaFileAlt} 
            colorClass="bg-green-100 ring-green-500" 
          />
        </div>
      </div>

      {extracted.coverStorageKey && (
        <div className="mb-4">
          <label className="flex items-center gap-2 cursor-pointer p-4 bg-green-50 rounded-xl border-2 border-green-200 hover:bg-green-100 transition-colors shadow-sm group">
            <input 
              type="checkbox" 
              checked={selectedSources.cover} 
              onChange={handleCoverToggle}
              className="w-6 h-6 text-green-600 rounded-lg focus:ring-green-500 transition-all"
            />
            <div className="flex-grow flex items-center justify-between">
              <div>
                <span className="font-extrabold text-green-900">{t('import.review.useExtractedCover')}</span>
                <p className="text-xs text-green-700 mt-0.5">Replace current cover with image from ebook file</p>
              </div>
              <div className="p-1 bg-white rounded-lg border border-green-300 shadow-inner group-hover:scale-105 transition-transform">
                <img 
                  src={`/api/import/staged/${stagedUpload.id}/cover`} 
                  alt="Extracted Cover" 
                  className="h-24 w-16 object-cover rounded shadow-sm"
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
