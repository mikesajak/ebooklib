import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import Notification from './Notification';

const BookDetails = () => {
  const { id } = useParams();
  const [book, setBook] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [editedBook, setEditedBook] = useState(null);
  const [authors, setAuthors] = useState([]);
  const [series, setSeries] = useState([]);
  const [notification, setNotification] = useState(null);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setEditedBook(prevBook => ({
      ...prevBook,
      [name]: value
    }));
  };

  const handleAuthorChange = (e) => {
    const selectedAuthorId = e.target.value;
    const selectedAuthor = authors.find(author => author.id === selectedAuthorId);
    setEditedBook(prevBook => ({
      ...prevBook,
      authors: selectedAuthor ? [selectedAuthor] : []
    }));
  };

  const handleSeriesChange = (e) => {
    const selectedSeriesId = e.target.value;
    const selectedSeries = series.find(s => s.id === selectedSeriesId);
    setEditedBook(prevBook => ({
      ...prevBook,
      series: selectedSeries ? selectedSeries : null
    }));
  };

  const fetchBook = async () => {
    try {
      const response = await fetch(`/api/books/${id}`);
      if (!response.ok) {
        throw new Error('Failed to fetch book details');
      }
      const data = await response.json();
      setBook(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    try {
      const bookData = {
        ...editedBook,
        authorIds: editedBook.authors.map(author => author.id),
        seriesId: editedBook.series ? editedBook.series.id : null
      };
      delete bookData.authors;
      delete bookData.series;

      const response = await fetch(`/api/books/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(bookData),
      });

      if (!response.ok) {
        let errorMessage = 'Failed to update book';
        try {
            const errorData = await response.json();
            errorMessage = errorData.message || errorMessage;
        } catch (e) {
            // ignore if response is not json
        }
        throw new Error(errorMessage);
      }

      const updatedBook = await response.json();
      setBook(updatedBook);
      setIsEditing(false);
      setNotification({ type: 'success', message: 'Book updated successfully!' });
    } catch (err) {
      setNotification({ type: 'error', message: err.message });
      fetchBook();
      setIsEditing(false);
    }
  };

  useEffect(() => {
    fetchBook();
  }, [id]);

  useEffect(() => {
    if (book) {
      setEditedBook({ ...book });
    }
  }, [book]);

  useEffect(() => {
    const fetchAuthors = async () => {
      try {
        const response = await fetch('/api/authors');
        if (!response.ok) {
          throw new Error('Failed to fetch authors');
        }
        const data = await response.json();
        setAuthors(data.content);
      } catch (err) {
        console.error('Error fetching authors:', err);
      }
    };
    fetchAuthors();
  }, []);

  useEffect(() => {
    const fetchSeries = async () => {
      try {
        const response = await fetch('/api/series');
        if (!response.ok) {
          throw new Error('Failed to fetch series');
        }
        const data = await response.json();
        setSeries(data.content);
      } catch (err) {
        console.error('Error fetching series:', err);
      }
    };
    fetchSeries();
  }, []);

  if (loading) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">Book Details</h1>
        <p className="text-center text-gray-500">Loading book details...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">Book Details</h1>
        <p className="text-center text-red-500">Error: {error}</p>
        <Link to="/" className="back-link">
          Back to List
        </Link>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">Book Details</h1>
      {notification && (
        <Notification
          message={notification.message}
          type={notification.type}
          onClose={() => setNotification(null)}
        />
      )}
      <div className="flex justify-end mb-4">
        <button
          onClick={() => setIsEditing(!isEditing)}
          className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
        >
          {isEditing ? 'Cancel' : 'Edit'}
        </button>
      </div>
      {isEditing ? (
        <div className="bg-white border border-gray-300 rounded p-6 shadow">
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="title">Title:</label>
            <input
              type="text"
              id="title"
              name="title"
              value={editedBook?.title || ''}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            />
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="author">Author:</label>
            <select
              id="author"
              name="authorId"
              value={editedBook?.authors?.[0]?.id || ''}
              onChange={handleAuthorChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            >
              <option value="">Select an author</option>
              {authors.map(author => (
                <option key={author.id} value={author.id}>
                  {author.name}
                </option>
              ))}
            </select>
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="series">Series:</label>
            <select
              id="series"
              name="seriesId"
              value={editedBook?.series?.id || ''}
              onChange={handleSeriesChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            >
              <option value="">Select a series</option>
              {series.map(s => (
                <option key={s.id} value={s.id}>
                  {s.title}
                </option>
              ))}
            </select>
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="volume">Volume:</label>
            <input
              type="number"
              id="volume"
              name="volume"
              value={editedBook?.volume || ''}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            />
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="creationDate">Creation Date:</label>
            <input
              type="date"
              id="creationDate"
              name="creationDate"
              value={editedBook?.creationDate || ''}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            />
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="publicationDate">Publication Date:</label>
            <input
              type="date"
              id="publicationDate"
              name="publicationDate"
              value={editedBook?.publicationDate || ''}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            />
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="publisher">Publisher:</label>
            <input
              type="text"
              id="publisher"
              name="publisher"
              value={editedBook?.publisher || ''}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            />
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="description">Description:</label>
            <textarea
              id="description"
              name="description"
              value={editedBook?.description || ''}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            />
          </div>
          <div className="flex justify-end mt-4">
            <button
              onClick={handleSave}
              className="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded mr-2"
            >
              Save
            </button>
            <button
              onClick={() => setIsEditing(false)}
              className="bg-gray-500 hover:bg-gray-700 text-white font-bold py-2 px-4 rounded"
            >
              Cancel
            </button>
          </div>
        </div>
      ) : (
        <div className="bg-white border border-gray-300 rounded p-6 shadow">
          <div className="mb-4">
            <strong>Title:</strong> {book.title}
          </div>
          <div className="mb-4">
            <strong>Authors:</strong> {book.authors.map((author, index) => (
              <span key={author.id}>
                <Link to={`/author/${author.id}`} className="author-link">
                  {author.name}
                </Link>
                {index < book.authors.length - 1 ? ', ' : ''}
              </span>
            ))}
          </div>
          <div className="mb-4">
            <strong>Series:</strong> {book.series ? <Link to={`/series/${book.series.id}`} className="series-link">{book.series.title}</Link> : 'N/A'}
          </div>
          <div className="mb-4">
            <strong>Volume:</strong> {book.volume || 'N/A'}
          </div>
          <div className="mb-4">
            <strong>Creation Date:</strong> {book.creationDate || 'N/A'}
          </div>
          <div className="mb-4">
            <strong>Publication Date:</strong> {book.publicationDate || 'N/A'}
          </div>
          <div className="mb-4">
            <strong>Publisher:</strong> {book.publisher || 'N/A'}
          </div>
          <div className="mb-4">
            <strong>Description:</strong> {book.description || 'N/A'}
          </div>
        </div>
      )}
      <Link to="/" className="back-link">
        Back to List
      </Link>
    </div>
  );
};

export default BookDetails;
