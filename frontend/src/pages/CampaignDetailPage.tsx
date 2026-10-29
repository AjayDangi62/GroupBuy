import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { campaignApi } from '../api/campaignApi';
import { participationApi } from '../api/participationApi';
import { CampaignDetail } from '../types/campaign';
import { PricingTiersVisualizer } from '../components/campaign/PricingTiersVisualizer';
import { CountdownTimer } from '../components/common/CountdownTimer';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorAlert } from '../components/common/ErrorAlert';
import { useAuth } from '../context/AuthContext';
import { MOCK_CAMPAIGNS } from '../mock/mockData';
import { formatCurrency } from '../utils/currency';
import {
  Users,
  Sparkles,
  ShieldCheck,
  CheckCircle2,
  Clock,
  ArrowRight,
  TrendingDown,
  AlertTriangle,
  Minus,
  Plus,
} from 'lucide-react';

export const CampaignDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();

  const [detail, setDetail] = useState<CampaignDetail | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [quantity, setQuantity] = useState<number>(1);
  const [joining, setJoining] = useState<boolean>(false);
  const [joinSuccess, setJoinSuccess] = useState<boolean>(false);
  const [joinError, setJoinError] = useState<string | null>(null);

  const fetchCampaign = async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const data = await campaignApi.getCampaignDetails(Number(id));
      setDetail(data);
    } catch {
      // Fallback to mock data
      const mockCamp = MOCK_CAMPAIGNS.find((c) => c.id === Number(id));
      if (mockCamp) {
        setDetail({
          campaign: mockCamp,
          currentQuote: {
            eligibleQuantity: 1,
            unitPrice: mockCamp.pricingTiers[1].unitPrice,
            subtotal: mockCamp.pricingTiers[1].unitPrice,
            currency: mockCamp.currency,
            tierMinQuantity: mockCamp.pricingTiers[1].minQuantity,
            tierMaxQuantity: mockCamp.pricingTiers[1].maxQuantity,
            tierIndex: 1,
            policyType: 'TIERED',
            summary: 'Tier 2 volume discount unlocked',
          },
          quantityProgressPercent: Math.round((mockCamp.currentPaidUnits / mockCamp.targetMinQuantity) * 100),
          targetMet: mockCamp.currentPaidUnits >= mockCamp.targetMinQuantity,
        });
      } else {
        setError('Campaign not found or failed to load.');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCampaign();
  }, [id]);

  const handleJoinCampaign = async () => {
    if (!detail) return;
    if (!isAuthenticated) {
      navigate(`/login?redirect=/campaigns/${detail.campaign.id}`);
      return;
    }

    setJoining(true);
    setJoinError(null);
    try {
      await participationApi.joinCampaign(detail.campaign.id, { quantity });
      setJoinSuccess(true);
      setTimeout(() => {
        setJoinSuccess(false);
        navigate('/participations');
      }, 2000);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      setJoinError(e.response?.data?.message || e.message || 'Failed to join group deal.');
    } finally {
      setJoining(false);
    }
  };

  if (loading) {
    return <LoadingSpinner fullScreen message="Loading group deal..." />;
  }

  if (error || !detail) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-16 text-center">
        <ErrorAlert message={error || 'Campaign not found'} />
        <Link
          to="/campaigns"
          className="inline-block mt-4 text-sm font-semibold text-emerald-700 hover:text-emerald-800 underline"
        >
          Return to Group Deals
        </Link>
      </div>
    );
  }

  const { campaign, quantityProgressPercent, targetMet } = detail;

  const lowestTierPrice = campaign.pricingTiers.length > 0
    ? campaign.pricingTiers[campaign.pricingTiers.length - 1].unitPrice
    : campaign.productBasePrice;

  const currentTierPrice = campaign.pricingTiers.length > 0
    ? campaign.pricingTiers[0].unitPrice
    : campaign.productBasePrice;

  const maxDiscountPercent = campaign.productBasePrice > 0
    ? Math.round(((campaign.productBasePrice - lowestTierPrice) / campaign.productBasePrice) * 100)
    : 0;

  const estimatedTotal = currentTierPrice * quantity;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-10 bg-white">
      {/* Top Banner & Status */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-100 pb-6">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <span className="text-xs font-bold text-emerald-800 bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-200">
              Campaign #{campaign.id}
            </span>
            <span className="text-xs font-bold text-emerald-700 uppercase tracking-wider">
              {campaign.status}
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 leading-tight">
            {campaign.title}
          </h1>
        </div>

        {/* Countdown Box */}
        <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 flex items-center gap-4 shrink-0">
          <Clock className="w-5 h-5 text-emerald-700" />
          <div>
            <div className="text-[11px] font-bold text-slate-500 uppercase">Campaign Closes In:</div>
            <CountdownTimer targetDate={campaign.endsAt} />
          </div>
        </div>
      </div>

      {joinError && <ErrorAlert message={joinError} onDismiss={() => setJoinError(null)} />}

      {joinSuccess && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-900 rounded-2xl flex items-center gap-3">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
          <div>
            <p className="font-bold text-sm">Successfully Joined Campaign!</p>
            <p className="text-xs text-emerald-700">Redirecting to your participations dashboard...</p>
          </div>
        </div>
      )}

      {/* Main Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
        {/* Left Column: Image & Details */}
        <div className="lg:col-span-7 space-y-8">
          <div className="relative aspect-16/10 bg-slate-50 border border-slate-200 rounded-3xl overflow-hidden shadow-xs">
            {campaign.productImageUrl ? (
              <img
                src={campaign.productImageUrl}
                alt={campaign.title}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-slate-300">
                <Sparkles className="w-16 h-16" />
              </div>
            )}
            {maxDiscountPercent > 0 && (
              <div className="absolute top-4 left-4 bg-emerald-700 text-white font-black text-sm px-4 py-1.5 rounded-full shadow-lg">
                SAVE UP TO {maxDiscountPercent}%
              </div>
            )}
          </div>

          {/* Pricing Tiers Visualizer Ladder */}
          <PricingTiersVisualizer
            tiers={campaign.pricingTiers}
            currentPaidUnits={campaign.currentPaidUnits}
            basePrice={campaign.productBasePrice}
            currency={campaign.currency}
          />

          {/* Description */}
          <div className="bg-slate-50 border border-slate-200 rounded-2xl p-6 space-y-3">
            <h2 className="text-base font-bold text-slate-900">Campaign Details & Specs</h2>
            <p className="text-sm text-slate-600 leading-relaxed whitespace-pre-line">
              {campaign.description || 'Join fellow shoppers to pool purchasing power and reach higher volume tiers directly with verified manufacturers.'}
            </p>
            <div className="pt-3 border-t border-slate-200 flex flex-wrap gap-4 text-xs text-slate-500">
              <span>Product SKU: <strong className="text-slate-800 font-mono">{campaign.productSku}</strong></span>
              <span>•</span>
              <span>Retail Base: <strong className="text-slate-800">{formatCurrency(campaign.productBasePrice, campaign.currency)}</strong></span>
            </div>
          </div>
        </div>

        {/* Right Column: Commit / Join Box */}
        <div className="lg:col-span-5 space-y-6">
          <div className="bg-white border border-slate-200 rounded-3xl p-6 shadow-lg shadow-slate-100 space-y-6 sticky top-24">
            <div>
              <span className="text-xs font-bold text-emerald-800 uppercase tracking-wider block mb-1">
                Target Progress
              </span>
              <div className="flex items-baseline justify-between">
                <span className="text-2xl font-black text-slate-900">
                  {campaign.currentPaidUnits} / {campaign.targetMinQuantity} Units
                </span>
                <span className="text-sm font-bold text-emerald-700">
                  {quantityProgressPercent}% Met
                </span>
              </div>

              {/* Progress Bar */}
              <div className="w-full bg-slate-100 h-3 rounded-full mt-2.5 overflow-hidden">
                <div
                  className="bg-emerald-600 h-3 rounded-full transition-all duration-500"
                  style={{ width: `${Math.min(100, quantityProgressPercent)}%` }}
                />
              </div>

              <div className="mt-3 flex items-center justify-between text-xs text-slate-500">
                <span className="flex items-center gap-1">
                  <Users className="w-4 h-4 text-emerald-600" />
                  {campaign.currentParticipants} active buyers committed
                </span>
                <span>Min Required: {campaign.minParticipants} buyers</span>
              </div>
            </div>

            {/* Current Tier Price Box */}
            <div className="p-4 bg-emerald-50/70 border border-emerald-200 rounded-2xl space-y-1">
              <div className="text-xs font-semibold text-emerald-800">Current Unit Price</div>
              <div className="flex items-baseline gap-2">
                <span className="text-3xl font-black text-slate-900">
                  {formatCurrency(currentTierPrice, campaign.currency)}
                </span>
                <span className="text-xs text-slate-400 line-through">
                  {formatCurrency(campaign.productBasePrice, campaign.currency)}
                </span>
              </div>
              <p className="text-[11px] text-emerald-700 font-medium">
                * You only authorize this price ceiling. If further tiers unlock, you are refunded the difference!
              </p>
            </div>

            {/* Quantity Selector */}
            <div className="space-y-2">
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                Select Your Order Quantity:
              </label>
              <div className="flex items-center border border-slate-300 rounded-xl bg-white shadow-xs">
                <button
                  type="button"
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                  disabled={quantity <= 1 || joining}
                  className="p-3 text-slate-600 hover:text-emerald-700 disabled:opacity-30 transition"
                >
                  <Minus className="w-4 h-4" />
                </button>
                <span className="flex-1 text-center font-bold text-base text-slate-900">{quantity}</span>
                <button
                  type="button"
                  onClick={() => setQuantity(quantity + 1)}
                  disabled={joining}
                  className="p-3 text-slate-600 hover:text-emerald-700 disabled:opacity-30 transition"
                >
                  <Plus className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Estimated Total Calculation */}
            <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-sm">
              <span className="text-slate-600">Max Authorization:</span>
              <span className="text-xl font-black text-slate-900">
                {formatCurrency(estimatedTotal, campaign.currency)}
              </span>
            </div>

            {/* Join Action */}
            <button
              type="button"
              onClick={handleJoinCampaign}
              disabled={joining || campaign.status !== 'ACTIVE'}
              className="w-full py-4 px-6 rounded-xl font-black text-sm bg-emerald-700 hover:bg-emerald-800 text-white shadow-md shadow-emerald-700/20 transition-all flex items-center justify-center gap-2 disabled:opacity-50 cursor-pointer"
            >
              {joining ? (
                <span>Joining Campaign...</span>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  <span>Commit & Join Group Buy</span>
                </>
              )}
            </button>

            {/* Assurance */}
            <div className="pt-2 text-[11px] text-slate-500 space-y-1.5">
              <div className="flex items-center gap-1.5">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                <span>Price Protection: never charged above current tier</span>
              </div>
              <div className="flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                <span>Differential refunds issued automatically upon finalization</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
