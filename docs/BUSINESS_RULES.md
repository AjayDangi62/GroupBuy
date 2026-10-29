# GroupBuy — Core Business Rules & Domain Invariants

## 1. Group-Buying Business Model

GroupBuy enables sellers to launch time-bound, collective-purchasing campaigns where unit prices decrease as total participant volume increases. Customers commit to purchase units at an **authorized maximum price** based on the current or baseline tier. When the campaign reaches its deadline, all eligible, paid units determine the final settled tier price. All successful participants receive that final discounted price.

---

## 2. The Final Campaign Pricing Rule

> **Core Invariant**:
> The final campaign unit price is determined strictly by eligible, successfully paid/authorized units at the campaign finalization point. All successful orders use that final price. Customers must not be silently charged above their authorization. Payment authorization, capture, adjustments, refunds, and unresolved payment attempts must be addressed before finalization can complete.

### Operational Guarantees:
1. **Never Charge Above Authorization**: If a customer authorized at Tier 1 (e.g., ₹2,000/unit) and the campaign reached Tier 3 (₹1,600/unit), the customer is captured at ₹1,600/unit (or captured at ₹2,000 and refunded ₹400 differential). The customer is **never** charged higher than ₹2,000.
2. **Deterministic Settlement**: Unresolved payment webhooks, pending attempts, and timeouts must either succeed or fail cleanly before finalization begins.
3. **Equal Final Unit Price**: Every participant in a campaign receives the identical final settled unit price, regardless of when during the campaign window they joined.

---

## 3. Pricing Policy Validation Invariants

A pricing policy defines the stepped discount structure for a campaign. The `PricingPolicyValidator` strictly enforces:

| Invariant Rule | Validation Specification | Justification |
| :--- | :--- | :--- |
| **Non-Empty Policy** | Policy must contain $\ge 1$ tiers | A campaign cannot exist without a defined price curve. |
| **Baseline Coverage** | Tier 1 must start at `minQuantity = 1` | Guarantees that any positive unit count (starting from 1) has a valid price. |
| **Positive Minimums** | Every tier `minQuantity >= 1` | Quantities are whole positive physical units. |
| **Non-Negative Prices** | Every tier `unitPrice >= 0` | Negative prices represent invalid domain states. |
| **Ascending Order** | `tier[i+1].minQuantity > tier[i].minQuantity` | Tiers must progress in strictly ascending quantity thresholds. |
| **Contiguous Ranges (No Gaps)** | `tier[i+1].minQuantity == tier[i].maxQuantity + 1` | Prevents quantity values where no tier matches (e.g. 1-4, then 6-9 leaving 5 undefined). |
| **No Overlaps** | `tier[i+1].minQuantity > tier[i].maxQuantity` | Prevents ambiguous pricing where a quantity qualifies for multiple prices. |
| **Non-Reversed Bounds** | `maxQuantity >= minQuantity` (when not null) | A range where min > max is logically invalid. |
| **Terminal Open-End** | Only the final tier has `maxQuantity = null` | Upper volume is uncapped without causing pricing cliff drop-offs. |
| **Currency Consistency** | All tiers must share identical ISO 4217 code | Mixing currencies within a single campaign policy is forbidden. |

---

## 4. Monetary Arithmetic & Rounding Policy

- **Currency**: `INR` (Indian Rupee) as default.
- **Scale**: Exactly 2 decimal places.
- **Rounding Mode**: `RoundingMode.HALF_EVEN` (Banker's Rounding).
  - Statistically eliminates cumulative rounding bias in volume order calculations.
- **Data Types**: `BigDecimal` across all calculations; floating-point types (`float`, `double`) are strictly prohibited in financial code.

---

## 5. Participation & Inventory Lifecycle (Preview for Future Phases)

1. **Reservation Hold**: Joining a campaign creates a short-lived (e.g., 15-minute) inventory reservation.
2. **Two-Phase Payment**: Payment gateway issues an authorization hold. If authorization fails or expires, the reservation hold is released back to available stock.
3. **Campaign Finalization**:
   - If total eligible units $\ge$ campaign minimum target: campaign **SUCCEEDS**. Payments are captured at final tier unit price; orders are snapshotted.
   - If total eligible units $<$ campaign minimum target: campaign **FAILS / CANCELS**. All payment authorizations are voided/refunded; inventory holds are released.
