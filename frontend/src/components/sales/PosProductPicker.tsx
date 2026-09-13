import {
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type KeyboardEvent,
  type PointerEvent as ReactPointerEvent,
} from 'react';
import { VirtualList } from '../common/VirtualList';
import {
  fetchProductCategories,
  fetchProducts,
  lookupProductByBarcode,
} from '../../services/productsService';
import {
  fetchPosQuickAccess,
  posSearchProducts,
  recordPosProductSelection,
  recordPosSearchEvent,
  togglePosProductFavorite,
  type PosProductHit,
  type StockState,
} from '../../services/productsPosService';
import type { Product, ProductCategory } from '../../types/api';

const SEARCH_DEBOUNCE_MS = 300;
const PAGE_SIZE = 40;
const ROW_HEIGHT = 64;
const LIST_HEIGHT_MOBILE = 220;
const LIST_HEIGHT_DESKTOP = 280;
const SEARCH_CACHE_LIMIT = 40;
const QUICK_PICK_LIMIT = 8;
const CATEGORY_CHIP_LIMIT = 12;

interface PosProductPickerProps {
  locationId?: number;
  shopId?: number;
  currencyCode: string;
  disabled?: boolean;
  autoFocus?: boolean;
  inputId?: string;
  /** Returns true when the line was added. Optional knownStock skips a slow stock round-trip. */
  onAdd: (
    product: Product,
    quantity: number,
    knownStock?: number | null,
  ) => Promise<boolean> | boolean;
}

type QuickTab = 'recent' | 'frequent' | 'favorites';

interface CacheEntry {
  at: number;
  items: PosProductHit[];
  totalElements: number;
}

function formatMoney(value: number, currencyCode: string): string {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: currencyCode,
    maximumFractionDigits: 2,
  }).format(value);
}

function formatQty(value: number): string {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(value);
}

function formatUnit(unit: string): string {
  switch (unit.toUpperCase()) {
    case 'PIECE':
      return 'pcs';
    case 'METRE':
      return 'm';
    default:
      return unit.toLowerCase();
  }
}

function stockLabel(state: StockState): string {
  switch (state) {
    case 'IN':
      return 'In Stock';
    case 'LOW':
      return 'Low Stock';
    case 'OUT':
      return 'Out of Stock';
    default:
      return 'Stock n/a';
  }
}

function stockClass(state: StockState): string {
  switch (state) {
    case 'IN':
      return 'pos-picker__stock--in';
    case 'LOW':
      return 'pos-picker__stock--low';
    case 'OUT':
      return 'pos-picker__stock--out';
    default:
      return 'pos-picker__stock--unknown';
  }
}

function looksLikeBarcode(value: string): boolean {
  const trimmed = value.trim();
  return trimmed.length >= 8 && /^[0-9A-Za-z-]+$/.test(trimmed) && /\d/.test(trimmed);
}

function hitToProduct(hit: PosProductHit): Product {
  return {
    id: hit.id,
    sku: hit.sku,
    name: hit.name,
    description: null,
    brand: hit.brand,
    categoryId: hit.categoryId,
    categoryName: hit.categoryName,
    unitOfMeasure: hit.unitOfMeasure,
    costPrice: 0,
    sellingPrice: hit.sellingPrice,
    currencyCode: hit.currencyCode,
    taxInclusive: true,
    trackInventory: true,
    reorderLevel: hit.reorderLevel,
    status: 'ACTIVE',
    createdAt: '',
    updatedAt: '',
  };
}

/** Map catalog products (live /api/products) into POS hit shape for seamless fallback. */
function productToHit(product: Product): PosProductHit {
  return {
    id: product.id,
    sku: product.sku,
    name: product.name,
    brand: product.brand,
    categoryId: product.categoryId,
    categoryName: product.categoryName,
    unitOfMeasure: product.unitOfMeasure,
    sellingPrice: product.sellingPrice,
    currencyCode: product.currencyCode,
    reorderLevel: product.reorderLevel,
    stockAvailable: null,
    stockState: 'UNKNOWN',
    favorite: false,
    saleCount90d: 0,
  };
}

