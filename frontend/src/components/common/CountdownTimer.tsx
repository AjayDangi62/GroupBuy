import React, { useEffect, useState } from 'react';
import { Clock } from 'lucide-react';

interface CountdownTimerProps {
  targetDate: string;
  onExpire?: () => void;
  compact?: boolean;
}

interface TimeLeft {
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
  isExpired: boolean;
}

export const CountdownTimer: React.FC<CountdownTimerProps> = ({
  targetDate,
  onExpire,
  compact = false,
}) => {
  const calculateTimeLeft = (): TimeLeft => {
    const diff = new Date(targetDate).getTime() - new Date().getTime();
    if (diff <= 0) {
      return { days: 0, hours: 0, minutes: 0, seconds: 0, isExpired: true };
    }

    return {
      days: Math.floor(diff / (1000 * 60 * 60 * 24)),
      hours: Math.floor((diff / (1000 * 60 * 60)) % 24),
      minutes: Math.floor((diff / 1000 / 60) % 60),
      seconds: Math.floor((diff / 1000) % 60),
      isExpired: false,
    };
  };

  const [timeLeft, setTimeLeft] = useState<TimeLeft>(calculateTimeLeft());

  useEffect(() => {
    const timer = setInterval(() => {
      const updated = calculateTimeLeft();
      setTimeLeft(updated);
      if (updated.isExpired) {
        clearInterval(timer);
        onExpire?.();
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [targetDate, onExpire]);

  if (timeLeft.isExpired) {
    return (
      <span className="inline-flex items-center text-xs font-semibold text-rose-600 bg-rose-50 px-2 py-0.5 rounded-md border border-rose-200">
        <Clock className="w-3.5 h-3.5 mr-1" />
        Deal Ended
      </span>
    );
  }

  if (compact) {
    return (
      <span className="inline-flex items-center text-xs font-semibold text-amber-700 bg-amber-50 px-2.5 py-1 rounded-md border border-amber-200">
        <Clock className="w-3.5 h-3.5 mr-1 text-amber-600" />
        {timeLeft.days > 0 ? `${timeLeft.days}d ` : ''}
        {String(timeLeft.hours).padStart(2, '0')}:{String(timeLeft.minutes).padStart(2, '0')}:
        {String(timeLeft.seconds).padStart(2, '0')}
      </span>
    );
  }

  return (
    <div className="flex items-center space-x-2 text-slate-800">
      <div className="flex flex-col items-center bg-slate-100 border border-slate-200 rounded-lg px-2.5 py-1 min-w-12">
        <span className="text-sm font-bold text-slate-900">{timeLeft.days}</span>
        <span className="text-[10px] text-slate-500 uppercase tracking-wider">Days</span>
      </div>
      <span className="text-slate-400 font-bold">:</span>
      <div className="flex flex-col items-center bg-slate-100 border border-slate-200 rounded-lg px-2.5 py-1 min-w-12">
        <span className="text-sm font-bold text-slate-900">
          {String(timeLeft.hours).padStart(2, '0')}
        </span>
        <span className="text-[10px] text-slate-500 uppercase tracking-wider">Hours</span>
      </div>
      <span className="text-slate-400 font-bold">:</span>
      <div className="flex flex-col items-center bg-slate-100 border border-slate-200 rounded-lg px-2.5 py-1 min-w-12">
        <span className="text-sm font-bold text-slate-900">
          {String(timeLeft.minutes).padStart(2, '0')}
        </span>
        <span className="text-[10px] text-slate-500 uppercase tracking-wider">Mins</span>
      </div>
      <span className="text-slate-400 font-bold">:</span>
      <div className="flex flex-col items-center bg-slate-100 border border-slate-200 rounded-lg px-2.5 py-1 min-w-12">
        <span className="text-sm font-bold text-slate-900">
          {String(timeLeft.seconds).padStart(2, '0')}
        </span>
        <span className="text-[10px] text-slate-500 uppercase tracking-wider">Secs</span>
      </div>
    </div>
  );
};

