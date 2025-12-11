import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import useMutation from './hooks/useMutation';
import AddPage from './AddPage';
import Form from './Form';

const createAuthor = async (authorData) => {
  const response = await fetch('/api/authors', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(authorData),
  });

  if (!response.ok) {
    let errorMessage = 'Failed to create author';
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
  const [author, setAuthor] = useState({
    firstName: '',
    lastName: '',
    bio: '',
    birthDate: '',
    deathDate: ''
  });

  const { mutate, isSaving, notification, setNotification } = useMutation(createAuthor, {
    onSuccess: () => {
      navigate('/authors', { state: { notification: { type: 'success', message: 'Author added successfully!' } } });
    }
  });

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
    navigate('/authors');
  };

  const isFormValid = author.firstName.trim() !== '' && author.lastName.trim() !== '';
  const isSaveDisabled = !isFormValid || isSaving;

  return (
    <AddPage title={t('addAuthor.title')} notification={notification} setNotification={setNotification}>
      <Form onSave={handleSave} onCancel={handleCancel} isSaveDisabled={isSaveDisabled}>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="firstName">{t('addAuthor.form.firstName')}:</label>
          <input type="text" id="firstName" name="firstName" value={author.firstName} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="lastName">{t('addAuthor.form.lastName')}:</label>
          <input type="text" id="lastName" name="lastName" value={author.lastName} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="bio">{t('addAuthor.form.bio')}:</label>
          <textarea id="bio" name="bio" value={author.bio} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="birthDate">{t('addAuthor.form.birthDate')}:</label>
          <input type="date" id="birthDate" name="birthDate" value={author.birthDate} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
        <div className="mb-4">
          <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="deathDate">{t('addAuthor.form.deathDate')}:</label>
          <input type="date" id="deathDate" name="deathDate" value={author.deathDate} onChange={handleChange} className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
        </div>
      </Form>
    </AddPage>
  );
};

export default AddAuthor;