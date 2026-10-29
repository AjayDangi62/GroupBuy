import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { participationApi } from '../api/participationApi';
import { Participation } from '../types/participation';
import { Badge } from '../components/common/Badge';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';
import { ErrorAlert } from '../components/common/ErrorAlert';
import { Sparkles, Users, XCircle, ArrowRight } from 'lucide-react';
import { formatCurrency } from '../utils/currency';

export const MyParticipationsPage: React.FC = () => {
  const [participations, setParticipations] = useState<Participation[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [cancellingId, setCancellingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const fetchParticipations = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await participationApi.getMyParticipations(0, 50);
      setParticipations(res.content || []);
    } catch {
      setError('Failed to load group-buy participations.');
      setParticipations([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchParticipations();
  }, []);

  const handleCancelParticipation = async (campaignId: number) => {
    if (!window.confirm('Are you sure you want to cancel your participation? Your reserved stock hold will be released.')) {
      return;
    }

    setCancellingId(campaignId);
    try {
      await participationApi.cancelParticipation(campaignId);
      await fetchParticipations();
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setError(e.response?.data?.message || 'Failed to cancel participation.');
    } finally {
      setCancellingId(null);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8 bg-white min-h-[80vh]">
      <div>
        <div className="flex items-center gap-2 text-emerald-700 font-bold text-xs uppercase tracking-wider mb-1">
          <Sparkles className="w-4 h-4 text-emerald-600" />
          <span>My Group Commitments</span>
        </div>
        <h1 className="text-3xl font-black text-slate-900 tracking-tight">Joined Group Deals</h1>
        <p className="text-sm text-slate-500 mt-1">
          Track campaigns you have joined, live unlock progress, and automatic differential refunds upon settlement.
        </p>
      </div>

      {error && <ErrorAlert message={error} onDismiss={() => setError(null)} />}

      {loading ? (
        <div className="py-24 flex justify-center">
          <LoadingSpinner size="lg" text="Loading your commitments..." />
        </div>
      ) : participations.length === 0 ? (
        <EmptyState
          title="No Group Commitments Yet"
          description="You haven't joined any group-buy deals. Explore active drops to save with community volume discounts."
          actionText="Browse Live Group Deals"
          actionLink="/campaigns"
        />
      ) : (
        <div className="space-y-4">
          {participations.map((part) => {
            const canCancel = part.status === 'CONFIRMED' || part.status === 'PENDING';

            return (
              <div
                key={part.id}
                className="bg-white border border-slate-200 rounded-2xl p-6 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-6"
              >
                <div className="space-y-2">
                  <div className="flex items-center gap-2.5">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-700 bg-emerald-50 px-2.5 py-0.5 rounded-full border border-emerald-200">
                      Campaign #{part.campaignId}
                    </span>
                    <Badge status={part.status} />
                  </div>

                  <h3 className="font-bold text-slate-900 text-base">
                    Order Commitment for Campaign #{part.campaignId}
                  </h3>

                  <div className="flex flex-wrap items-center gap-4 text-xs text-slate-500">
                    <span>
                      Pledged: <strong className="text-slate-800">{part.quantity} units</strong>
                    </span>
                    <span>•</span>
                    <span>
                      Max Authorized: <strong className="text-slate-800">{formatCurrency(part.authorizedUnitPrice, part.currency)} / unit</strong>
                    </span>
                    {part.finalUnitPrice && (
                      <>
                        <span>•</span>
                        <span className="text-emerald-700 font-bold">
                          Settled At: {formatCurrency(part.finalUnitPrice, part.currency)}
                        </span>
                      </>
                    )}
                    {part.finalTotalAmount && part.authorizedTotalAmount > part.finalTotalAmount && (
                      <>
                        <span>•</span>
                        <span className="text-emerald-700 font-bold">
                          Refund Credit: {formatCurrency(part.authorizedTotalAmount - part.finalTotalAmount, part.currency)}
                        </span>
                      </>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-3 self-end md:self-center">
                  <Link
                    to={`/campaigns/${part.campaignId}`}
                    className="font-bold text-emerald-700 hover:text-emerald-800 flex items-center gap-1 text-sm"
                  >
                    <span>View Campaign</span>
                    <ArrowRight className="w-4 h-4" />
                  </Link>

                  {canCancel && (
                    <button
                      onClick={() => handleCancelParticipation(part.campaignId)}
                      disabled={cancellingId === part.campaignId}
                      className="text-xs font-semibold text-rose-600 hover:text-rose-700 border border-rose-200 hover:bg-rose-50 px-3 py-1.5 rounded-lg transition disabled:opacity-50 cursor-pointer"
                    >
                      {cancellingId === part.campaignId ? 'Cancelling...' : 'Cancel Hold'}
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
