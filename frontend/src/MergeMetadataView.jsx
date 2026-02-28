import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { FaDatabase, FaFileAlt, FaPencilAlt } from 'react-icons/fa';
import SearchableDropdown from './SearchableDropdown';

const MergeMetadataView = ({
  stagedUpload,
  existingBook,
  draftBook,
  dirtyFields,
  authorOptions,
  seriesOptions,
  onMergedDataChange
}) => {
  const { t } = useTranslation();
  const extracted = stagedUpload.metadata || {};
  const validation = stagedUpload.validation || {};

  const resolveInitialAuthors = () => {
    const draftAuthors = draftBook?.authors || [];
    // Only use draft authors if the user explicitly touched the authors field
    if (dirtyFields?.has('authors') && draftAuthors.some(a => a.id || a.lastName)) {
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

  const getInitialSource = (field) => {
    if (dirtyFields?.has(field)) return 'draft';
    if (extracted && extracted[field] && extracted[field].toString().trim() !== '') return 'extracted';
    if (existingBook && existingBook[field]) return 'existing';
    return 'extracted';
  };

  const [mergedData, setMergedData] = useState({
    title: (dirtyFields?.has('title') ? draftBook?.title : (extracted.title || existingBook?.title || '')),
    authorIds: initialAuthors.authorIds,
    authorNames: initialAuthors.authorNames,
    publisher: (dirtyFields?.has('publisher') ? draftBook?.publisher : (extracted.publisher || existingBook?.publisher || '')),
    publicationDate: (dirtyFields?.has('publicationDate') ? draftBook?.publicationDate : (extracted.publicationDate || (existingBook?.publicationDate ? existingBook.publicationDate.split('T')[0] : ''))),
    description: (dirtyFields?.has('description') ? draftBook?.description : (extracted.description || existingBook?.description || '')),
    seriesId: (dirtyFields?.has('series') ? draftBook?.series?.id : (existingBook?.series?.id || null)),
    volume: (dirtyFields?.has('volume') ? draftBook?.volume : (existingBook?.volume || null)),
    labels: (dirtyFields?.has('labels') ? draftBook?.labels : (existingBook?.labels || [])),
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

  const MultiSourceButton = ({ sources, value, field, isSelected }) => {
    const sourceConfigs = {
      existing: { label: 'Library', icon: FaDatabase, colorClass: 'ring-blue-500 bg-blue-100' },
      draft: { label: 'Form', icon: FaPencilAlt, colorClass: 'ring-yellow-500 bg-yellow-100' },
      extracted: { label: 'File', icon: FaFileAlt, colorClass: 'ring-green-500 bg-green-100' }
    };

    const firstSource = sources[0];
    const ringClass = isSelected ? sourceConfigs[firstSource].colorClass : 'bg-white border-gray-200 hover:border-gray-300';

    return (
      <div 
        className={`flex-1 p-2 rounded border cursor-pointer transition-all flex flex-col gap-1
          ${isSelected ? `ring-2 ring-inset ${ringClass} border-transparent shadow-sm` : 'bg-white border-gray-200 hover:border-gray-300'}`}
        onClick={() => toggleField(field, firstSource)}
      >
        <div className="flex flex-wrap items-center gap-2">
          {sources.map(src => {
            const Config = sourceConfigs[src];
            return (
              <div key={src} className={`flex items-center gap-1 text-[9px] font-bold uppercase tracking-wider ${isSelected ? 'text-gray-900' : 'text-gray-400'}`}>
                <Config.icon className={isSelected ? '' : 'opacity-50'} />
                {Config.label}
              </div>
            );
          })}
        </div>
        <div className={`text-sm break-words ${!value ? 'italic text-gray-400' : 'text-gray-700'}`}>
          {value || t('common.na')}
        </div>
      </div>
    );
  };

  const FieldComparison = ({ label, field, existingValue, extractedValue, draftValue }) => {
    // Determine which sources to show - ORDER MATTERS HERE for stable layout
    const sourceValues = [];
    
    // 1. Library (Existing)
    if (existingValue || !extractedValue) {
      sourceValues.push({ id: 'existing', value: existingValue || '' });
    }
    
    // 2. Form (Draft)
    const isDraftRelevant = dirtyFields?.has(field) || (draftValue && draftValue !== extractedValue);
    if (draftValue && isDraftRelevant) {
      sourceValues.push({ id: 'draft', value: draftValue });
    }
    
    // 3. File (Extracted)
    if (extractedValue) {
      sourceValues.push({ id: 'extracted', value: extractedValue });
    }

    // Group identical values while preserving the order of the first occurrence
    const groups = [];
    sourceValues.forEach(sv => {
      const existingGroup = groups.find(g => g.value === sv.value);
      if (existingGroup) {
        existingGroup.sources.push(sv.id);
      } else {
        groups.push({ value: sv.value, sources: [sv.id] });
      }
    });

    // Ensure the groups themselves are sorted based on the "highest priority" source in them
    // Priority: draft > existing > extracted (Manual data is always the primary reference on the left)
    const sourcePriority = { draft: 0, existing: 1, extracted: 2 };
    groups.sort((a, b) => {
      const aMin = Math.min(...a.sources.map(s => sourcePriority[s]));
      const bMin = Math.min(...b.sources.map(s => sourcePriority[s]));
      return aMin - bMin;
    });

    return (
      <div className="mb-6 last:mb-2">
        <label className="block text-sm font-bold text-gray-700 mb-2">{label}</label>
        <div className="flex flex-col sm:flex-row gap-3">
          {groups.map((group, idx) => (
            <MultiSourceButton 
              key={idx}
              sources={group.sources}
              value={group.value}
              field={field}
              isSelected={group.sources.includes(selectedSources[field])}
            />
          ))}
        </div>
      </div>
    );
  };

  return (
    <div className="merge-metadata-view max-h-[65vh] overflow-y-auto px-4 pr-3 custom-scrollbar">
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

      <FieldComparison 
        label={t('addBook.form.description')} 
        field="description" 
        existingValue={existingBook?.description} 
        extractedValue={extracted.description} 
        draftValue={draftBook?.description} 
      />

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
