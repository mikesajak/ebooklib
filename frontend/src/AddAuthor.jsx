import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FaUserTag, FaInfoCircle, FaPencilAlt } from 'react-icons/fa';
import useMutation from './hooks/useMutation';
import AddPage from './AddPage';
import Form from './Form';
import { fetchWithCsrf } from './api';

const saveAuthor = async (authorData, isEditMode, authorId) => {
  const method = isEditMode ? 'PUT' : 'POST';
  const url = isEditMode ? `/api/authors/${authorId}` : '/api/authors';

  const response = await fetchWithCsrf(url, {
    method: method,
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(authorData),
  });

  if (!response.ok) {
    let errorMessage = 'Failed to save author';
    try {
      const errorData = await response.json();
      errorMessage = errorData.message || errorMessage;
    } catch (e) {
      // ignore if response is not json
    }
    throw new Error(errorMessage);
  }

  return response.json();
};

const AddAuthor = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { id } = useParams(); // Get author ID from URL
  const isEditMode = !!id; // Determine if in edit mode
  const [author, setAuthor] = useState({
    firstName: '',
    lastName: '',
    bio: '',
    birthDate: '',
    deathDate: ''
  });
  const [originalAuthor, setOriginalAuthor] = useState(null); // Store original author data for comparison
  const [loading, setLoading] = useState(true);
  const [dirtyFields, setDirtyFields] = useState(new Set());

  const markDirty = (fieldName) => {
    setDirtyFields(prev => {
      const next = new Set(prev);
      next.add(fieldName);
      return next;
    });
  };

  const { mutate, isSaving, notification, setNotification } = useMutation(
    (authorData) => saveAuthor(authorData, isEditMode, id),
    {
      onSuccess: (data) => {
        const successMessage = t(isEditMode ? 'addAuthor.updateSuccess' : 'addAuthor.addSuccess');
        navigate(`/author/${data.id}`, { state: { notification: { type: 'success', message: successMessage } } });
      }
    }
  );

  useEffect(() => {
    const fetchAuthor = async () => {
      try {
        setLoading(true);
        const response = await fetchWithCsrf(`/api/authors/${id}`);
        if (!response.ok) {
          throw new Error('Failed to fetch author details');
        }
        const data = await response.json();
        setAuthor(data);
        setOriginalAuthor(data); // Store original data
      } catch (err) {
        setNotification({ type: 'error', message: err.message });
      } finally {
        setLoading(false);
      }
    };

    if (isEditMode) {
      fetchAuthor();
    } else {
      setLoading(false); // No need to load if not in edit mode
    }
  }, [id, isEditMode, setNotification]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    markDirty(name);
    setAuthor(prevAuthor => ({
      ...prevAuthor,
      [name]: value
    }));
  };

  const handleSave = () => {
    mutate(author);
  };

  const handleCancel = () => {
    navigate(isEditMode ? `/author/${id}` : '/authors');
  };

  const isFormValid = author.firstName.trim() !== '' && author.lastName.trim() !== '';

  const hasChanges = () => {
    if (!originalAuthor || !author) return false;

    const normalize = (val) => val || '';

    if (normalize(originalAuthor.firstName) !== normalize(author.firstName)) return true;
    if (normalize(originalAuthor.lastName) !== normalize(author.lastName)) return true;
    if (normalize(originalAuthor.bio) !== normalize(author.bio)) return true;
    if (normalize(originalAuthor.birthDate) !== normalize(author.birthDate)) return true;
    if (normalize(originalAuthor.deathDate) !== normalize(author.deathDate)) return true;

    return false;
  };

  const isSaveDisabled = !isFormValid || isSaving || (isEditMode && !hasChanges());

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

  if (loading) {
    return (
      <AddPage title={t(isEditMode ? 'addAuthor.editTitle' : 'addAuthor.title')} notification={notification} setNotification={setNotification}>
        <p>{t('common.loading')}</p>
      </AddPage>
    );
  }

  return (
    <AddPage title={t(isEditMode ? 'addAuthor.editTitle' : 'addAuthor.title')} notification={notification} setNotification={setNotification}>
      <Form onSave={handleSave} onCancel={handleCancel} isSaveDisabled={isSaveDisabled}>
        
        <SectionHeader icon={FaUserTag} title={t('addAuthor.section.identity', 'Personal Identity')} description="Basic identification for the author." />
        <InputCard isDirty={dirtyFields.has('firstName') || dirtyFields.has('lastName')}>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div>
              <div className="flex items-center gap-2 mb-3">
                <label className="block text-gray-700 text-xs font-black uppercase tracking-widest" htmlFor="firstName">
                  {t('addAuthor.form.firstName')}
                </label>
                {dirtyFields.has('firstName') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
              </div>
              <input type="text" id="firstName" name="firstName" value={author.firstName || ''} onChange={handleChange} className={`shadow-sm appearance-none border-2 rounded-xl w-full py-3 px-4 text-gray-700 leading-tight font-bold focus:outline-none focus:ring-4 focus:ring-indigo-500/10 focus:border-indigo-500 transition-all ${dirtyFields.has('firstName') ? 'border-yellow-300' : 'border-gray-100 bg-gray-50/30'}`} />
            </div>
            <div>
              <div className="flex items-center gap-2 mb-3">
                <label className="block text-gray-700 text-xs font-black uppercase tracking-widest" htmlFor="lastName">
                  {t('addAuthor.form.lastName')}
                </label>
                {dirtyFields.has('lastName') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
              </div>
              <input type="text" id="lastName" name="lastName" value={author.lastName || ''} onChange={handleChange} className={`shadow-sm appearance-none border-2 rounded-xl w-full py-3 px-4 text-gray-700 leading-tight font-bold focus:outline-none focus:ring-4 focus:ring-indigo-500/10 focus:border-indigo-500 transition-all ${dirtyFields.has('lastName') ? 'border-yellow-300' : 'border-gray-100 bg-gray-50/30'}`} />
            </div>
          </div>
        </InputCard>

        <SectionHeader icon={FaInfoCircle} title={t('addAuthor.section.bio', 'Biography & Key Dates')} description="Contextual information and lifespan dates." />
        <InputCard isDirty={dirtyFields.has('bio') || dirtyFields.has('birthDate') || dirtyFields.has('deathDate')}>
          <div className="mb-8">
            <div className="flex items-center gap-2 mb-3">
              <label className="block text-gray-700 text-xs font-black uppercase tracking-widest" htmlFor="bio">{t('addAuthor.form.bio')}</label>
              {dirtyFields.has('bio') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
            </div>
            <textarea id="bio" name="bio" value={author.bio || ''} onChange={handleChange} rows="4" className={`shadow-sm appearance-none border-2 rounded-2xl w-full py-3 px-4 text-gray-700 leading-tight font-medium focus:outline-none focus:ring-4 focus:ring-indigo-500/10 focus:border-indigo-500 transition-all ${dirtyFields.has('bio') ? 'border-yellow-300' : 'border-gray-100 bg-gray-50/30'}`} />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div>
              <div className="flex items-center gap-2 mb-3">
                <label className="block text-gray-700 text-xs font-black uppercase tracking-widest" htmlFor="birthDate">{t('addAuthor.form.birthDate')}</label>
                {dirtyFields.has('birthDate') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
              </div>
              <input type="date" id="birthDate" name="birthDate" value={author.birthDate || ''} onChange={handleChange} className={`shadow-sm appearance-none border-2 rounded-xl w-full py-3 px-4 text-gray-700 leading-tight font-bold focus:outline-none focus:ring-4 focus:ring-indigo-500/10 focus:border-indigo-500 transition-all ${dirtyFields.has('birthDate') ? 'border-yellow-300' : 'border-gray-100 bg-gray-50/30'}`} />
            </div>
            <div>
              <div className="flex items-center gap-2 mb-3">
                <label className="block text-gray-700 text-xs font-black uppercase tracking-widest" htmlFor="deathDate">{t('addAuthor.form.deathDate')}</label>
                {dirtyFields.has('deathDate') && <FaPencilAlt className="text-yellow-600 text-[10px]" />}
              </div>
              <input type="date" id="deathDate" name="deathDate" value={author.deathDate || ''} onChange={handleChange} className={`shadow-sm appearance-none border-2 rounded-xl w-full py-3 px-4 text-gray-700 leading-tight font-bold focus:outline-none focus:ring-4 focus:ring-indigo-500/10 focus:border-indigo-500 transition-all ${dirtyFields.has('deathDate') ? 'border-yellow-300' : 'border-gray-100 bg-gray-50/30'}`} />
            </div>
          </div>
        </InputCard>
      </Form>
    </AddPage>
  );
};

export default AddAuthor;
