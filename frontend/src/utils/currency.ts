/**
 * Currency formatting utilities for the GroupBuy Indian marketplace.
 *
 * All user-facing monetary amounts should be formatted through these helpers
 * so that ₹ formatting is consistent across every page and component.
 */

/**
 * Format a number as Indian Rupees using en-IN locale.
 * Example: formatINR(18999) → "₹18,999.00"
 */
export const formatINR = (amount: number): string => {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(amount);
};

/**
 * Format a monetary amount respecting its stored currency code.
 * When the backend returns real data with a non-INR currency (e.g. USD from
 * legacy records), the value is displayed using that currency's locale so the
 * user sees an accurate figure rather than a fake exchange-rate conversion.
 * New data created through the admin panel uses INR by default.
 */
export const formatCurrency = (amount: number, currency: string = 'INR'): string => {
  const code = (currency || 'INR').toUpperCase();
  if (code === 'INR') {
    return formatINR(amount);
  }
  // Legacy non-INR value from real DB — display as-is in its own currency
  try {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: code,
      maximumFractionDigits: 2,
    }).format(amount);
  } catch {
    // Fallback if the currency code is unrecognised
    return `${code} ${amount.toFixed(2)}`;
  }
};

