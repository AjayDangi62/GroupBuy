import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { campaignApi } from '../api/campaignApi';
import { productApi } from '../api/productApi';
import { categoryApi } from '../api/categoryApi';
import { Campaign } from '../types/campaign';
import { Product } from '../types/product';
import { Category } from '../types/category';
import { CampaignCard } from '../components/campaign/CampaignCard';
import { ProductCard } from '../components/product/ProductCard';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { MOCK_CAMPAIGNS, MOCK_PRODUCTS, MOCK_CATEGORIES } from '../mock/mockData';
import {
  Users,
  TrendingDown,
  ShieldCheck,
  Sparkles,
  ArrowRight,
  Flame,
  Layers,
  ShoppingBag,
  Percent,
} from 'lucide-react';

export const HomePage: React.FC = () => {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [campaignsRes, productsRes, categoriesRes] = await Promise.allSettled([
          campaignApi.getActiveCampaigns(0, 6),
          productApi.getProducts({ size: 8 }),
          categoryApi.getActiveCategories(),
        ]);

        // Campaigns fallback
        if (campaignsRes.status === 'fulfilled' && campaignsRes.value?.content?.length > 0) {
          setCampaigns(campaignsRes.value.content);
        } else {
          setCampaigns(MOCK_CAMPAIGNS);
        }

        // Products fallback
        if (productsRes.status === 'fulfilled' && productsRes.value?.content?.length > 0) {
          setProducts(productsRes.value.content);
        } else {
          setProducts(MOCK_PRODUCTS);
        }

        // Categories fallback
        if (categoriesRes.status === 'fulfilled' && categoriesRes.value?.length > 0) {
          setCategories(categoriesRes.value);
        } else {
          setCategories(MOCK_CATEGORIES);
        }
      } catch {
        // Safe fallback to mock data
        setCampaigns(MOCK_CAMPAIGNS);
        setProducts(MOCK_PRODUCTS);
        setCategories(MOCK_CATEGORIES);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  return (
    <div className="space-y-16 sm:space-y-24 pb-24 bg-white">
      {/* Hero Section */}
      <section className="relative overflow-hidden bg-gradient-to-b from-emerald-50/70 via-white to-white pt-12 sm:pt-20 pb-16 sm:pb-24 border-b border-slate-100">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
            {/* Left Hero Content */}
            <div className="lg:col-span-7 space-y-6 sm:space-y-8">
              <div className="inline-flex items-center gap-2 bg-emerald-100/80 border border-emerald-200/80 rounded-full px-4 py-1.5 text-xs font-bold text-emerald-800">
                <Sparkles className="w-4 h-4 text-emerald-700" />
                <span>India's Smart Group-Buying Marketplace</span>
              </div>

              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold text-slate-900 tracking-tight leading-[1.15]">
                Buy Together.{' '}
                <span className="text-emerald-700 underline decoration-emerald-300 underline-offset-8">
                  Pay Less
                </span>
                .
              </h1>

              <p className="text-base sm:text-lg text-slate-600 leading-relaxed max-w-2xl">
                GroupBuy brings shoppers across India together to unlock direct volume discounts. Join active buying groups, watch unit prices drop in real-time, and get automatic refunds if a cheaper tier is unlocked before the deal closes.
              </p>

              {/* Visual 3-step banner */}
              <div className="p-3.5 bg-emerald-50/80 border border-emerald-200 rounded-2xl flex flex-wrap items-center justify-between gap-2 text-xs font-semibold text-emerald-950">
                <span className="flex items-center gap-1.5">
                  <span className="w-5 h-5 rounded-full bg-emerald-700 text-white flex items-center justify-center text-[11px] font-bold">1</span>
                  Choose a product
                </span>
                <span className="text-emerald-400 font-bold hidden sm:inline">→</span>
                <span className="flex items-center gap-1.5">
                  <span className="w-5 h-5 rounded-full bg-emerald-700 text-white flex items-center justify-center text-[11px] font-bold">2</span>
                  Join a buying group
                </span>
                <span className="text-emerald-400 font-bold hidden sm:inline">→</span>
                <span className="flex items-center gap-1.5">
                  <span className="w-5 h-5 rounded-full bg-emerald-700 text-white flex items-center justify-center text-[11px] font-bold">3</span>
                  Unlock a lower price
                </span>
              </div>

              {/* Action Buttons */}
              <div className="pt-2 flex flex-wrap gap-4 items-center">
                <Link
                  to="/campaigns"
                  className="bg-emerald-700 hover:bg-emerald-800 text-white font-extrabold px-7 py-4 rounded-xl shadow-md shadow-emerald-700/20 flex items-center gap-2.5 transition-all transform hover:-translate-y-0.5 text-base"
                >
                  <span>Explore Deals</span>
                  <ArrowRight className="w-5 h-5" />
                </Link>

                <Link
                  to="/products"
                  className="bg-white hover:bg-slate-50 text-slate-700 font-bold px-7 py-4 rounded-xl border border-slate-300 hover:border-emerald-600 transition-colors text-base"
                >
                  Browse Catalog
                </Link>
              </div>

              {/* Highlights & Guarantees */}
              <div className="pt-6 border-t border-slate-200 grid grid-cols-3 gap-4 sm:gap-8">
                <div>
                  <div className="text-2xl sm:text-3xl font-extrabold text-emerald-800">Up to 45%</div>
                  <div className="text-xs text-slate-500 font-medium mt-0.5">Tier Volume Savings</div>
                </div>
                <div>
                  <div className="text-2xl sm:text-3xl font-extrabold text-emerald-800">100%</div>
                  <div className="text-xs text-slate-500 font-medium mt-0.5">Price Protection Guarantee</div>
                </div>
                <div>
                  <div className="text-2xl sm:text-3xl font-extrabold text-emerald-800">Automated</div>
                  <div className="text-xs text-slate-500 font-medium mt-0.5">Differential Refunds</div>
                </div>
              </div>
            </div>

            {/* Right Hero Interactive Visual Card */}
            <div className="lg:col-span-5">
              <div className="relative mx-auto max-w-md bg-white border border-slate-200/90 rounded-3xl p-6 shadow-xl shadow-slate-200/50">
                <div className="flex items-center justify-between pb-4 border-b border-slate-100">
                  <div className="flex items-center gap-2">
                    <span className="w-3 h-3 rounded-full bg-emerald-500 animate-pulse" />
                    <span className="text-xs font-bold text-slate-700 uppercase tracking-wider">Live Volume Tier Demo</span>
                  </div>
                  <span className="text-xs font-bold bg-emerald-50 text-emerald-800 px-2.5 py-1 rounded-full border border-emerald-200">
                    Tier 3 Reached
                  </span>
                </div>

                <div className="mt-5 space-y-3.5">
                  <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 text-xs">
                    <span className="text-slate-500 font-medium">1 - 19 units</span>
                    <span className="font-semibold text-slate-600 line-through">₹18,999 (Retail)</span>
                  </div>
                  <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 text-xs">
                    <span className="text-slate-500 font-medium">20 - 49 units</span>
                    <span className="font-semibold text-slate-700">₹17,499 (Save ₹1,500)</span>
                  </div>
                  <div className="flex items-center justify-between p-3.5 rounded-xl bg-emerald-50 border border-emerald-300 text-xs font-bold">
                    <span className="text-emerald-900 flex items-center gap-1.5">
                      <Sparkles className="w-3.5 h-3.5 text-emerald-600" />
                      50 - 99 units (Current Tier)
                    </span>
                    <span className="text-emerald-800 text-sm">₹16,499 (Save ₹2,500)</span>
                  </div>
                  <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 text-xs">
                    <span className="text-slate-500 font-medium">100+ units</span>
                    <span className="font-semibold text-emerald-700">₹14,999 (Save ₹4,000 Target)</span>
                  </div>
                </div>

                <div className="mt-6 pt-5 border-t border-slate-100 flex items-center justify-between text-xs text-slate-600">
                  <span>38 buyers currently committed</span>
                  <Link to="/campaigns" className="text-emerald-700 font-bold hover:underline">
                    View active deals →
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Popular Categories */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
              Browse by Category
            </h2>
            <p className="text-sm text-slate-500 mt-1">
              Curated categories with regular group drops and wholesale tiers.
            </p>
          </div>
          <Link
            to="/products"
            className="hidden sm:flex items-center gap-1 text-sm font-bold text-emerald-700 hover:text-emerald-800 transition"
          >
            <span>All Categories</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 sm:gap-6">
          {categories.map((cat) => (
            <Link
              key={cat.id}
              to={`/products?category=${cat.id}`}
              className="p-5 rounded-2xl border border-slate-200 bg-white hover:border-emerald-500 hover:shadow-md transition-all group block text-center"
            >
              <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-700 mx-auto flex items-center justify-center group-hover:scale-110 group-hover:bg-emerald-700 group-hover:text-white transition-all mb-3">
                <Layers className="w-6 h-6" />
              </div>
              <h3 className="font-bold text-slate-800 text-sm group-hover:text-emerald-700 transition-colors">
                {cat.name}
              </h3>
              <p className="text-xs text-slate-400 mt-1 line-clamp-2">
                {cat.description || 'Quality group-buy items'}
              </p>
            </Link>
          ))}
        </div>
      </section>

      {/* Trending Group Deals */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-8">
          <div>
            <div className="inline-flex items-center gap-1.5 text-xs font-bold text-emerald-700 uppercase tracking-wider mb-2">
              <Flame className="w-4 h-4 text-emerald-600" />
              <span>Trending Drops</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
              Active Group-Buy Deals
            </h2>
            <p className="text-sm text-slate-500 mt-1">
              Join before campaign deadlines expire to secure unlocked discounts.
            </p>
          </div>

          <Link
            to="/campaigns"
            className="inline-flex items-center gap-1.5 text-sm font-bold text-emerald-700 hover:text-emerald-800"
          >
            <span>View All Group Deals</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>

        {loading ? (
          <div className="py-12 flex justify-center">
            <LoadingSpinner size="lg" text="Loading active group deals..." />
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {campaigns.slice(0, 3).map((campaign) => (
              <CampaignCard key={campaign.id} campaign={campaign} />
            ))}
          </div>
        )}
      </section>

      {/* How GroupBuy Works */}
      <section className="bg-slate-50 py-16 border-y border-slate-200/70">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-12">
            <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900">
              How Smart Group Buying Works
            </h2>
            <p className="text-sm text-slate-600 mt-2">
              Three simple steps to unlock manufacturer wholesale pricing with zero risk.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="bg-white p-6 rounded-2xl border border-slate-200 text-center relative">
              <div className="w-12 h-12 rounded-xl bg-emerald-100 text-emerald-800 font-black text-lg mx-auto flex items-center justify-center mb-4">
                1
              </div>
              <h3 className="font-bold text-slate-900 text-base mb-2">Join a Live Campaign</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                Choose the desired quantity and authorize the current maximum price ceiling. You are never charged more than this amount.
              </p>
            </div>

            <div className="bg-white p-6 rounded-2xl border border-slate-200 text-center relative">
              <div className="w-12 h-12 rounded-xl bg-emerald-100 text-emerald-800 font-black text-lg mx-auto flex items-center justify-center mb-4">
                2
              </div>
              <h3 className="font-bold text-slate-900 text-base mb-2">Demand Unlocks Tiers</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                As other customers join the campaign, total committed quantity unlocks progressively lower unit pricing tiers.
              </p>
            </div>

            <div className="bg-white p-6 rounded-2xl border border-slate-200 text-center relative">
              <div className="w-12 h-12 rounded-xl bg-emerald-100 text-emerald-800 font-black text-lg mx-auto flex items-center justify-center mb-4">
                3
              </div>
              <h3 className="font-bold text-slate-900 text-base mb-2">Finalize & Automatic Refund</h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                When the campaign concludes, everyone settles at the final lowest tier achieved, and any overpayment is automatically refunded.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Featured Products Catalog */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-8">
          <div>
            <div className="inline-flex items-center gap-1.5 text-xs font-bold text-emerald-700 uppercase tracking-wider mb-2">
              <ShoppingBag className="w-4 h-4 text-emerald-600" />
              <span>Direct Catalog</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
              Featured Products
            </h2>
            <p className="text-sm text-slate-500 mt-1">
              Purchase individually at standard retail, or check if an active group campaign is running.
            </p>
          </div>

          <Link
            to="/products"
            className="inline-flex items-center gap-1.5 text-sm font-bold text-emerald-700 hover:text-emerald-800"
          >
            <span>Explore All Products</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>

        {loading ? (
          <div className="py-12 flex justify-center">
            <LoadingSpinner size="lg" text="Loading featured products..." />
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {products.slice(0, 8).map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
};
