import React from 'react';
import { AlertCircle, X } from 'lucide-react';

interface ErrorAlertProps {
  message: string;
  onDismiss?: () => void;
}

export const ErrorAlert: React.FC<ErrorAlertProps> = ({ message, onDismiss }) => {
  if (!message) return null;

  return (
    <div className="bg-rose-50 border border-rose-200 text-rose-800 rounded-xl p-4 flex items-start justify-between shadow-xs mb-4">
      <div className="flex items-start space-x-3">
        <AlertCircle className="w-5 h-5 text-rose-500 mt-0.5 shrink-0" />
        <div>
          <h4 className="text-sm font-semibold text-rose-900">Notice</h4>
          <p className="text-sm text-rose-700 mt-0.5">{message}</p>
        </div>
      </div>
      {onDismiss && (
        <button
          onClick={onDismiss}
          className="text-rose-400 hover:text-rose-600 transition-colors p-1"
          aria-label="Dismiss error"
        >
          <X className="w-4 h-4" />
        </button>
      )}
    </div>
  );
};

