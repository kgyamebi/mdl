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
  mfaEnabled: boolean;
  roles: string[];
  permissions: string[];
}

export interface LoginResponse {
  accessToken: string | null;
  refreshToken: string | null;
  tokenType: string;
  expiresInMinutes: number;
  user: AuthUser | null;
  mfaRequired?: boolean;
  mfaToken?: string;
}

export interface MfaSetupResponse {
  secret: string;
  otpAuthUrl: string;
}

export interface SaleReturnItem {
  id: number;
  saleItemId: number;
  productId: number;
  productSku: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineRefund: number;
}

export interface SaleReturnRefund {
  id: number;
  paymentMethod: string;
  amount: number;
  reference: string | null;
  processedBy: number;
  createdAt: string;
}

export interface SaleReturn {
  id: number;
  returnNumber: string;
  saleId: number;
  saleNumber: string;
  shopId: number;
  currencyCode: string;
  status: string;
  totalRefundAmount: number;
  reason: string;
  notes: string | null;
  processedBy: number;
  items: SaleReturnItem[];
  refunds: SaleReturnRefund[];
  createdAt: string;
}

export interface InventorySummary {
  balanceRowCount: number;
  lowStockCount: number;
  pendingAdjustmentRequests: number;
  activeReservations: number;
}

export interface BusinessOverviewReport {
  currencyCode: string;
  completedSalesToday: number;
  salesAmountToday: number;
  lowStockBalanceCount: number;
  pendingTransferRequests: number;
  activeTemporaryPermissions: number;
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
  barcodes?: ProductBarcode[];
  createdAt: string;
  updatedAt: string;
}

export interface ProductBarcode {
  id: number;
  barcode: string;
  barcodeType: string;
  primary: boolean;
}

export interface ProductCategory {
  id: number;
  name: string;
  code: string;
  status: string;
}

export interface AuditLog {
  id: number;
  userId: number | null;
  action: string;
  module: string;
  entityType: string | null;
  entityId: number | null;
  entityRef: string | null;
  summary: string;
  details: string | null;
  ipAddress: string | null;
  createdAt: string;
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

export interface TransferWarehouseOption {
  id: number;
  code: string;
  name: string;
  warehouseType: string;
  linkedShopId: number | null;
  linkedShopName: string | null;
}

export interface TransferShopOption {
  id: number;
  code: string;
  name: string;
  warehouseId: number;
}

export interface TransferFormOptions {
  warehouses: TransferWarehouseOption[];
  shops: TransferShopOption[];
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
  warehouseLocationId: number;
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

export interface CopilotMessage {
  id: number;
  role: 'USER' | 'ASSISTANT';
  content: string;
  createdAt: string;
}

export interface CopilotChatResponse {
  conversationId: number;
  reply: string;
  provider: string;
  model: string | null;
  promptTokens: number;
  completionTokens: number;
  suggestedFollowUps: string[];
}

export interface CopilotSuggestedPrompt {
  prompt: string;
  category: string;
}

export interface ManagedUser {
  id: number;
  email: string;
  username: string;
  firstName: string;
  lastName: string;
  fullName: string;
  phone: string | null;
  status: string;
  roles: string[];
  locations: UserLocationAssignment[];
  lastLoginAt: string | null;
  createdAt: string;
}

export interface CreatedUser extends ManagedUser {
  generatedPassword?: string | null;
}

export interface UserLocationAssignment {
  locationId: number;
  locationCode: string;
  locationName: string;
  accessLevel: string;
}

export interface RoleOption {
  code: string;
  name: string;
  description: string | null;
}

export interface PermissionOption {
  code: string;
  name: string;
  module: string;
}

export interface BusinessProfile {
  id: number;
  code: string;
  name: string;
  legalName: string | null;
  currencyCode: string;
  timezone: string;
  status: string;
}

export interface CurrencyOption {
  code: string;
  name: string;
  symbol: string;
}

export interface BusinessStructure {
  business: { code: string; name: string; currencyCode: string };
  mainWarehouses: Warehouse[];
  shopWarehouses: Warehouse[];
  shops: Shop[];
  transferRouteCount: number;
}

export interface TransferRoute {
  id: number;
  fromWarehouseId: number;
  fromWarehouseCode: string;
  fromWarehouseName: string;
  toWarehouseId: number;
  toWarehouseCode: string;
  toWarehouseName: string;
  enabled: boolean;
  notes: string | null;
}

export interface ApprovalRule {
  id: number;
  code: string;
  name: string;
  description: string | null;
  entityType: ApprovalEntityType;
  requiredPermission: string;
  minAbsQuantity: number | null;
  enabled: boolean;
  priority: number;
  steps: ApprovalRuleStep[];
  createdAt: string;
  updatedAt: string;
}

export interface ApprovalRuleStep {
  id: number;
  stepOrder: number;
  name: string;
  requiredPermission: string;
}

export interface Stocktake {
  id: number;
  stocktakeNumber: string;
  locationId: number;
  locationCode: string;
  locationName: string;
  status: string;
  notes: string | null;
  lineCount: number;
  varianceLineCount: number;
  totalVariance: number;
  lines: StocktakeLine[];
  submittedAt: string | null;
  approvedAt: string | null;
  cancelReason: string | null;
  createdAt: string;
}

export interface StocktakeLine {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  unitOfMeasure: string;
  expectedQuantity: number;
  countedQuantity: number | null;
  variance: number | null;
  notes: string | null;
}
