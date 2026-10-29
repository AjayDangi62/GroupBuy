import React from 'react';
import { Loader2 } from 'lucide-react';

export interface LoadingSpinnerProps {
  message?: string;
  text?: string;
  size?: 'sm' | 'md' | 'lg';
  fullScreen?: boolean;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  message,
  text,
  size = 'md',
  fullScreen = false,
}) => {
  const displayMessage = text || message || 'Loading...';
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-8 h-8',
    lg: 'w-12 h-12',
  };

  const content = (
    <div className="flex flex-col items-center justify-center p-8 space-y-3">
      <Loader2 className={`${sizeClasses[size]} animate-spin text-emerald-700`} />
      {displayMessage && <p className="text-sm font-medium text-slate-500 animate-pulse">{displayMessage}</p>}
    </div>
  );

  if (fullScreen) {
    return (
      <div className="fixed inset-0 bg-white/80 backdrop-blur-xs flex items-center justify-center z-50">
        {content}
      </div>
    );
  }

  return content;
};
