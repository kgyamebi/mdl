import { useCallback, useEffect, useId, useRef, useState } from 'react';
import { fetchProducts } from '../../services/productsService';
import type { Product } from '../../types/api';

const SEARCH_DEBOUNCE_MS = 250;
const MAX_RESULTS = 25;

interface ProductSearchSelectProps {
  value: Product | null;
  onChange: (product: Product | null) => void;
  /** Optional per-product line shown under each suggestion, e.g. stock at the selected shop. */
  hintFor?: (product: Product) => string;
  /** Called with each batch of results so the caller can load extra detail for them. */
  onResults?: (products: Product[], term: string) => void;
  placeholder?: string;
  disabled?: boolean;
  inputId?: string;
}

function productLabel(product: Product): string {
  return `${product.sku} — ${product.name}`;
}

export function ProductSearchSelect({
  value,
  onChange,
  hintFor,
  onResults,
  placeholder = 'Type item name or code…',
  disabled = false,
  inputId,
}: ProductSearchSelectProps) {
  const generatedId = useId();
  const fieldId = inputId ?? `product-search-${generatedId}`;
  const listboxId = `${fieldId}-listbox`;

  const containerRef = useRef<HTMLDivElement>(null);
  const requestRef = useRef(0);

  // Held in a ref so a caller passing an inline callback can't restart the search.
  const onResultsRef = useRef(onResults);
  onResultsRef.current = onResults;

  const [query, setQuery] = useState('');
  const [editing, setEditing] = useState(false);
  const [open, setOpen] = useState(false);
  const [results, setResults] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [highlight, setHighlight] = useState(0);

  const inputValue = editing ? query : value ? productLabel(value) : '';

  const closeAndReset = useCallback(() => {
    setOpen(false);
    setEditing(false);
    setQuery('');
    setHighlight(0);
  }, []);

  useEffect(() => {
    function handlePointerDown(event: PointerEvent) {
      if (!containerRef.current?.contains(event.target as Node)) {
        closeAndReset();
      }
    }
    document.addEventListener('pointerdown', handlePointerDown);
    return () => document.removeEventListener('pointerdown', handlePointerDown);
  }, [closeAndReset]);

  useEffect(() => {
    if (!editing) {
      return;
    }

    const term = query.trim();
    if (term.length === 0) {
      setResults([]);
      setLoading(false);
      setError(null);
      return;
    }

    const requestId = ++requestRef.current;
    setLoading(true);
    setError(null);

    const timer = setTimeout(() => {
      fetchProducts({ search: term, status: 'ACTIVE', size: MAX_RESULTS, page: 0 })
        .then((response) => {
          // Ignore responses from superseded keystrokes.
          if (requestRef.current !== requestId) {
            return;
          }
          setResults(response.items);
          setHighlight(0);
          onResultsRef.current?.(response.items, term);
        })
        .catch((err: Error) => {
          if (requestRef.current === requestId) {
            setResults([]);
            setError(err.message || 'Search failed');
          }
        })
        .finally(() => {
          if (requestRef.current === requestId) {
            setLoading(false);
          }
        });
    }, SEARCH_DEBOUNCE_MS);

    return () => clearTimeout(timer);
  }, [query, editing]);

  function selectProduct(product: Product) {
    onChange(product);
    closeAndReset();
  }

  function handleKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setOpen(true);
      setHighlight((current) => (results.length === 0 ? 0 : (current + 1) % results.length));
      return;
    }
    if (event.key === 'ArrowUp') {
      event.preventDefault();
      setHighlight((current) => (results.length === 0 ? 0 : (current - 1 + results.length) % results.length));
      return;
    }
    if (event.key === 'Enter') {
      if (open && results[highlight]) {
        event.preventDefault();
        selectProduct(results[highlight]);
      }
      return;
    }
    if (event.key === 'Escape') {
      event.preventDefault();
      closeAndReset();
    }
  }

  const showDropdown = open && editing;

  return (
    <div className="product-search" ref={containerRef}>
      <div className="product-search__control">
        <input
          id={fieldId}
          type="text"
          className="input product-search__input"
          role="combobox"
          aria-expanded={showDropdown}
          aria-controls={listboxId}
          aria-autocomplete="list"
          aria-activedescendant={
            showDropdown && results[highlight] ? `${listboxId}-option-${results[highlight].id}` : undefined
          }
          autoComplete="off"
          disabled={disabled}
          placeholder={placeholder}
          value={inputValue}
          onFocus={() => {
            setEditing(true);
            setQuery('');
            setOpen(true);
          }}
          onChange={(event) => {
            setEditing(true);
            setOpen(true);
            setQuery(event.target.value);
          }}
          onKeyDown={handleKeyDown}
        />
        {value && !editing && (
          <button
            type="button"
            className="product-search__clear"
            aria-label="Clear selected item"
            onClick={() => {
              onChange(null);
              closeAndReset();
            }}
          >
            ×
          </button>
        )}
      </div>

      {showDropdown && (
        <ul className="product-search__results" id={listboxId} role="listbox">
          {query.trim().length === 0 && (
            <li className="product-search__message">Start typing to find an item by name or code.</li>
          )}
          {query.trim().length > 0 && loading && (
            <li className="product-search__message">Searching…</li>
          )}
          {error && <li className="product-search__message product-search__message--error">{error}</li>}
          {query.trim().length > 0 && !loading && !error && results.length === 0 && (
            <li className="product-search__message">No items match “{query.trim()}”.</li>
          )}
          {results.map((product, index) => {
            const hint = hintFor?.(product) ?? '';
            return (
              <li key={product.id} role="none">
                <button
                  type="button"
                  id={`${listboxId}-option-${product.id}`}
                  role="option"
                  aria-selected={index === highlight}
                  className={`product-search__option${
                    index === highlight ? ' product-search__option--active' : ''
                  }`}
                  onPointerEnter={() => setHighlight(index)}
                  onClick={() => selectProduct(product)}
                >
                  <span className="product-search__option-code">{product.sku}</span>
                  <span className="product-search__option-name">{product.name}</span>
                  {hint && <span className="product-search__option-hint">{hint}</span>}
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
