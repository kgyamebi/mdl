import type { Sale } from '../types/api';
import { mdlBrandHtml } from '../components/brand/brandMark';

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function formatMoney(value: number, currencyCode: string): string {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: currencyCode,
    maximumFractionDigits: 2,
  }).format(value);
}

export function printSaleReceipt(sale: Sale, businessName: string): void {
  const safeBusinessName = escapeHtml(businessName);
  const safeSaleNumber = escapeHtml(sale.saleNumber);
  const safeCreatedAt = escapeHtml(new Date(sale.createdAt).toLocaleString());

  const lines = sale.items
    .map(
      (item) =>
        `<tr><td>${escapeHtml(item.productName)}</td><td>${escapeHtml(String(item.quantity))}</td><td>${formatMoney(item.unitPrice, sale.currencyCode)}</td><td>${formatMoney(item.lineTotal, sale.currencyCode)}</td></tr>`,
    )
    .join('');

  const payments = sale.payments
    .map(
      (p) =>
        `<tr><td>${escapeHtml(p.paymentMethod)}</td><td>${formatMoney(p.amount, sale.currencyCode)}</td></tr>`,
    )
    .join('');

  const html = `<!DOCTYPE html>
<html><head><title>Receipt ${safeSaleNumber}</title>
<style>
  body { font-family: monospace; max-width: 320px; margin: 1rem auto; }
  h1 { font-size: 1.1rem; text-align: center; }
  table { width: 100%; border-collapse: collapse; font-size: 0.85rem; }
  td { padding: 0.2rem 0; vertical-align: top; }
  .total { font-weight: bold; border-top: 1px dashed #000; padding-top: 0.4rem; }
  @media print { body { margin: 0; } }
</style></head><body>
  ${mdlBrandHtml('#111')}
  <h1>${safeBusinessName}</h1>
  <p>Sale ${safeSaleNumber}<br/>${safeCreatedAt}</p>
  <table>${lines}</table>
  <p class="total">Total: ${formatMoney(sale.totalAmount, sale.currencyCode)}</p>
  <table>${payments}</table>
  <p style="text-align:center;margin-top:1rem;">Thank you</p>
</body></html>`;

  const popup = window.open('', '_blank', 'width=400,height=640');
  if (!popup) {
    return;
  }
  popup.document.write(html);
  popup.document.close();
  popup.focus();
  popup.print();
}
