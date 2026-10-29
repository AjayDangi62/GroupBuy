import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { productApi } from '../api/productApi';
import { campaignApi } from '../api/campaignApi';
import { Product } from '../types/product';
import { Campaign } from '../types/campaign';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorAlert } from '../components/common/ErrorAlert';
import { MOCK_PRODUCTS, MOCK_CAMPAIGNS } from '../mock/mockData';
import { formatCurrency } from '../utils/currency';
import {
  ShoppingCart,
  Check,
  Package,
  ShieldCheck,
  Truck,
  RotateCcw,
  Sparkles,
  ArrowRight,
  Minus,
  Plus,
} from 'lucide-react';

export const ProductDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { addToCart } = useCart();
  const { isAuthenticated } = useAuth();

  const [product, setProduct] = useState<Product | null>(null);
  const [activeCampaign, setActiveCampaign] = useState<Campaign | null>(null);
  const [quantity, setQuantity] = useState<number>(1);
  const [loading, setLoading] = useState<boolean>(true);
  const [addingToCart, setAddingToCart] = useState<boolean>(false);
  const [addedSuccess, setAddedSuccess] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchProduct = async () => {
      if (!id) return;
      setLoading(true);
      setError(null);
      try {
        const prod = await productApi.getProductById(Number(id));
        setProduct(prod);

        // Check if there is an active group campaign for this product
        try {
          const campaignsRes = await campaignApi.getActiveCampaigns(0, 50);
          const matched = campaignsRes.content?.find((c) => c.productId === prod.id && c.status === 'ACTIVE');
          if (matched) {
            setActiveCampaign(matched);
          } else {
            const mockMatched = MOCK_CAMPAIGNS.find((c) => c.productId === prod.id);
            if (mockMatched) setActiveCampaign(mockMatched);
          }
        } catch {
          const mockMatched = MOCK_CAMPAIGNS.find((c) => c.productId === prod.id);
          if (mockMatched) setActiveCampaign(mockMatched);
        }
      } catch {
        // Fallback to mock product
        const mockProd = MOCK_PRODUCTS.find((p) => p.id === Number(id));
        if (mockProd) {
          setProduct(mockProd);
          const mockCamp = MOCK_CAMPAIGNS.find((c) => c.productId === mockProd.id);
          if (mockCamp) setActiveCampaign(mockCamp);
        } else {
          setError('Product not found or failed to load.');
        }
      } finally {
        setLoading(false);
      }
    };

    fetchProduct();
  }, [id]);

  const handleAddToCart = async () => {
    if (!product) return;
    if (!isAuthenticated) {
      navigate(`/login?redirect=/products/${product.id}`);
      return;
    }

    setAddingToCart(true);
    try {
      await addToCart(product.id, quantity);
      setAddedSuccess(true);
      setTimeout(() => setAddedSuccess(false), 2500);
    } catch {
      setError('Failed to add item to cart. Please check stock availability.');
    } finally {
      setAddingToCart(false);
    }
  };

  if (loading) {
    return <LoadingSpinner fullScreen message="Loading product details..." />;
  }

  if (error || !product) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-16 text-center">
        <ErrorAlert message={error || 'Product not found'} />
        <Link
          to="/products"
          className="inline-block mt-4 text-sm font-semibold text-emerald-700 hover:text-emerald-800 underline"
        >
          Return to Product Catalog
        </Link>
      </div>
    );
  }

  const isOutOfStock = product.availableInventory <= 0;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-10 bg-white">
      {/* Active Campaign Alert Banner if available */}
      {activeCampaign && (
        <div className="bg-emerald-900 rounded-2xl p-4 sm:p-5 text-white shadow-md flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3 text-center sm:text-left">
            <div className="w-10 h-10 rounded-xl bg-emerald-500 text-white font-bold flex items-center justify-center shrink-0">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <div className="text-xs font-bold text-emerald-300 uppercase tracking-wider">
                Live Group Deal Available!
              </div>
              <h3 className="font-bold text-base sm:text-lg">
                Save more on this item in the "{activeCampaign.title}" campaign
              </h3>
            </div>
          </div>
          <Link
            to={`/campaigns/${activeCampaign.id}`}
            className="inline-flex items-center gap-2 bg-white text-emerald-900 font-bold px-4 py-2 rounded-xl text-sm hover:bg-emerald-50 transition shrink-0 shadow-sm"
          >
            <span>View Group Deal</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      )}

      {/* Main Product Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
        {/* Left: Product Image */}
        <div className="lg:col-span-6">
          <div className="aspect-square bg-slate-50 border border-slate-200 rounded-3xl overflow-hidden flex items-center justify-center relative shadow-xs">
            {product.imageUrl ? (
              <img
                src={product.imageUrl}
                alt={product.name}
                className="w-full h-full object-cover"
              />
            ) : (
              <Package className="w-20 h-20 text-slate-300" />
            )}
            {isOutOfStock && (
              <div className="absolute inset-0 bg-white/80 backdrop-blur-2xs flex items-center justify-center">
                <span className="bg-rose-600 text-white font-bold text-sm uppercase px-4 py-1.5 rounded-full shadow-sm">
                  Out of Stock
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Right: Product Details & Purchase Actions */}
        <div className="lg:col-span-6 space-y-6">
          <div>
            {product.categoryName && (
              <span className="text-xs font-bold text-emerald-700 uppercase tracking-wider block mb-2">
                {product.categoryName}
              </span>
            )}
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 leading-tight">
              {product.name}
            </h1>
            <div className="flex items-center gap-3 mt-2 text-xs text-slate-500 font-mono">
              <span>SKU: {product.sku}</span>
              <span>•</span>
              <span className={product.availableInventory > 0 ? 'text-emerald-700 font-semibold' : 'text-rose-600 font-semibold'}>
                {product.availableInventory > 0 ? `${product.availableInventory} Units Available` : 'Out of Stock'}
              </span>
            </div>
          </div>

          {/* Pricing Box */}
          <div className="p-5 bg-slate-50 border border-slate-200 rounded-2xl space-y-2">
            <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Direct Retail Price</div>
            <div className="flex items-baseline gap-3">
              <span className="text-3xl sm:text-4xl font-extrabold text-slate-900">
                {formatCurrency(product.basePrice, product.currency)}
              </span>
              <span className="text-xs text-slate-500">Includes all standard platform warranties</span>
            </div>
          </div>

          {/* Description */}
          <div>
            <h2 className="text-sm font-bold text-slate-900 uppercase tracking-wider mb-2">Description & Specs</h2>
            <p className="text-sm text-slate-600 leading-relaxed whitespace-pre-line">
              {product.description || 'No detailed specifications provided for this product.'}
            </p>
          </div>

          {/* Quantity Selector & Add to Cart */}
          <div className="pt-4 border-t border-slate-100 space-y-4">
            <div className="flex items-center gap-4">
              <span className="text-xs font-bold text-slate-700 uppercase tracking-wider">Quantity:</span>
              <div className="flex items-center border border-slate-300 rounded-xl bg-white shadow-xs">
                <button
                  type="button"
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                  disabled={quantity <= 1 || isOutOfStock}
                  className="p-2 text-slate-600 hover:text-emerald-700 disabled:opacity-30 transition"
                >
                  <Minus className="w-4 h-4" />
                </button>
                <span className="w-10 text-center font-bold text-sm text-slate-900">{quantity}</span>
                <button
                  type="button"
                  onClick={() => setQuantity(Math.min(product.availableInventory, quantity + 1))}
                  disabled={quantity >= product.availableInventory || isOutOfStock}
                  className="p-2 text-slate-600 hover:text-emerald-700 disabled:opacity-30 transition"
                >
                  <Plus className="w-4 h-4" />
                </button>
              </div>
            </div>

            <div className="flex flex-col sm:flex-row gap-3 pt-2">
              <button
                type="button"
                onClick={handleAddToCart}
                disabled={isOutOfStock || addingToCart}
                className={`flex-1 py-3.5 px-6 rounded-xl font-bold text-sm flex items-center justify-center gap-2 shadow-sm transition-all cursor-pointer ${
                  addedSuccess
                    ? 'bg-emerald-600 text-white'
                    : isOutOfStock
                    ? 'bg-slate-200 text-slate-400 cursor-not-allowed'
                    : 'bg-emerald-700 hover:bg-emerald-800 text-white'
                }`}
              >
                {addedSuccess ? (
                  <>
                    <Check className="w-4 h-4" />
                    <span>Added to Cart!</span>
                  </>
                ) : (
                  <>
                    <ShoppingCart className="w-4 h-4" />
                    <span>{addingToCart ? 'Adding...' : 'Add to Cart (Retail)'}</span>
                  </>
                )}
              </button>

              {activeCampaign && (
                <Link
                  to={`/campaigns/${activeCampaign.id}`}
                  className="flex-1 py-3.5 px-6 rounded-xl font-bold text-sm bg-emerald-50 text-emerald-800 hover:bg-emerald-100 border border-emerald-300 flex items-center justify-center gap-2 transition"
                >
                  <Sparkles className="w-4 h-4 text-emerald-700" />
                  <span>Join Group Buy Instead</span>
                </Link>
              )}
            </div>
          </div>

          {/* Guarantees */}
          <div className="grid grid-cols-3 gap-3 pt-4 border-t border-slate-100 text-center text-xs text-slate-600">
            <div className="flex flex-col items-center gap-1.5 p-3 rounded-xl bg-slate-50">
              <Truck className="w-4 h-4 text-emerald-700" />
              <span className="font-medium">Direct Shipping</span>
            </div>
            <div className="flex flex-col items-center gap-1.5 p-3 rounded-xl bg-slate-50">
              <ShieldCheck className="w-4 h-4 text-emerald-700" />
              <span className="font-medium">Secure Payment</span>
            </div>
            <div className="flex flex-col items-center gap-1.5 p-3 rounded-xl bg-slate-50">
              <RotateCcw className="w-4 h-4 text-emerald-700" />
              <span className="font-medium">Verified Sellers</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
