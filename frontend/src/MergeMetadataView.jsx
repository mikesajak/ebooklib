import React, { useState, useEffect, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { FaDatabase, FaFileAlt, FaPencilAlt, FaGlobe } from 'react-icons/fa';
import SearchableDropdown from './SearchableDropdown';
import CoverImagePreview from './CoverImagePreview';

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
  
  // stagedUpload can be a single upload or an object that looks like one 
  // (e.g., from ResolutionItem where we might have multiple formats)
  const extracted = stagedUpload?.metadata || {};
  const validation = stagedUpload?.validation || {};
  const enrichmentList = extracted.enrichment || [];
  const external = enrichmentList.length > 0 ? enrichmentList[0] : null;

  const resolveInitialAuthors = () => {
    const draftAuthors = draftBook?.authors || [];
    // 1. User manual input (Form/Draft)
    if (dirtyFields?.has('authors') && draftAuthors.some(a => a.id || a.lastName)) {
      return {
        authorIds: draftAuthors.filter(a => !!a.id).map(a => a.id),
        authorNames: draftAuthors.filter(a => !a.id && (a.firstName || a.lastName)).map(a => `${a.firstName} ${a.lastName}`.trim())
      };
    }

    // 2. Existing library book
    if (existingBook) {
      return {
        authorIds: existingBook.authors.map(a => a.id),
        authorNames: []
      };
    }
    
    // 3. External enrichment
    if (external && external.authors && external.authors.length > 0) {
      const ids = [];
      const names = [];
      external.authors.forEach(name => {
        const normalizedName = name.toLowerCase().trim();
        const match = authorOptions.find(opt => opt.name.toLowerCase().trim() === normalizedName);
        if (match) ids.push(match.id);
        else names.push(name);
      });
      return { authorIds: ids, authorNames: names };
    }

    // 4. File extraction
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
    if (external && external[field] && external[field].toString().trim() !== '') return 'external';
    if (extracted && extracted[field] && extracted[field].toString().trim() !== '') return 'extracted';
    if (existingBook && existingBook[field]) return 'existing';
    return 'extracted';
  };

  const [mergedData, setMergedData] = useState({
    title: (dirtyFields?.has('title') ? draftBook?.title : (external?.title || extracted.title || existingBook?.title || '')),
    authorIds: initialAuthors.authorIds,
    authorNames: initialAuthors.authorNames,
    publisher: (dirtyFields?.has('publisher') ? draftBook?.publisher : (external?.publisher || extracted.publisher || existingBook?.publisher || '')),
    publicationDate: (dirtyFields?.has('publicationDate') ? draftBook?.publicationDate : (external?.publicationDate || extracted.publicationDate || (existingBook?.publicationDate ? existingBook.publicationDate.split('T')[0] : ''))),
    description: (dirtyFields?.has('description') ? draftBook?.description : (external?.description || extracted.description || existingBook?.description || '')),
    seriesId: (dirtyFields?.has('series') ? draftBook?.series?.id : (existingBook?.series?.id || null)),
    volume: (dirtyFields?.has('volume') ? draftBook?.volume : (existingBook?.volume || null)),
    labels: (dirtyFields?.has('labels') ? draftBook?.labels : (existingBook?.labels || [])),
    updateCover: !!(external?.coverUrl || extracted.coverStorageKey) && !existingBook
  });

  const [selectedSources, setSelectedSources] = useState({
    title: getInitialSource('title'),
    publisher: getInitialSource('publisher'),
    publicationDate: getInitialSource('publicationDate'),
    description: getInitialSource('description'),
    cover: !!(external?.coverUrl || extracted.coverStorageKey) && !existingBook
  });

  useEffect(() => {
    onMergedDataChange(mergedData);
  }, [mergedData, onMergedDataChange]);

  const toggleField = (field, source) => {
    setSelectedSources(prev => ({ ...prev, [field]: source }));
    
    let value;
    if (source === 'external') {
      value = external[field];
    } else if (source === 'extracted') {
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

  const handleDirectFieldChange = (field, value) => {
    setMergedData(prev => ({ ...prev, [field]: value }));
    
    // Check if the manual value happens to match an existing source
    let matchedSource = 'manual';
    if (external && external[field] && String(external[field]) === String(value)) {
      matchedSource = 'external';
    } else if (extracted && extracted[field] && String(extracted[field]) === String(value)) {
      matchedSource = 'extracted';
    } else if (draftBook && draftBook[field] && String(draftBook[field]) === String(value)) {
      matchedSource = 'draft';
    } else if (existingBook && existingBook[field] && String(existingBook[field]) === String(value)) {
      matchedSource = 'existing';
    }

    setSelectedSources(prev => ({ ...prev, [field]: matchedSource }));
  };

  return (
    <div className="merge-metadata-view max-h-[65vh] overflow-y-auto px-4 pr-3 custom-scrollbar">
      <div className="mb-6 p-4 bg-indigo-50 border-l-4 border-indigo-500 rounded-r-lg shadow-sm text-sm">
        <h4 className="font-bold text-indigo-900 mb-1">{t('import.review.statusTitle')}</h4>
        {existingBook ? (
          <div className="flex items-center gap-2 text-indigo-800">
            <FaDatabase />
            <span>
              {t('import.review.matchingWith', 'Matching with:')} <span className="font-bold">{existingBook.title}</span>
              {validation.titleMatch && validation.authorMatch 
                ? ` (${t('import.review.statusMatch', 'Matches an existing book in your library')})` 
                : ` (${t('import.review.statusMismatch', 'Potential metadata mismatch with existing book')})`}
            </span>
          </div>
        ) : (
          <div className="flex items-center gap-2 text-indigo-800">
            <FaPencilAlt />
            <span>{t('import.review.statusNewBook', 'This is a new book entry')}</span>
          </div>
        )}
      </div>

      <FieldComparison 
        label={t('addBook.form.title')} 
        field="title" 
        existingValue={existingBook?.title} 
        extractedValue={extracted.title} 
        draftValue={draftBook?.title}
        externalValue={external?.title}
        selectedSources={selectedSources}
        mergedData={mergedData}
        dirtyFields={dirtyFields}
        onToggleField={toggleField}
        onDirectFieldChange={handleDirectFieldChange}
        t={t}
      />

      <div className="mb-6 pb-2">
        <label className="block text-sm font-bold text-gray-700 mb-2">{t('addBook.form.author')}</label>
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-4 bg-gray-50 p-3 rounded-lg border border-gray-200">
          <div className="flex flex-col gap-1">
            <div className="text-[10px] font-bold text-gray-400 uppercase flex items-center gap-1"><FaFileAlt /> {t('import.review.sources.extracted', 'Extracted')}</div>
            <div className="text-sm text-gray-600">{extracted.authors?.join(', ') || t('common.na')}</div>
          </div>
          <div className="flex flex-col gap-1">
            <div className="text-[10px] font-bold text-purple-400 uppercase flex items-center gap-1"><FaGlobe /> {t('import.review.sources.external', 'External')}</div>
            <div className="text-sm text-gray-600">{external?.authors?.join(', ') || t('common.na')}</div>
          </div>
          <div className="lg:col-span-2">
            <div className="text-[10px] font-bold text-gray-400 uppercase mb-2 flex items-center gap-1"><FaPencilAlt /> {t('import.review.sources.finalList', 'Final Assigned List')}</div>
            
            {/* New Authors */}
            {mergedData.authorNames.map((name, index) => (
              <div key={`new-${index}`} className="flex mb-2 gap-2 items-center bg-green-50 p-2 rounded border border-green-200 shadow-sm animate-fade-in">
                <div className="flex-grow text-sm font-medium text-green-800">
                  <span className="text-[9px] bg-green-200 px-1 rounded mr-2 uppercase font-bold">{t('import.review.sources.new', 'New')}</span>
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
        externalValue={external?.publisher}
        selectedSources={selectedSources}
        mergedData={mergedData}
        dirtyFields={dirtyFields}
        onToggleField={toggleField}
        onDirectFieldChange={handleDirectFieldChange}
        t={t}
      />

      <FieldComparison 
        label={t('addBook.form.publicationDate')} 
        field="publicationDate" 
        existingValue={existingBook?.publicationDate} 
        extractedValue={extracted.publicationDate} 
        draftValue={draftBook?.publicationDate} 
        externalValue={external?.publicationDate}
        inputType="date"
        selectedSources={selectedSources}
        mergedData={mergedData}
        dirtyFields={dirtyFields}
        onToggleField={toggleField}
        onDirectFieldChange={handleDirectFieldChange}
        t={t}
      />

      <FieldComparison 
        label={t('addBook.form.description')} 
        field="description" 
        existingValue={existingBook?.description} 
        extractedValue={extracted.description} 
        draftValue={draftBook?.description} 
        externalValue={external?.description}
        inputType="textarea"
        selectedSources={selectedSources}
        mergedData={mergedData}
        dirtyFields={dirtyFields}
        onToggleField={toggleField}
        onDirectFieldChange={handleDirectFieldChange}
        t={t}
      />

      {(extracted.coverStorageKey || external?.coverUrl) && (
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
                <span className="font-extrabold text-green-900">
                  {existingBook ? t('import.review.useExtractedCover', 'Update Cover') : t('import.review.importCover', 'Import Cover')}
                </span>
                <p className="text-xs text-green-700 mt-0.5">
                  {external?.coverUrl ? t('import.review.externalCoverHint', "Using external high-res cover if available") : (existingBook ? t('import.review.useExtractedCoverSubtext') : t('import.review.importCoverSubtext'))}
                </p>
              </div>
              <div className="p-1 bg-white rounded-lg border border-green-300 shadow-inner group-hover:scale-105 transition-transform" onClick={(e) => e.stopPropagation()}>
                <CoverImagePreview 
                  src={external?.coverUrl || (stagedUpload?.id ? `/api/import/staged/${stagedUpload.id}/cover` : '')} 
                  alt="Extracted Cover" 
                  title={draftBook?.title || 'Extracted Cover'}
                  containerClassName="h-24 w-16"
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

const MultiSourceButton = ({ sources, value, field, isSelected, onToggle, t }) => {
  const sourceConfigs = {
    existing: { label: t('import.review.sources.library', 'Library'), icon: FaDatabase, colorClass: 'ring-blue-500 bg-blue-100' },
    draft: { label: t('import.review.sources.form', 'Form'), icon: FaPencilAlt, colorClass: 'ring-yellow-500 bg-yellow-100' },
    extracted: { label: t('import.review.sources.file', 'File'), icon: FaFileAlt, colorClass: 'ring-green-500 bg-green-100' },
    external: { label: t('import.review.sources.web', 'Web'), icon: FaGlobe, colorClass: 'ring-purple-500 bg-purple-100' }
  };

  const firstSource = sources[0];
  const ringClass = isSelected ? sourceConfigs[firstSource].colorClass : 'bg-white border-gray-200 hover:border-gray-300';

  return (
    <div 
      className={`flex-1 p-2 rounded border cursor-pointer transition-all flex flex-col gap-1
        ${isSelected ? `ring-2 ring-inset ${ringClass} border-transparent shadow-sm` : 'bg-white border-gray-200 hover:border-gray-300'}`}
      onClick={() => onToggle(field, firstSource)}
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

const FieldComparison = ({
  label,
  field,
  existingValue,
  extractedValue,
  draftValue,
  externalValue,
  inputType = 'text',
  selectedSources,
  mergedData,
  dirtyFields,
  onToggleField,
  onDirectFieldChange,
  t
}) => {
  // Determine which sources to show - ORDER MATTERS HERE for stable layout
  const sourceValues = [];
  
  // 1. Library (Existing)
  if (existingValue || (!extractedValue && !externalValue)) {
    sourceValues.push({ id: 'existing', value: existingValue || '' });
  }
  
  // 2. Form (Draft)
  const isDraftRelevant = dirtyFields?.has(field) || (draftValue && draftValue !== extractedValue && draftValue !== externalValue);
  if (draftValue && isDraftRelevant) {
    sourceValues.push({ id: 'draft', value: draftValue });
  }
  
  // 3. File (Extracted)
  if (extractedValue) {
    sourceValues.push({ id: 'extracted', value: extractedValue });
  }

  // 4. Web (External)
  if (externalValue && externalValue !== extractedValue) {
    sourceValues.push({ id: 'external', value: externalValue });
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
  // Priority: draft > existing > external > extracted
  const sourcePriority = { draft: 0, existing: 1, external: 2, extracted: 3 };
  groups.sort((a, b) => {
    const aMin = Math.min(...a.sources.map(s => sourcePriority[s]));
    const bMin = Math.min(...b.sources.map(s => sourcePriority[s]));
    return aMin - bMin;
  });

  const isManual = selectedSources[field] === 'manual';

  return (
    <div className="mb-6 last:mb-2">
      <div className="flex items-center justify-between mb-2">
        <label className="block text-sm font-bold text-gray-700">{label}</label>
        {isManual && (
          <span className="text-[10px] font-bold uppercase tracking-wider text-amber-600 bg-amber-50 border border-amber-200 px-2 py-0.5 rounded-full flex items-center gap-1">
            <FaPencilAlt size={10} /> {t('import.review.sources.custom', 'Manually Edited')}
          </span>
        )}
      </div>
      <div className="flex flex-col sm:flex-row gap-3 mb-2">
        {groups.map((group, idx) => (
          <MultiSourceButton 
            key={idx}
            sources={group.sources}
            value={group.value}
            field={field}
            isSelected={group.sources.includes(selectedSources[field])}
            onToggle={onToggleField}
            t={t}
          />
        ))}
      </div>
      {inputType === 'textarea' ? (
        <textarea
          rows={3}
          value={mergedData[field] || ''}
          onChange={(e) => onDirectFieldChange(field, e.target.value)}
          placeholder={t('import.review.editFieldPlaceholder', 'Edit {{label}}...', { label })}
          className="w-full text-sm p-2.5 bg-white border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 shadow-sm"
        />
      ) : inputType === 'date' ? (
        <input
          type="date"
          value={mergedData[field] || ''}
          onChange={(e) => onDirectFieldChange(field, e.target.value)}
          className="w-full text-sm p-2.5 bg-white border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 shadow-sm"
        />
      ) : (
        <input
          type="text"
          value={mergedData[field] || ''}
          onChange={(e) => onDirectFieldChange(field, e.target.value)}
          placeholder={t('import.review.editFieldPlaceholder', 'Edit {{label}}...', { label })}
          className="w-full text-sm p-2.5 bg-white border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 shadow-sm"
        />
      )}
    </div>
  );
};

export default MergeMetadataView;
