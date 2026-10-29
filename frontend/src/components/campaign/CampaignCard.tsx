import React from 'react';
import { Link } from 'react-router-dom';
import { Campaign } from '../../types/campaign';
import { CountdownTimer } from '../common/CountdownTimer';
import { Users, Sparkles, ArrowRight, ShoppingBag } from 'lucide-react';
import { formatCurrency } from '../../utils/currency';

interface CampaignCardProps {
  campaign: Campaign;
}

export const CampaignCard: React.FC<CampaignCardProps> = ({ campaign }) => {
  const currentPrice = campaign.pricingTiers.length > 0
    ? campaign.pricingTiers[0].unitPrice
    : campaign.productBasePrice;

  const lowestTierPrice = campaign.pricingTiers.length > 0
    ? campaign.pricingTiers[campaign.pricingTiers.length - 1].unitPrice
    : campaign.productBasePrice;

  const savingsAmount = Math.max(0, campaign.productBasePrice - lowestTierPrice);
  const maxDiscount = campaign.productBasePrice > 0
    ? Math.round((savingsAmount / campaign.productBasePrice) * 100)
    : 0;

  const progressPercent = campaign.targetMinQuantity > 0
    ? Math.min(100, Math.round((campaign.currentPaidUnits / campaign.targetMinQuantity) * 100))
    : 0;

  // Find next tier if applicable
  const nextTier = campaign.pricingTiers.find((t) => t.minQuantity > campaign.currentPaidUnits);
  const unitsToNextTier = nextTier ? nextTier.minQuantity - campaign.currentPaidUnits : null;

  return (
    <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-xs hover:shadow-xl hover:border-emerald-300 transition-all duration-300 flex flex-col group">
      {/* Product Image & Top Overlays */}
      <div className="relative aspect-4/3 bg-slate-50 overflow-hidden">
        {campaign.productImageUrl ? (
          <img
            src={campaign.productImageUrl}
            alt={campaign.title}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-slate-300">
            <Sparkles className="w-12 h-12" />
          </div>
        )}

        {/* Max discount pill */}
        {maxDiscount > 0 && (
          <span className="absolute top-3 left-3 bg-emerald-700 text-white text-xs font-black px-3 py-1 rounded-full shadow-md tracking-wide">
            SAVE UP TO {maxDiscount}% ({formatCurrency(savingsAmount, campaign.currency)})
          </span>
        )}

        {/* Countdown pill */}
        <div className="absolute top-3 right-3">
          <CountdownTimer targetDate={campaign.endsAt} compact />
        </div>
      </div>

      {/* Content */}
      <div className="p-5 flex-1 flex flex-col justify-between">
        <div>
          <div className="text-xs font-bold text-emerald-700 mb-1 uppercase tracking-wider">
            {campaign.productName}
          </div>

          <Link to={`/campaigns/${campaign.id}`}>
            <h3 className="font-bold text-slate-900 text-base leading-snug line-clamp-2 group-hover:text-emerald-700 transition-colors">
              {campaign.title}
            </h3>
          </Link>

          {/* Pricing Comparison */}
          <div className="mt-3 flex flex-wrap items-baseline gap-2">
            <span className="text-2xl font-black text-slate-900">
              {formatCurrency(lowestTierPrice, campaign.currency)}
            </span>
            <span className="text-sm text-slate-400 line-through">
              {formatCurrency(campaign.productBasePrice, campaign.currency)}
            </span>
            <span className="text-xs text-emerald-700 font-semibold">
              starts at {formatCurrency(currentPrice, campaign.currency)}
            </span>
          </div>

          {/* Progress Section */}
          <div className="mt-4 bg-slate-50 border border-slate-100 rounded-xl p-3">
            <div className="flex items-center justify-between text-xs font-bold mb-1.5">
              <span className="text-slate-700">
                {campaign.currentPaidUnits} / {campaign.targetMinQuantity} Units
              </span>
              <span className="text-emerald-700">{progressPercent}%</span>
            </div>
            <div className="w-full bg-slate-200 h-2.5 rounded-full overflow-hidden">
              <div
                className="bg-emerald-600 h-2.5 rounded-full transition-all duration-500"
                style={{ width: `${progressPercent}%` }}
              />
            </div>

            <div className="mt-2 flex items-center justify-between text-xs text-slate-500">
              <span className="flex items-center">
                <Users className="w-3.5 h-3.5 mr-1 text-slate-400" />
                {campaign.currentParticipants} active {campaign.currentParticipants === 1 ? 'buyer' : 'buyers'}
              </span>
              {unitsToNextTier && unitsToNextTier > 0 && nextTier && (
                <span className="text-emerald-700 font-medium">
                  {unitsToNextTier} more to {formatCurrency(nextTier.unitPrice, campaign.currency)}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Action Buttons: Join Group & Buy Solo */}
        <div className="mt-5 pt-3 border-t border-slate-100 flex items-center gap-2">
          <Link
            to={`/campaigns/${campaign.id}`}
            className="flex-1 inline-flex items-center justify-center gap-1.5 bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-xs py-2.5 px-3 rounded-xl transition-colors shadow-xs group/btn"
          >
            <span>Join Group</span>
            <ArrowRight className="w-3.5 h-3.5 group-hover/btn:translate-x-1 transition-transform" />
          </Link>
          <Link
            to={`/products/${campaign.productId}`}
            className="inline-flex items-center justify-center gap-1 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs py-2.5 px-3 rounded-xl transition-colors"
            title="Buy individually at retail price without waiting"
          >
            <ShoppingBag className="w-3.5 h-3.5 text-slate-500" />
            <span>Buy Solo</span>
          </Link>
        </div>
      </div>
    </div>
  );
};
