import React, { useEffect, useState } from 'react';
import { Sparkles, CheckCircle2, XCircle, RefreshCw, Eye, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import { campaignApi } from '../../api/campaignApi';
import { Campaign } from '../../types/campaign';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { ErrorAlert } from '../../components/common/ErrorAlert';
import { Badge } from '../../components/common/Badge';
import { formatCurrency } from '../../utils/currency';

export const AdminCampaignsPage: React.FC = () => {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [actionInProgress, setActionInProgress] = useState<number | null>(null);

  const fetchCampaigns = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await campaignApi.getAllCampaignsAdmin(0, 50);
      setCampaigns(res.content);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      setError(e.response?.data?.message || e.message || 'Failed to load campaigns');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCampaigns();
  }, []);

  const handleFinalize = async (c: Campaign) => {
    if (!window.confirm(`Finalize campaign #${c.id} ("${c.title}")? This will calculate tier discounts and lock the campaign.`)) {
      return;
    }

    try {
      setActionInProgress(c.id);
      setError(null);
      await campaignApi.finalizeCampaignAdmin(c.id);
      setActionSuccess(`Campaign #${c.id} was finalized successfully.`);
      await fetchCampaigns();
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      setError(e.response?.data?.message || e.message || 'Failed to finalize campaign');
    } finally {
      setActionInProgress(null);
    }
  };

  const handleCancel = async (c: Campaign) => {
    if (!window.confirm(`Cancel campaign #${c.id} ("${c.title}")? This cannot be undone.`)) {
      return;
    }

    try {
      setActionInProgress(c.id);
      setError(null);
      await campaignApi.cancelCampaign(c.id);
      setActionSuccess(`Campaign #${c.id} was cancelled.`);
      await fetchCampaigns();
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      setError(e.response?.data?.message || e.message || 'Failed to cancel campaign');
    } finally {
      setActionInProgress(null);
    }
  };

  const getStatusBadgeVariant = (status: Campaign['status']) => {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'SUCCESSFUL':
        return 'info';
      case 'SCHEDULED':
        return 'warning';
      case 'EXPIRED':
      case 'CANCELLED':
      case 'FAILED':
      default:
        return 'neutral';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
            <Sparkles className="w-6 h-6 text-purple-600" />
            Group-Buy Campaigns Management
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Monitor live group campaigns, verify tier progress, and manually finalize or cancel deals.
          </p>
        </div>
        <button
          onClick={fetchCampaigns}
          className="inline-flex items-center gap-2 px-4 py-2 border border-slate-300 rounded-lg text-sm text-slate-700 hover:bg-slate-50 transition"
        >
          <RefreshCw className="w-4 h-4" />
          Refresh
        </button>
      </div>

      {actionSuccess && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-sm flex items-center justify-between">
          <span>{actionSuccess}</span>
          <button onClick={() => setActionSuccess(null)} className="text-emerald-600 hover:text-emerald-900 font-bold ml-2">
            ×
          </button>
        </div>
      )}

      {error && <ErrorAlert message={error} onDismiss={() => setError(null)} />}

      {loading ? (
        <div className="py-20 flex justify-center">
          <LoadingSpinner size="lg" text="Loading campaigns..." />
        </div>
      ) : (
        <div className="bg-white border border-slate-200 rounded-xl overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-slate-700 font-semibold text-xs uppercase border-b border-slate-200">
                <tr>
                  <th className="px-6 py-4">Campaign</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4">Qty / Target</th>
                  <th className="px-6 py-4">Participants</th>
                  <th className="px-6 py-4">Current Price</th>
                  <th className="px-6 py-4">Ends At</th>
                  <th className="px-6 py-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {campaigns.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-6 py-8 text-center text-slate-400">
                      No campaigns found.
                    </td>
                  </tr>
                ) : (
                  campaigns.map((c) => {
                    const isBusy = actionInProgress === c.id;
                    const canFinalize = c.status === 'ACTIVE';
                    const canCancel = c.status === 'ACTIVE' || c.status === 'SCHEDULED';

                    return (
                      <tr key={c.id} className="hover:bg-slate-50/60 transition-colors">
                        <td className="px-6 py-4">
                          <div>
                            <span className="font-semibold text-slate-900 block">{c.title}</span>
                            <span className="text-xs text-slate-400 font-mono">
                              Product ID #{c.productId} • Campaign #{c.id}
                            </span>
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          <Badge variant={getStatusBadgeVariant(c.status)}>
                            {c.status}
                          </Badge>
                        </td>
                        <td className="px-6 py-4">
                          <div className="text-xs">
                            <span className="font-semibold text-slate-800">{c.currentPaidUnits}</span>
                            <span className="text-slate-400"> / {c.targetMinQuantity} units</span>
                          </div>
                        </td>
                        <td className="px-6 py-4 font-medium text-slate-700">
                          {c.currentParticipants} / min {c.minParticipants}
                        </td>
                        <td className="px-6 py-4 font-semibold text-emerald-600">
                          {formatCurrency(c.finalUnitPrice ?? c.pricingTiers[0]?.unitPrice ?? c.productBasePrice, c.currency)}
                        </td>
                        <td className="px-6 py-4 text-xs text-slate-500">
                          {new Date(c.endsAt).toLocaleDateString()} {new Date(c.endsAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </td>
                        <td className="px-6 py-4 text-right">
                          <div className="flex items-center justify-end gap-2">
                            <Link
                              to={`/campaigns/${c.id}`}
                              className="p-1.5 text-slate-500 hover:text-purple-600 rounded hover:bg-slate-100 transition"
                              title="View Public Campaign"
                            >
                              <Eye className="w-4 h-4" />
                            </Link>

                            {canFinalize && (
                              <button
                                onClick={() => handleFinalize(c)}
                                disabled={isBusy}
                                className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold rounded bg-purple-50 text-purple-700 border border-purple-200 hover:bg-purple-100 transition disabled:opacity-50"
                                title="Finalize Campaign"
                              >
                                <CheckCircle2 className="w-3.5 h-3.5" />
                                Finalize
                              </button>
                            )}

                            {canCancel && (
                              <button
                                onClick={() => handleCancel(c)}
                                disabled={isBusy}
                                className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold rounded bg-red-50 text-red-700 border border-red-200 hover:bg-red-100 transition disabled:opacity-50"
                                title="Cancel Campaign"
                              >
                                <XCircle className="w-3.5 h-3.5" />
                                Cancel
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
