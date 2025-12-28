import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import useMutation from './hooks/useMutation';
import AddPage from './AddPage';
import Form from './Form';

const saveAuthor = async (authorData, isEditMode, authorId) => {
  const method = isEditMode ? 'PUT' : 'POST';
  const url = isEditMode ? `/api/authors/${authorId}` : '/api/authors';

  const response = await fetch(url, {
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
        const response = await fetch(`/api/authors/${id}`);
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
  }, [id, isEditMode]);

  const handleChange = (e) => {
    const { name, value } = e.target;
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
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="firstName">{t('addAuthor.form.firstName')}:</label>
          <input type="text" id="firstName" name="firstName" value={author.firstName || ''} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="lastName">{t('addAuthor.form.lastName')}:</label>
          <input type="text" id="lastName" name="lastName" value={author.lastName || ''} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="bio">{t('addAuthor.form.bio')}:</label>
          <textarea id="bio" name="bio" value={author.bio || ''} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="birthDate">{t('addAuthor.form.birthDate')}:</label>
          <input type="date" id="birthDate" name="birthDate" value={author.birthDate || ''} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="deathDate">{t('addAuthor.form.deathDate')}:</label>
          <input type="date" id="deathDate" name="deathDate" value={author.deathDate || ''} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
      </Form>
    </AddPage>
  );
};

export default AddAuthor;