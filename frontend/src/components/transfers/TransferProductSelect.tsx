import { useCallback, useEffect, useId, useRef, useState } from 'react';
import { fetchInventoryBalances } from '../../services/inventoryService';
import type { InventoryBalance } from '../../types/api';

const SEARCH_DEBOUNCE_MS = 250;
const PAGE_SIZE = 50;

interface TransferProductSelectProps {
  locationId: number | null;
  value: InventoryBalance | null;
  onChange: (balance: InventoryBalance | null) => void;
  disabled?: boolean;
  inputId?: string;
}

function formatQty(value: number): string {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(value);
}

function productLabel(balance: InventoryBalance): string {
  return `${balance.productSku} — ${balance.productName}`;
}

export function TransferProductSelect({
  locationId,
  value,
  onChange,
  disabled = false,
  inputId,
}: TransferProductSelectProps) {
  const generatedId = useId();
  const fieldId = inputId ?? `transfer-product-${generatedId}`;
  const listboxId = `${fieldId}-listbox`;

  const containerRef = useRef<HTMLDivElement>(null);
  const requestRef = useRef(0);
  const ignoreNextFocusRef = useRef(false);

  const [query, setQuery] = useState('');
  const [editing, setEditing] = useState(false);
  const [open, setOpen] = useState(false);
  const [results, setResults] = useState<InventoryBalance[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [highlight, setHighlight] = useState(0);
  const [totalMatches, setTotalMatches] = useState(0);

  const inputValue = editing ? query : value ? productLabel(value) : '';
  const pickerDisabled = disabled || locationId == null;

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
    if (!editing || locationId == null) {
      return;
    }

    const requestId = ++requestRef.current;
    setLoading(true);
    setError(null);

    const timer = setTimeout(() => {
      fetchInventoryBalances({
        locationId,
        search: query.trim() || undefined,
        minQuantity: 0.01,
        size: PAGE_SIZE,
        page: 0,
      })
        .then((response) => {
          if (requestRef.current !== requestId) {
            return;
          }
          setResults(response.items.filter((item) => item.quantityAvailable > 0));
          setTotalMatches(response.totalElements);
          setHighlight(0);
        })
        .catch((err: Error) => {
          if (requestRef.current === requestId) {
            setResults([]);
            setTotalMatches(0);
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
  }, [query, editing, locationId]);

  function selectBalance(balance: InventoryBalance) {
    ignoreNextFocusRef.current = true;
    onChange(balance);
    closeAndReset();
  }

  function handleOptionPointerDown(event: React.PointerEvent<HTMLButtonElement>, balance: InventoryBalance) {
    event.preventDefault();
    event.stopPropagation();
    selectBalance(balance);
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
    if (event.key === 'Enter' && open && results[highlight]) {
      event.preventDefault();
      selectBalance(results[highlight]);
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
            showDropdown && results[highlight] ? `${listboxId}-option-${results[highlight].productId}` : undefined
          }
          autoComplete="off"
          disabled={pickerDisabled}
          placeholder={
            locationId == null
              ? 'Select a source location first…'
              : 'Type or scroll to pick stock on hand…'
          }
          value={inputValue}
          onFocus={() => {
            if (pickerDisabled) {
              return;
            }
            if (ignoreNextFocusRef.current) {
              ignoreNextFocusRef.current = false;
              return;
            }
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
          {loading && <li className="product-search__message">Loading stock…</li>}
          {error && <li className="product-search__message product-search__message--error">{error}</li>}
          {!loading && !error && results.length === 0 && (
            <li className="product-search__message">
              {query.trim()
                ? `No transferable stock matches “${query.trim()}”.`
                : 'No stock on hand at this location to transfer.'}
            </li>
          )}
          {results.map((balance, index) => (
            <li key={balance.productId} role="none">
              <button
                type="button"
                id={`${listboxId}-option-${balance.productId}`}
                role="option"
                aria-selected={index === highlight}
                className={`product-search__option${
                  index === highlight ? ' product-search__option--active' : ''
                }`}
                onPointerEnter={() => setHighlight(index)}
                onPointerDown={(event) => handleOptionPointerDown(event, balance)}
                onClick={(event) => {
                  event.preventDefault();
                  event.stopPropagation();
                }}
              >
                <span className="product-search__option-code">{balance.productSku}</span>
                <span className="product-search__option-name">{balance.productName}</span>
                <span className="product-search__option-hint">
                  {formatQty(balance.quantityAvailable)} {balance.unitOfMeasure.toLowerCase()} available
                </span>
              </button>
            </li>
          ))}
          {!loading && totalMatches > results.length && (
            <li className="product-search__message">
              Showing {results.length} of {totalMatches}. Type a name or code to find the rest.
            </li>
          )}
        </ul>
      )}
    </div>
  );
}
