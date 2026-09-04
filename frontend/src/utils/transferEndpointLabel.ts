import type { Shop, TransferShopOption, TransferWarehouseOption, Warehouse } from '../types/api';

export type TransferEndpointKind = 'MAIN' | 'SHOP_STOCK' | 'OTHER';

type TransferEndpoint = Pick<Warehouse, 'id' | 'code' | 'name' | 'warehouseType'> & {
  linkedShopName?: string | null;
};

function toEndpoint(
  warehouse: TransferWarehouseOption | Warehouse,
): TransferEndpoint {
  if ('linkedShopName' in warehouse) {
    return warehouse;
  }
  return warehouse;
}

export function getTransferEndpointKind(
  warehouse: TransferEndpoint,
  shops: Array<Pick<Shop, 'warehouseId'>>,
): TransferEndpointKind {
  if (warehouse.linkedShopName || shops.some((shop) => shop.warehouseId === warehouse.id)) {
    return 'SHOP_STOCK';
  }
  if (warehouse.warehouseType === 'MAIN') {
    return 'MAIN';
  }
  return 'OTHER';
}

export function formatTransferEndpointLabel(
  warehouse: TransferEndpoint,
  shops: Array<Pick<Shop, 'warehouseId' | 'name'>>,
): string {
  if (warehouse.linkedShopName) {
    return `${warehouse.linkedShopName} (shop stock)`;
  }
  const shop = shops.find((entry) => entry.warehouseId === warehouse.id);
  if (shop) {
    return `${shop.name} (shop stock)`;
  }
  if (warehouse.warehouseType === 'MAIN') {
    return `${warehouse.name} (main warehouse)`;
  }
  return warehouse.name;
}

export function groupWarehousesForTransfer(
  warehouses: TransferEndpoint[],
  shops: Array<Pick<Shop, 'warehouseId' | 'name'>>,
) {
  const main: TransferEndpoint[] = [];
  const shopStock: TransferEndpoint[] = [];
  const other: TransferEndpoint[] = [];

  for (const warehouse of warehouses) {
    const kind = getTransferEndpointKind(warehouse, shops);
    if (kind === 'MAIN') {
      main.push(warehouse);
    } else if (kind === 'SHOP_STOCK') {
      shopStock.push(warehouse);
    } else {
      other.push(warehouse);
    }
  }

  return { main, shopStock, other };
}

export function formatTransferRouteLabel(
  fromWarehouseId: number,
  toWarehouseId: number,
  warehouses: TransferEndpoint[],
  shops: Array<Pick<Shop, 'warehouseId' | 'name'>>,
): string {
  const from = warehouses.find((warehouse) => warehouse.id === fromWarehouseId);
  const to = warehouses.find((warehouse) => warehouse.id === toWarehouseId);
  if (!from || !to) {
    return 'Unknown route';
  }
  return `${formatTransferEndpointLabel(from, shops)} → ${formatTransferEndpointLabel(to, shops)}`;
}

export function mapTransferFormOptions(options: {
  warehouses: TransferWarehouseOption[];
  shops: TransferShopOption[];
}) {
  return {
    warehouses: options.warehouses.map(toEndpoint),
    shops: options.shops.map((shop) => ({
      id: shop.id,
      code: shop.code,
      name: shop.name,
      warehouseId: shop.warehouseId,
    })),
  };
}
