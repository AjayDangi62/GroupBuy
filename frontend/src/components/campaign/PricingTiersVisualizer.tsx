import React from 'react';
import { PricingTier } from '../../types/campaign';
import { CheckCircle2, TrendingDown } from 'lucide-react';
import { formatCurrency } from '../../utils/currency';

interface PricingTiersVisualizerProps {
  tiers: PricingTier[];
  currentPaidUnits: number;
  basePrice: number;
  currency?: string;
}

export const PricingTiersVisualizer: React.FC<PricingTiersVisualizerProps> = ({
  tiers,
  currentPaidUnits,
  basePrice,
  currency = 'INR',
}) => {
  if (!tiers || tiers.length === 0) return null;

  // Identify active tier index
  const activeTierIndex = tiers.findIndex((tier) => {
    if (tier.maxQuantity === null) {
      return currentPaidUnits >= tier.minQuantity;
    }
    return currentPaidUnits >= tier.minQuantity && currentPaidUnits <= tier.maxQuantity;
  });

  const nextTier = activeTierIndex >= 0 && activeTierIndex < tiers.length - 1 ? tiers[activeTierIndex + 1] : null;
  const unitsToNextTier = nextTier ? nextTier.minQuantity - currentPaidUnits : 0;

  return (
    <div className="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <TrendingDown className="w-5 h-5 text-emerald-700" />
          <h4 className="font-bold text-slate-900 text-sm">Volume Discount Ladder</h4>
        </div>
        <span className="text-xs font-bold text-emerald-800 bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-200">
          {currentPaidUnits} {currentPaidUnits === 1 ? 'unit' : 'units'} ordered
        </span>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-2.5">
        {tiers.map((tier, idx) => {
          const isReached = currentPaidUnits >= tier.minQuantity;
          const isCurrent = idx === activeTierIndex;
          const discountPercent = basePrice > 0 ? Math.round(((basePrice - tier.unitPrice) / basePrice) * 100) : 0;

          return (
            <div
              key={idx}
              className={`relative rounded-xl p-3 border transition-all ${
                isCurrent
                  ? 'border-emerald-600 bg-emerald-50/70 ring-2 ring-emerald-500/30'
                  : isReached
                  ? 'border-emerald-200 bg-emerald-50/30'
                  : 'border-slate-200 bg-slate-50 opacity-75'
              }`}
            >
              {isCurrent && (
                <span className="absolute -top-2.5 right-2 bg-emerald-700 text-white text-[10px] font-extrabold px-2 py-0.5 rounded-full shadow-xs">
                  Active Tier
                </span>
              )}
              {isReached && !isCurrent && (
                <span className="absolute -top-2.5 right-2 bg-emerald-600 text-white text-[10px] font-extrabold px-2 py-0.5 rounded-full shadow-xs flex items-center">
                  <CheckCircle2 className="w-2.5 h-2.5 mr-0.5" /> Unlocked
                </span>
              )}

              <div className="text-xs font-semibold text-slate-600">
                {tier.maxQuantity ? `${tier.minQuantity} - ${tier.maxQuantity} Units` : `${tier.minQuantity}+ Units`}
              </div>

              <div className="mt-1 flex items-baseline gap-1">
                <span className="text-lg font-black text-slate-900">
                  {formatCurrency(tier.unitPrice, tier.currency || currency)}
                </span>
              </div>

              {discountPercent > 0 && (
                <div className="mt-1 text-xs font-bold text-emerald-700">
                  Save {discountPercent}%
                </div>
              )}
            </div>
          );
        })}
      </div>

      {nextTier && unitsToNextTier > 0 && (
        <div className="mt-4 bg-emerald-50 border border-emerald-200 rounded-xl p-3 text-xs text-emerald-900 flex items-center justify-between">
          <span>
            Only <strong className="font-extrabold text-emerald-950">{unitsToNextTier} more {unitsToNextTier === 1 ? 'unit' : 'units'}</strong> needed to drop the price to{' '}
            <strong className="font-extrabold text-emerald-950">
              {formatCurrency(nextTier.unitPrice, nextTier.currency || currency)}
            </strong>
            !
          </span>
        </div>
      )}
    </div>
  );
};
