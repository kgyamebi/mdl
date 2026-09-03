export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface HealthStatus {
  status: string;
  application: string;
  version: string;
  timestamp: string;
  database: 'UP' | 'DOWN';
}

export interface AuthUser {
  id: number;
  email: string;
  username: string;
  fullName: string;
  businessId: number;
  businessCode: string;
  businessName: string;
  currencyCode: string;
  roles: string[];
  permissions: string[];
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMinutes: number;
  user: AuthUser;
}

export interface InventorySummary {
  balanceRowCount: number;
  lowStockCount: number;
  pendingAdjustmentRequests: number;
  activeReservations: number;
}

export interface InventoryBalance {
  id: number;
  locationId: number;
  locationCode: string;
  locationName: string;
  locationType: string;
  productId: number;
  productSku: string;
  productName: string;
  unitOfMeasure: string;
  quantityOnHand: number;
  quantityReserved: number;
  quantityAvailable: number;
  reorderLevel: number | null;
  belowReorderLevel: boolean;
  updatedAt: string;
}

export interface AttentionCategory {
  code: string;
  title: string;
  count: number;
  severity: string;
  summary: string;
}

export interface AlertItem {
  id: number;
  alertType: string;
  severity: string;
  module: string;
  title: string;
  summary: string;
  entityType: string;
  entityId: number;
  entityRef: string;
  details: string;
  status: string;
  acknowledgedBy: number | null;
  acknowledgedAt: string | null;
  resolvedAt: string | null;
  createdAt: string;
}

export interface OwnerAttentionReport {
  totalOpenAlerts: number;
  criticalCount: number;
  warningCount: number;
  categories: AttentionCategory[];
  recentAlerts: AlertItem[];
}

export interface ApprovalInboxSummary {
  adjustmentCount: number;
  transferCount: number;
  importCount: number;
  stocktakeCount: number;
  totalCount: number;
}

export interface ApprovalInboxItem {
  entityType: string;
  entityId: number;
  reference: string;
  title: string;
  summary: string;
  status: string;
  requiredPermission: string;
  requiredPermissions: string[];
  currentStepOrder: number;
  totalSteps: number;
  currentStepName: string;
  parallelStep: boolean;
  canAct: boolean;
  submittedAt: string;
  submittedBy: number;
}

export interface ApprovalInboxResponse {
  summary: ApprovalInboxSummary;
  items: PageResponse<ApprovalInboxItem>;
}

export interface Product {
  id: number;
  sku: string;
  name: string;
  description: string | null;
  brand: string | null;
  categoryId: number | null;
  categoryName: string | null;
  unitOfMeasure: string;
  costPrice: number;
  sellingPrice: number;
  currencyCode: string;
  taxInclusive: boolean;
  trackInventory: boolean;
  reorderLevel: number | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export type ApprovalEntityType =
  | 'INVENTORY_ADJUSTMENT'
  | 'STOCK_TRANSFER'
  | 'IMPORT_ORDER'
  | 'STOCKTAKE';

export interface Warehouse {
  id: number;
  code: string;
  name: string;
  warehouseType: string;
  restricted: boolean;
  description: string | null;
  status: string;
}

export interface StockTransferItem {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  unitOfMeasure: string;
  requestedQuantity: number;
  dispatchedQuantity: number;
  receivedQuantity: number;
  remainingToReceive: number;
  notes: string | null;
}

export interface StockTransfer {
  id: number;
  transferNumber: string;
  fromWarehouseId: number;
  fromWarehouseCode: string;
  fromWarehouseName: string;
  toWarehouseId: number;
  toWarehouseCode: string;
  toWarehouseName: string;
  fromLocationId: number;
  fromLocationCode: string;
  toLocationId: number;
  toLocationCode: string;
  status: string;
  notes: string | null;
  requestedBy: number;
  approvedBy: number | null;
  approvedAt: string | null;
  dispatchedBy: number | null;
  dispatchedAt: string | null;
  rejectedBy: number | null;
  rejectedAt: string | null;
  rejectReason: string | null;
  items: StockTransferItem[];
  createdAt: string;
  updatedAt: string;
}

export type StockTransferStatus =
  | 'REQUESTED'
  | 'APPROVED'
  | 'DISPATCHED'
  | 'PARTIALLY_RECEIVED'
  | 'RECEIVED'
  | 'REJECTED'
  | 'CANCELLED';

export interface LocationSummary {
  id: number;
  code: string;
  name: string;
  locationType: string;
  city: string | null;
  country: string | null;
  status: string;
}

export interface ImportItem {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  unitOfMeasure: string;
  expectedQuantity: number;
  receivedQuantity: number;
  remainingQuantity: number;
  unitCost: number;
  notes: string | null;
}

export interface ImportOrder {
  id: number;
  importNumber: string;
  supplierName: string;
  supplierReference: string | null;
  destinationLocationId: number;
  destinationLocationCode: string;
  destinationLocationName: string;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
  status: string;
  expectedArrivalDate: string | null;
  notes: string | null;
  assignedReceiverUserId: number | null;
  createdBy: number;
  approvedBy: number | null;
  approvedAt: string | null;
  verifiedBy: number | null;
  verifiedAt: string | null;
  items: ImportItem[];
  createdAt: string;
  updatedAt: string;
}

export interface Shop {
  id: number;
  code: string;
  name: string;
  status: string;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
}

export interface SaleItem {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  unitOfMeasure: string;
  quantity: number;
  quantityReturned: number;
  unitPrice: number;
  lineTotal: number;
}

export interface SalePayment {
  id: number;
  paymentMethod: string;
  amount: number;
  reference: string | null;
  receivedBy: number;
  createdAt: string;
}

export interface Sale {
  id: number;
  saleNumber: string;
  shopId: number;
  shopCode: string;
  shopName: string;
  shopLocationId: number;
  warehouseLocationId: number;
  currencyCode: string;
  status: string;
  subtotal: number;
  totalAmount: number;
  returnedAmount: number;
  customerName: string | null;
  notes: string | null;
  soldBy: number;
  cancelledBy: number | null;
  cancelledAt: string | null;
  cancelReason: string | null;
  refundedBy: number | null;
  refundedAt: string | null;
  refundReason: string | null;
  items: SaleItem[];
  payments: SalePayment[];
  createdAt: string;
  updatedAt: string;
}

export type PaymentMethod = 'CASH' | 'MOBILE_MONEY' | 'CARD' | 'BANK_TRANSFER';

export interface NotificationItem {
  id: number;
  notificationType: string;
  category: string;
  title: string;
  message: string;
  entityType: string | null;
  entityId: number | null;
  entityRef: string | null;
  sourceType: string | null;
  sourceId: number | null;
  status: string;
  readAt: string | null;
  dismissedAt: string | null;
  createdAt: string;
}

export interface UnreadNotificationCount {
  unreadCount: number;
}

export interface ReportExport {
  id: number;
  reportType: string;
  exportFormat: string;
  fileName: string;
  rowCount: number;
  parameters: string;
  status: string;
  exportedBy: number;
  createdAt: string;
}
