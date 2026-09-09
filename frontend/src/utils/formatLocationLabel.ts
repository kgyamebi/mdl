/** Short, consistent labels for shop floors and warehouses. */
export function formatLocationLabel(name: string): string {
  const raw = name.trim();
  if (!raw) {
    return raw;
  }

  if (/main\s+warehouse/i.test(raw)) {
    return 'Main warehouse';
  }

  const warehouseMatch = raw.match(/warehouse\s+([a-z0-9]+)\s*$/i);
  if (warehouseMatch) {
    return `Warehouse ${warehouseMatch[1].toUpperCase()}`;
  }

  if (/shop$/i.test(raw)) {
    const code = raw
      .replace(/^modern\s+dream\s+/i, '')
      .replace(/\s*shop$/i, '')
      .trim();
    return code ? `Shop ${code.toUpperCase()}` : 'Shop';
  }

  return raw
    .replace(/^modern\s+dream\s+/i, '')
    .replace(/\s+/g, ' ')
    .toLowerCase()
    .replace(/\b([a-z0-9])/g, (ch) => ch.toUpperCase());
}