async function searchWithLiveFallback(params: {
  q?: string;
  categoryId?: number;
  locationId?: number;
  page: number;
  size: number;
}): Promise<{ items: PosProductHit[]; totalElements: number; usedFallback: boolean }> {
  try {
    const response = await posSearchProducts(params);
    return {
      items: response.items,
      totalElements: response.totalElements,
      usedFallback: false,
    };
  } catch {
    // Keep checkout working if POS endpoints are unavailable — same catalog API as live today.
    const response = await fetchProducts({
      search: params.q,
      categoryId: params.categoryId,
      status: 'ACTIVE',
      page: params.page,
      size: params.size,
    });
    return {
      items: response.items.map(productToHit),
      totalElements: response.totalElements,
      usedFallback: true,
    };
  }
}

export function PosProductPicker({
  locationId,
  shopId,
  currencyCode,
  disabled = false,
  autoFocus = true,
  inputId,
  onAdd,
}: PosProductPickerProps) {
  const generatedId = useId();
  const searchId = inputId ?? generatedId;
  const qtyId = useId();
  const searchRef = useRef<HTMLInputElement>(null);
  const qtyRef = useRef<HTMLInputElement>(null);
  const requestRef = useRef(0);
  const searchStartedAt = useRef<number>(0);
  const cacheRef = useRef<Map<string, CacheEntry>>(new Map());

  const [query, setQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [results, setResults] = useState<PosProductHit[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [highlight, setHighlight] = useState(0);
  const [selected, setSelected] = useState<PosProductHit | null>(null);
  const [quantity, setQuantity] = useState('1');
  const [quickTab, setQuickTab] = useState<QuickTab>('recent');
  const [recent, setRecent] = useState<PosProductHit[]>([]);
  const [frequent, setFrequent] = useState<PosProductHit[]>([]);
  const [favorites, setFavorites] = useState<PosProductHit[]>([]);
  const [barcodeBusy, setBarcodeBusy] = useState(false);
  const [isNarrow, setIsNarrow] = useState(
    typeof window !== 'undefined' ? window.matchMedia('(max-width: 720px)').matches : false,
  );
  /** Only auto-scroll the list to highlight during keyboard navigation — not touch scroll. */
  const [keyboardScrollIndex, setKeyboardScrollIndex] = useState<number | null>(null);
  const pointerGestureRef = useRef<{ x: number; y: number; moved: boolean } | null>(null);
  const suppressTapUntilRef = useRef(0);

  const listHeight = isNarrow ? LIST_HEIGHT_MOBILE : LIST_HEIGHT_DESKTOP;
  const showSearchResults = debouncedQuery.trim().length > 0 || categoryId != null;
  const quickItems = quickTab === 'recent' ? recent : quickTab === 'frequent' ? frequent : favorites;
  const activeList = showSearchResults ? results : quickItems;
  const visibleCategories = categories.slice(0, CATEGORY_CHIP_LIMIT);
  const quickPicks = useMemo(() => {
    const seen = new Set<number>();
    const picks: PosProductHit[] = [];
    for (const hit of [...frequent, ...recent, ...favorites]) {
      if (seen.has(hit.id) || hit.stockState === 'OUT') {
        continue;
      }
      seen.add(hit.id);
      picks.push(hit);
      if (picks.length >= QUICK_PICK_LIMIT) {
        break;
      }
    }
    return picks;
  }, [frequent, recent, favorites]);

  useEffect(() => {
    const media = window.matchMedia('(max-width: 720px)');
    const onChange = () => setIsNarrow(media.matches);
    onChange();
    media.addEventListener('change', onChange);
    return () => media.removeEventListener('change', onChange);
  }, []);

  useEffect(() => {
    if (!autoFocus || disabled) {
      return;
    }
    const timer = window.setTimeout(() => searchRef.current?.focus(), 50);
    return () => window.clearTimeout(timer);
  }, [autoFocus, disabled, shopId]);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedQuery(query), SEARCH_DEBOUNCE_MS);
    return () => window.clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    fetchProductCategories(true)
      .then(setCategories)
      .catch(() => setCategories([]));
  }, []);

  const reloadQuickAccess = useCallback(() => {
    fetchPosQuickAccess(locationId)
      .then((data) => {
        setRecent(data.recent);
        setFrequent(data.frequent);
        setFavorites(data.favorites);
      })
      .catch(() => {
        setRecent([]);
        setFrequent([]);
        setFavorites([]);
      });
  }, [locationId]);

  useEffect(() => {
    reloadQuickAccess();
  }, [reloadQuickAccess]);

  const filterKey = useMemo(
    () => `${debouncedQuery.trim().toLowerCase()}|${categoryId ?? ''}|${locationId ?? ''}`,
    [debouncedQuery, categoryId, locationId],
  );
  const filterKeyRef = useRef(filterKey);
  const cacheKey = `${filterKey}|${page}`;

  useEffect(() => {
    if (!showSearchResults) {
      setResults([]);
      setTotalElements(0);
      setLoading(false);
      setError(null);
      setHighlight(0);
      return;
    }

    const filterChanged = filterKeyRef.current !== filterKey;
    if (filterChanged) {
      filterKeyRef.current = filterKey;
      setHighlight(0);
      if (page !== 0) {
        setPage(0);
        return;
      }
    }

    const cached = cacheRef.current.get(cacheKey);
    if (cached && Date.now() - cached.at < 60_000) {
      setResults(cached.items);
      setTotalElements(cached.totalElements);
      if (page === 0) {
        setHighlight(0);
      }
      setLoading(false);
      return;
    }

    const requestId = ++requestRef.current;
    const started = performance.now();
    searchStartedAt.current = started;
    setLoading(page === 0);
    setLoadingMore(page > 0);
    setError(null);

    searchWithLiveFallback({
      q: debouncedQuery.trim() || undefined,
      categoryId: categoryId ?? undefined,
      locationId,
      page,
      size: PAGE_SIZE,
    })
      .then((response) => {
        if (requestRef.current !== requestId) {
          return;
        }
        setResults((current) => {
          const nextItems =
            page === 0
              ? response.items
              : [
                  ...current,
                  ...response.items.filter((item) => !current.some((existing) => existing.id === item.id)),
                ];
          cacheRef.current.set(cacheKey, {
            at: Date.now(),
            items: nextItems,
            totalElements: response.totalElements,
          });
          if (cacheRef.current.size > SEARCH_CACHE_LIMIT) {
            const oldest = cacheRef.current.keys().next().value;
            if (oldest) {
              cacheRef.current.delete(oldest);
            }
          }
          return nextItems;
        });
        setTotalElements(response.totalElements);
        if (page === 0) {
          setHighlight(0);
        }

        const latencyMs = Math.round(performance.now() - started);
        if (!response.usedFallback) {
          void recordPosSearchEvent({
            queryText: debouncedQuery.trim() || (categoryId ? `category:${categoryId}` : ''),
            resultCount: response.totalElements,
            latencyMs,
            shopId,
            failed: response.totalElements === 0,
          });
        }
      })
      .catch((err: Error) => {
        if (requestRef.current !== requestId) {
          return;
        }
        setError(err.message || 'Search failed');
        setResults([]);
      })
      .finally(() => {
        if (requestRef.current === requestId) {
          setLoading(false);
          setLoadingMore(false);
        }
      });
  }, [cacheKey, showSearchResults, filterKey, debouncedQuery, categoryId, locationId, page, shopId]);

  useEffect(() => {
    setHighlight((current) => {
      if (activeList.length === 0) {
        return 0;
      }
      return Math.min(current, activeList.length - 1);
    });
  }, [activeList.length]);

  async function commitAdd(hit: PosProductHit, qtyValue: string) {
    const qty = Number(qtyValue);
    if (!Number.isFinite(qty) || qty <= 0) {
      setError('Enter a quantity greater than zero.');
      return;
    }
    if (hit.stockState === 'OUT') {
      setError(`${hit.sku} is out of stock at this shop.`);
      return;
    }

    const selectionStarted = performance.now();
    const knownStock = hit.stockAvailable != null ? Number(hit.stockAvailable) : null;
    const added = await onAdd(hitToProduct(hit), qty, knownStock);
    if (!added) {
      setError('Could not add that product — check stock or shop selection.');
      return;
    }

    void recordPosProductSelection(hit.id);
    void recordPosSearchEvent({
      queryText: debouncedQuery.trim() || hit.name,
      resultCount: totalElements || 1,
      selectedProductId: hit.id,
      selectionLatencyMs: Math.round(performance.now() - selectionStarted),
      shopId,
    });

    setSelected(null);
    setQuantity('1');
    setQuery('');
    setDebouncedQuery('');
    setCategoryId(null);
    setHighlight(0);
    setError(null);
    reloadQuickAccess();
    window.setTimeout(() => searchRef.current?.focus(), 20);
  }

  async function handleOneTapAdd(hit: PosProductHit) {
    setError(null);
    await commitAdd(hit, '1');
  }

  function chooseForQuantity(hit: PosProductHit) {
    setSelected(hit);
    setQuantity('1');
    setError(null);
    window.setTimeout(() => {
      qtyRef.current?.focus();
      qtyRef.current?.select();
    }, 20);
  }

  async function handleBarcodeScan(raw: string) {
    const code = raw.trim();
    if (!code || barcodeBusy) {
      return;
    }
    setBarcodeBusy(true);
    setError(null);
    try {
      const product = await lookupProductByBarcode(code);
      const added = await onAdd(product, 1);
      if (!added) {
        setError('Could not add scanned product — check stock or shop selection.');
        return;
      }
      void recordPosProductSelection(product.id);
      setQuery('');
      setDebouncedQuery('');
      setCategoryId(null);
      setHighlight(0);
      reloadQuickAccess();
      window.setTimeout(() => searchRef.current?.focus(), 20);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Barcode not found');
    } finally {
      setBarcodeBusy(false);
    }
  }

  async function handleFavoriteToggle(hit: PosProductHit) {
    try {
      const result = await togglePosProductFavorite(hit.id);
      const patch = (items: PosProductHit[]) =>
        items.map((item) => (item.id === hit.id ? { ...item, favorite: result.favorite } : item));
      setResults((current) => patch(current));
      setRecent((current) => patch(current));
      setFrequent((current) => patch(current));
      if (result.favorite) {
        setFavorites((current) => {
          if (current.some((item) => item.id === hit.id)) {
            return patch(current);
          }
          return [{ ...hit, favorite: true }, ...current];
        });
      } else {
        setFavorites((current) => current.filter((item) => item.id !== hit.id));
      }
    } catch {
      // Keep UI responsive if favorite toggle fails.
    }
  }

  function handleSearchKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      if (activeList.length === 0) {
        return;
      }
      setHighlight((current) => {
        const next = (current + 1) % activeList.length;
        setKeyboardScrollIndex(next);
        return next;
      });
      return;
    }
    if (event.key === 'ArrowUp') {
      event.preventDefault();
      if (activeList.length === 0) {
        return;
      }
      setHighlight((current) => {
        const next = (current - 1 + activeList.length) % activeList.length;
        setKeyboardScrollIndex(next);
        return next;
      });
      return;
    }
    // `#` opens quantity mode for the highlighted (or first) result — same as the # button.
    if (event.key === '#' || (event.key === '3' && event.shiftKey)) {
      const hit = activeList[highlight] ?? activeList[0];
      if (hit && hit.stockState !== 'OUT') {
        event.preventDefault();
        chooseForQuantity(hit);
      }
      return;
    }
    if (event.key === 'Enter') {
      event.preventDefault();
      const term = query.trim();
      if (looksLikeBarcode(term)) {
        void handleBarcodeScan(term);
        return;
      }
      const hit = activeList[highlight] ?? activeList[0];
      if (!hit) {
        return;
      }
      // Shift+Enter opens qty mode; plain Enter adds ×1 for counter speed.
      if (event.shiftKey) {
        if (hit.stockState === 'OUT') {
          setError(`${hit.sku} is out of stock at this shop.`);
          return;
        }
        chooseForQuantity(hit);
        return;
      }
      void handleOneTapAdd(hit);
      return;
    }
    if (event.key === 'Escape') {
      event.preventDefault();
      setQuery('');
      setDebouncedQuery('');
      setSelected(null);
      setCategoryId(null);
      setHighlight(0);
      setError(null);
    }
  }

  function handleQtyKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter') {
      event.preventDefault();
      if (selected) {
        void commitAdd(selected, quantity);
      }
      return;
    }
    if (event.key === 'Escape') {
      setSelected(null);
      searchRef.current?.focus();
    }
  }

  function beginPointerGesture(event: ReactPointerEvent) {
    pointerGestureRef.current = {
      x: event.clientX,
      y: event.clientY,
      moved: false,
    };
  }

  function trackPointerGesture(event: ReactPointerEvent) {
    const gesture = pointerGestureRef.current;
    if (!gesture || gesture.moved) {
      return;
    }
    if (Math.abs(event.clientX - gesture.x) > 10 || Math.abs(event.clientY - gesture.y) > 10) {
      gesture.moved = true;
    }
  }

  function wasTapGesture(): boolean {
    if (Date.now() < suppressTapUntilRef.current) {
      pointerGestureRef.current = null;
      return false;
    }
    const gesture = pointerGestureRef.current;
    pointerGestureRef.current = null;
    return !gesture?.moved;
  }

  function renderRow(hit: PosProductHit, index: number, style: CSSProperties) {
    const active = index === highlight;
    const out = hit.stockState === 'OUT';
    return (
      <div key={hit.id} style={style} className="pos-picker__row-wrap">
        <button
          type="button"
          className={`pos-picker__card${active ? ' pos-picker__card--active' : ''}${out ? ' pos-picker__card--out' : ''}`}
          onMouseEnter={() => {
            // Hover highlight only on real pointers — touch + mouseenter fights scrolling.
            if (window.matchMedia('(hover: hover) and (pointer: fine)').matches) {
              setHighlight(index);
            }
          }}
          onPointerDown={beginPointerGesture}
          onPointerMove={trackPointerGesture}
          onPointerCancel={() => {
            pointerGestureRef.current = null;
          }}
          onClick={() => {
            if (!wasTapGesture()) {
              return;
            }
            if (out) {
              setError(`${hit.sku} is out of stock at this shop.`);
              return;
            }
            void handleOneTapAdd(hit);
          }}
        >
          <div className="pos-picker__card-main">
            <strong className="pos-picker__name">{hit.name}</strong>
            <span className="pos-picker__meta">
              {hit.sku}
              {hit.brand ? ` · ${hit.brand}` : ''}
              {' · '}
              <span className={`pos-picker__stock ${stockClass(hit.stockState)}`}>
                {stockLabel(hit.stockState)}
                {hit.stockAvailable != null ? ` ${formatQty(hit.stockAvailable)}` : ''}
              </span>
            </span>
          </div>
          <div className="pos-picker__card-side">
            <span className="pos-picker__price">{formatMoney(hit.sellingPrice, hit.currencyCode || currencyCode)}</span>
            <span className="pos-picker__unit">/{formatUnit(hit.unitOfMeasure)}</span>
          </div>
        </button>
        <button
          type="button"
          className="pos-picker__qty-btn"
          aria-label="Choose quantity"
          title="Choose quantity"
          disabled={out}
          onPointerDown={beginPointerGesture}
          onPointerMove={trackPointerGesture}
          onClick={(event) => {
            event.stopPropagation();
            if (!wasTapGesture()) {
              return;
            }
            chooseForQuantity(hit);
          }}
        >
          #
        </button>
        <button
          type="button"
          className={`pos-picker__fav${hit.favorite ? ' pos-picker__fav--on' : ''}`}
          aria-label={hit.favorite ? 'Remove favorite' : 'Add favorite'}
          onPointerDown={beginPointerGesture}
          onPointerMove={trackPointerGesture}
          onClick={(event) => {
            event.stopPropagation();
            if (!wasTapGesture()) {
              return;
            }
            void handleFavoriteToggle(hit);
          }}
        >
          ★
        </button>
      </div>
    );
  }

  const canLoadMore = showSearchResults && results.length < totalElements && !loading && !loadingMore;

  return (
    <div className={`pos-picker${disabled ? ' pos-picker--disabled' : ''}`}>
      <div className="pos-picker__search-sticky">
        <label className="pos-picker__search-label" htmlFor={searchId}>
          Find product
        </label>
        <input
          ref={searchRef}
          id={searchId}
          type="search"
          className="input pos-picker__search"
          placeholder="Type name, SKU, or scan barcode…"
          value={query}
          disabled={disabled}
          autoComplete="off"
          autoCorrect="off"
          spellCheck={false}
          onChange={(event) => {
            setQuery(event.target.value);
            setSelected(null);
          }}
          onKeyDown={handleSearchKeyDown}
        />
        <p className="hint pos-picker__hint">
          Enter adds ×1 · # or Shift+Enter for qty · ↑↓ to move
        </p>
      </div>

      {!showSearchResults && quickPicks.length > 0 && (
        <div className="pos-picker__quick" aria-label="Quick picks">
          <div className="pos-picker__quick-label">Quick picks</div>
          <div className="pos-picker__quick-row">
            {quickPicks.map((hit) => (
              <button
                key={hit.id}
                type="button"
                className="pos-picker__quick-chip"
                onClick={() => void handleOneTapAdd(hit)}
              >
                <strong>{hit.name}</strong>
                <span>{formatMoney(hit.sellingPrice, hit.currencyCode || currencyCode)}</span>
              </button>
            ))}
          </div>
        </div>
      )}

      {visibleCategories.length > 0 && (
        <div className="pos-picker__chips" role="listbox" aria-label="Categories">
          <button
            type="button"
            className={`pos-picker__chip${!categoryId ? ' pos-picker__chip--active' : ''}`}
            onClick={() => setCategoryId(null)}
          >
            All
          </button>
          {visibleCategories.map((category) => (
            <button
              key={category.id}
              type="button"
              className={`pos-picker__chip${categoryId === category.id ? ' pos-picker__chip--active' : ''}`}
              onClick={() => setCategoryId(category.id)}
            >
              {category.name}
            </button>
          ))}
        </div>
      )}

      {selected && (
        <div className="pos-picker__speed">
          <div className="pos-picker__speed-product">
            <strong>{selected.name}</strong>
            <span className="muted">
              {selected.sku} · {formatMoney(selected.sellingPrice, selected.currencyCode || currencyCode)}
            </span>
          </div>
          <label className="pos-picker__speed-qty" htmlFor={qtyId}>
            Qty ({formatUnit(selected.unitOfMeasure)})
            <input
              ref={qtyRef}
              id={qtyId}
              type="number"
              min="0.01"
              step="any"
              className="input"
              value={quantity}
              onChange={(event) => setQuantity(event.target.value)}
              onKeyDown={handleQtyKeyDown}
            />
          </label>
          <button
            type="button"
            className="btn btn--primary btn--touch"
            onClick={() => void commitAdd(selected, quantity)}
          >
            Add
          </button>
        </div>
      )}

      {!showSearchResults && (
        <div className="pos-picker__tabs" role="tablist" aria-label="Quick access lists">
          {([
            ['recent', 'Recent'],
            ['frequent', 'Frequent'],
            ['favorites', 'Favorites'],
          ] as const).map(([id, label]) => (
            <button
              key={id}
              type="button"
              role="tab"
              aria-selected={quickTab === id}
              className={`pos-picker__tab${quickTab === id ? ' pos-picker__tab--active' : ''}`}
              onClick={() => {
                setQuickTab(id);
                setHighlight(0);
              }}
            >
              {label}
            </button>
          ))}
        </div>
      )}

      {error && <p className="form__error pos-picker__error">{error}</p>}

      {loading && <p className="hint pos-picker__status">Searching…</p>}
      {!loading && showSearchResults && results.length === 0 && (
        <p className="hint pos-picker__status">No products match “{debouncedQuery.trim() || 'this category'}”.</p>
      )}
      {!loading && !showSearchResults && quickItems.length === 0 && quickPicks.length === 0 && (
        <p className="hint pos-picker__status">
          Start typing a product name — common items will also appear here after a few sales.
        </p>
      )}

      {activeList.length > 0 && (
        <VirtualList
          className="pos-picker__list"
          items={activeList}
          itemHeight={ROW_HEIGHT}
          height={listHeight}
          scrollToIndex={keyboardScrollIndex}
          onUserScroll={() => {
            // Ignore the synthetic click that follows a finger-scroll on mobile.
            suppressTapUntilRef.current = Date.now() + 350;
            setKeyboardScrollIndex(null);
          }}
          renderItem={renderRow}
        />
      )}

      {canLoadMore && (
        <button
          type="button"
          className="btn btn--ghost btn--block pos-picker__more"
          onClick={() => setPage((current) => current + 1)}
        >
          {loadingMore ? 'Loading…' : `More results (${results.length}/${totalElements})`}
        </button>
      )}
    </div>
  );
}
