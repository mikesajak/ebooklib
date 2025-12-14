import React from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

const AuthorGroupTable = ({ authors, openConfirmDialog }) => {
  const { t } = useTranslation();

  return (
    <div className="bg-white shadow-md rounded my-6">
      <table className="min-w-full table-auto">
        <thead>
          <tr className="bg-gray-200 text-gray-600 uppercase text-sm leading-normal">
            <th className="py-3 px-6 text-left w-1/2">{t('authorList.header.name')}</th>
            <th className="py-3 px-6 text-left w-1/4">{t('authorList.header.bookCount')}</th>
            <th className="py-3 px-6 text-center w-1/4">{t('common.actions')}</th>
          </tr>
        </thead>
        <tbody className="text-gray-600 text-sm font-light">
          {authors.map((author) => (
            <tr key={author.id} className="border-b border-gray-200 hover:bg-gray-100">
              <td className="py-3 px-6 text-left whitespace-nowrap w-1/2">
                <Link to={`/author/${author.id}`} className="author-link">
                  {author.firstName} {author.lastName}
                </Link>
              </td>
              <td className="py-3 px-6 text-left w-1/4">{author.bookCount}</td>
              <td className="py-3 px-6 text-center whitespace-nowrap text-sm font-medium w-1/4">
                <Link to={`/authors/${author.id}/edit`} className="text-indigo-600 hover:text-indigo-900 mr-2">{t('common.edit')}</Link>
                <button onClick={() => openConfirmDialog(author)} className="text-red-600 hover:text-red-900">{t('common.delete')}</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default AuthorGroupTable;