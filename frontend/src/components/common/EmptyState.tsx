import React from 'react';
import { PackageOpen } from 'lucide-react';
import { Link } from 'react-router-dom';

interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description: string;
  actionText?: string;
  actionLink?: string;
  onAction?: () => void;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title,
  description,
  actionText,
  actionLink,
  onAction,
}) => {
  return (
    <div className="text-center py-12 px-4 max-w-md mx-auto">
      <div className="w-16 h-16 bg-slate-100 text-slate-400 rounded-2xl flex items-center justify-center mx-auto mb-4">
        {icon || <PackageOpen className="w-8 h-8" />}
      </div>
      <h3 className="text-lg font-semibold text-slate-800">{title}</h3>
      <p className="text-sm text-slate-500 mt-1 mb-6">{description}</p>
      {actionText && actionLink && (
        <Link
          to={actionLink}
          className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-xl shadow-xs text-white bg-indigo-600 hover:bg-indigo-700 transition-colors"
        >
          {actionText}
        </Link>
      )}
      {actionText && onAction && !actionLink && (
        <button
          onClick={onAction}
          className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-xl shadow-xs text-white bg-indigo-600 hover:bg-indigo-700 transition-colors cursor-pointer"
        >
          {actionText}
        </button>
      )}
    </div>
  );
};

