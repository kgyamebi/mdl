import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import { fetchSale } from '../services/salesService';
import { createSaleReturn, fetchSaleReturn, fetchSaleReturns } from '../services/saleReturnsService';
import type { Sale, SaleReturn } from '../types/api';

function formatMoney(value: number, currencyCode: string): string {
  return new Intl.NumberFormat(undefined, { style: 'currency', currency: currencyCode }).format(value);
}

export function ReturnsPage() {
  const { hasPermission, user } = useAuth();
  const canReturn = hasPermission('sale:return');
  const currencyCode = user?.currencyCode ?? 'GHS';

  const [items, setItems] = useState<SaleReturn[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selectedReturn, setSelectedReturn] = useState<SaleReturn | null>(null);
  const [page] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [saleIdInput, setSaleIdInput] = useState('');
  const [sale, setSale] = useState<Sale | null>(null);
  const [returnQty, setReturnQty] = useState<Record<number, number>>({});
  const [reason, setReason] = useState('');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadReturns = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetchSaleReturns(page, 20);
      setItems(response.items);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load returns');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    loadReturns();
  }, [loadReturns]);

  useEffect(() => {
    if (selectedId == null) {
      setSelectedReturn(null);
      return;
    }
    fetchSaleReturn(selectedId).then(setSelectedReturn).catch((err) => {
      setError(err instanceof Error ? err.message : 'Failed to load return');
    });
  }, [selectedId]);

  async function loadSaleForReturn() {
    const id = Number(saleIdInput);
    if (!id) {
      setError('Enter a valid sale ID');
      return;
    }
    setError(null);
    try {
      const loaded = await fetchSale(id);
      if (loaded.status !== 'COMPLETED') {
        setError('Only completed sales can be returned');
        return;
      }
      setSale(loaded);
      const qty: Record<number, number> = {};
      loaded.items.forEach((item) => {
        qty[item.id] = 0;
      });
      setReturnQty(qty);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Sale not found');
      setSale(null);
    }
  }

  async function handleSubmitReturn(event: FormEvent) {
    event.preventDefault();
    if (!sale || !canReturn) {
      return;
    }

    const returnItems = sale.items
      .filter((item) => (returnQty[item.id] ?? 0) > 0)
      .map((item) => ({ saleItemId: item.id, quantity: returnQty[item.id] }));

    if (returnItems.length === 0) {
      setError('Select at least one item to return');
      return;
    }

    const totalRefund = returnItems.reduce((sum, ri) => {
      const item = sale.items.find((i) => i.id === ri.saleItemId)!;
      return sum + item.unitPrice * ri.quantity;
    }, 0);

    setSubmitting(true);
    setError(null);
    try {
      const created = await createSaleReturn(sale.id, {
        reason: reason.trim(),
        notes: notes.trim() || undefined,
        items: returnItems,
        refunds: [{ paymentMethod: 'CASH', amount: totalRefund }],
      });
      setSale(null);
      setSaleIdInput('');
      setReason('');
      setNotes('');
      setSelectedId(created.id);
      loadReturns();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Return failed');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Operations</p>
          <h1>Returns</h1>
          <p className="subtitle">Process customer returns and restock inventory</p>
        </div>
      </header>

      {canReturn && (
        <section className="panel">
          <h2>Process return</h2>
          <div className="form form--inline">
            <label className="form__field">
              <span>Sale ID</span>
              <input className="input" value={saleIdInput} onChange={(e) => setSaleIdInput(e.target.value)} />
            </label>
            <button type="button" className="btn btn--ghost" onClick={loadSaleForReturn}>
              Load sale
            </button>
          </div>
          {sale && (
            <form className="form" onSubmit={handleSubmitReturn}>
              <p className="muted">
                {sale.saleNumber} · {formatMoney(sale.totalAmount, sale.currencyCode)}
              </p>
              {sale.items.map((item) => (
                <label key={item.id} className="form__field">
                  <span>
                    {item.productSku} — max {item.quantity}
                  </span>
                  <input
                    className="input"
                    type="number"
                    min={0}
                    max={item.quantity}
                    step="0.01"
                    value={returnQty[item.id] ?? 0}
                    onChange={(e) =>
                      setReturnQty((prev) => ({ ...prev, [item.id]: Number(e.target.value) }))
                    }
                  />
                </label>
              ))}
              <label className="form__field">
                <span>Reason</span>
                <input className="input" value={reason} onChange={(e) => setReason(e.target.value)} required />
              </label>
              <label className="form__field">
                <span>Notes</span>
                <textarea className="textarea" rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} />
              </label>
              <button type="submit" className="btn btn--primary" disabled={submitting}>
                {submitting ? 'Processing…' : 'Submit return'}
              </button>
            </form>
          )}
        </section>
      )}

      {error && <p className="form__error">{error}</p>}
      {loading && <p className="muted">Loading returns…</p>}

      {!loading && (
        <div className={`workspace-split${selectedId != null ? ' workspace-split--open' : ''}`}>
          <div className="workspace-split__list">
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Return</th>
                    <th>Sale</th>
                    <th>Refund</th>
                    <th>Reason</th>
                    <th>Date</th>
                  </tr>
                </thead>
                <tbody>
                  {items.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="muted">
                        No returns yet.
                      </td>
                    </tr>
                  ) : (
                    items.map((item) => (
                      <tr
                        key={item.id}
                        className={`table__row--clickable${selectedId === item.id ? ' table__row--selected' : ''}`}
                        onClick={() => setSelectedId(item.id)}
                      >
                        <td>{item.returnNumber}</td>
                        <td>{item.saleNumber}</td>
                        <td>{formatMoney(item.totalRefundAmount, item.currencyCode || currencyCode)}</td>
                        <td>{item.reason}</td>
                        <td>{new Date(item.createdAt).toLocaleString()}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
          {selectedReturn && (
            <aside className="workspace-split__detail panel">
              <h2>{selectedReturn.returnNumber}</h2>
              <p className="muted">Sale {selectedReturn.saleNumber}</p>
              <p>{selectedReturn.reason}</p>
              <ul>
                {selectedReturn.items.map((item) => (
                  <li key={item.id}>
                    {item.productSku} × {item.quantity} —{' '}
                    {formatMoney(item.lineRefund, selectedReturn.currencyCode)}
                  </li>
                ))}
              </ul>
            </aside>
          )}
        </div>
      )}
    </div>
  );
}
