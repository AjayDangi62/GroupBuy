import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Product } from '../../types/product';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { ShoppingCart, Check, Package } from 'lucide-react';
import { formatCurrency } from '../../utils/currency';

interface ProductCardProps {
  product: Product;
}

export const ProductCard: React.FC<ProductCardProps> = ({ product }) => {
  const { addToCart, isLoading } = useCart();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [added, setAdded] = useState(false);

  const handleAddToCart = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!isAuthenticated) {
      navigate(`/login?redirect=/products/${product.id}`);
      return;
    }

    try {
      await addToCart(product.id, 1);
      setAdded(true);
      setTimeout(() => setAdded(false), 2000);
    } catch {
      // Handled by alert/context
    }
  };

  const isOutOfStock = product.availableInventory <= 0;

  return (
    <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-xs hover:shadow-lg hover:border-emerald-200 transition-all duration-300 flex flex-col group">
      <Link to={`/products/${product.id}`} className="relative aspect-square bg-slate-50 overflow-hidden block">
        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-slate-300">
            <Package className="w-12 h-12" />
          </div>
        )}

        {isOutOfStock && (
          <div className="absolute inset-0 bg-white/70 backdrop-blur-2xs flex items-center justify-center">
            <span className="bg-rose-600 text-white font-bold text-xs uppercase px-3 py-1 rounded-full shadow-sm">
              Out of Stock
            </span>
          </div>
        )}
      </Link>

      <div className="p-4 flex-1 flex flex-col justify-between">
        <div>
          {product.categoryName && (
            <span className="text-[11px] font-bold text-emerald-700 uppercase tracking-wider block mb-1">
              {product.categoryName}
            </span>
          )}

          <Link to={`/products/${product.id}`}>
            <h3 className="font-semibold text-slate-900 text-sm line-clamp-2 group-hover:text-emerald-700 transition-colors">
              {product.name}
            </h3>
          </Link>

          <p className="text-xs text-slate-400 mt-0.5 font-mono">SKU: {product.sku}</p>
        </div>

        <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between">
          <div>
            <span className="text-base sm:text-lg font-extrabold text-slate-900">
              {formatCurrency(product.basePrice, product.currency)}
            </span>
            <div className="text-[11px] text-slate-500">
              {product.availableInventory > 0 ? `${product.availableInventory} in stock` : 'Unavailable'}
            </div>
          </div>

          <button
            onClick={handleAddToCart}
            disabled={isOutOfStock || isLoading}
            className={`p-2.5 rounded-xl font-medium text-sm transition-all flex items-center justify-center cursor-pointer ${
              added
                ? 'bg-emerald-600 text-white'
                : isOutOfStock
                ? 'bg-slate-100 text-slate-400 cursor-not-allowed'
                : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-600 hover:text-white'
            }`}
            title={added ? 'Added to Cart' : 'Add to Cart'}
          >
            {added ? <Check className="w-4 h-4" /> : <ShoppingCart className="w-4 h-4" />}
          </button>
        </div>
      </div>
    </div>
  );
};
