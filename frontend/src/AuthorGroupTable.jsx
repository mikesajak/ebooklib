import React from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FaEdit, FaTrash } from 'react-icons/fa';

const AuthorGroupTable = ({ authors, openConfirmDialog }) => {
  const { t } = useTranslation();

  return (
    <div className="bg-white overflow-hidden">
      <table className="min-w-full table-auto">
        <thead>
          <tr className="bg-gray-50 border-b border-gray-200 text-gray-600 uppercase text-[10px] font-black tracking-widest">
            <th className="py-4 px-6 text-left w-1/2">{t('authorList.header.name')}</th>
            <th className="py-4 px-6 text-left w-1/4">{t('authorList.header.bookCount')}</th>
            <th className="py-4 px-6 text-center w-1/4">{t('common.actions')}</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {authors.map((author) => (
            <tr key={author.id} className="hover:bg-emerald-50/50 transition-colors group">
              <td className="py-4 px-6 text-left whitespace-nowrap w-1/2">
                <Link to={`/author/${author.id}`} className="author-link group-hover:text-emerald-700">
                  {author.firstName} {author.lastName}
                </Link>
              </td>
              <td className="py-4 px-6 text-left w-1/4 text-sm text-gray-600 font-medium">
                <span className="bg-gray-100 px-2 py-0.5 rounded-full border border-gray-200">{author.bookCount}</span>
              </td>
              <td className="py-4 px-6 text-center whitespace-nowrap w-1/4">
                <div className="flex justify-center gap-2">
                  <Link 
                    to={`/authors/${author.id}/edit`} 
                    className="bg-white p-2 text-indigo-600 hover:bg-indigo-600 hover:text-white rounded-lg border border-indigo-100 shadow-sm transition-all"
                    title={t('common.edit')}
                  >
                    <FaEdit />
                  </Link>
                  <button 
                    onClick={() => openConfirmDialog(author)} 
                    className="bg-white p-2 text-red-600 hover:bg-red-600 hover:text-white rounded-lg border border-red-100 shadow-sm transition-all"
                    title={t('common.delete')}
                  >
                    <FaTrash />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default AuthorGroupTable;
