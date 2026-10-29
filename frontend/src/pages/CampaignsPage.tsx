import React, { useEffect, useState } from 'react';
import { campaignApi } from '../api/campaignApi';
import { Campaign } from '../types/campaign';
import { CampaignCard } from '../components/campaign/CampaignCard';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';
import { MOCK_CAMPAIGNS } from '../mock/mockData';
import { Sparkles, ArrowUpDown } from 'lucide-react';

export const CampaignsPage: React.FC = () => {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [sortParam, setSortParam] = useState<string>('endsAt,asc');

  useEffect(() => {
    const fetchCampaigns = async () => {
      setLoading(true);
      try {
        const res = await campaignApi.getActiveCampaigns(0, 50, sortParam);
        if (res && res.content && res.content.length > 0) {
          setCampaigns(res.content);
        } else {
          setCampaigns(MOCK_CAMPAIGNS);
        }
      } catch {
        setCampaigns(MOCK_CAMPAIGNS);
      } finally {
        setLoading(false);
      }
    };

    fetchCampaigns();
  }, [sortParam]);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8 bg-white min-h-[80vh]">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-200 pb-6">
        <div>
          <div className="flex items-center gap-2 text-emerald-800 font-bold text-xs uppercase tracking-wider mb-1">
            <Sparkles className="w-4 h-4 text-emerald-600" />
            <span>Time-Sensitive Collective Deals</span>
          </div>
          <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">Active Group-Buying Deals</h1>
          <p className="text-sm text-slate-500 mt-1">
            Commit early at the current tier price. As more shoppers participate, unit costs automatically drop for all members.
          </p>
        </div>

        {/* Sort */}
        <div className="flex items-center gap-3 self-start sm:self-auto">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-500">
            <ArrowUpDown className="w-4 h-4" />
            <span>Sort:</span>
          </div>
          <select
            value={sortParam}
            onChange={(e) => setSortParam(e.target.value)}
            className="bg-white border border-slate-200 text-slate-800 text-sm rounded-xl py-2 px-3 focus:outline-hidden focus:ring-2 focus:ring-emerald-500 cursor-pointer"
          >
            <option value="endsAt,asc">Ending Soonest</option>
            <option value="createdAt,desc">Recently Launched</option>
            <option value="currentPaidUnits,desc">Most Popular</option>
          </select>
        </div>
      </div>

      {/* Grid / Empty / Loading */}
      {loading ? (
        <div className="py-24 flex justify-center">
          <LoadingSpinner size="lg" text="Loading active group deals..." />
        </div>
      ) : campaigns.length === 0 ? (
        <EmptyState
          title="No Active Group Campaigns"
          description="There are currently no active group drops open for participation. Check back soon or browse our product catalog."
          actionText="Explore All Products"
          actionLink="/products"
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {campaigns.map((c) => (
            <CampaignCard key={c.id} campaign={c} />
          ))}
        </div>
      )}
    </div>
  );
};
