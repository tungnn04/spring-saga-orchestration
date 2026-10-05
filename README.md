# Spring Saga Orchestration - E-Commerce Demo

Demo pattern **Saga Orchestration** với Spring Boot 3.3 + Java 21 cho bài toán e-commerce.

## Kiến trúc

```
POST /api/orders
        │
        ▼
  ┌─────────────┐
  │ order-service│  ← Saga Orchestrator (port 8081)
  │  :8081      │
  └──────┬──────┘
         │
         ├─── Step 1: Process Payment ──► payment-service :8082
         │                                   └── compensation: refundPayment
         │
         └─── Step 2: Reserve Inventory ─► inventory-service :8083
                                               └── compensation: releaseReservation
```

## Services

| Service | Port | DB | Vai trò |
|---------|------|----|---------|
| `order-service` | 8081 | order_db:5432 | **Orchestrator** - điều phối saga |
| `payment-service` | 8082 | payment_db:5433 | Participant - xử lý thanh toán |
| `inventory-service` | 8083 | inventory_db:5434 | Participant - quản lý tồn kho |

## Các tính năng chính

- ✅ **SagaStep<T>** - Generic step với action + compensation + retry
- ✅ **SagaExecutor** - Tự động rollback (compensate) khi step thất bại
- ✅ **Idempotency** - Chống duplicate request bằng `idempotencyKey`
- ✅ **Exponential Backoff** - Retry compensation với backoff tăng dần
- ✅ **Pessimistic Locking** - Tránh overselling trong inventory
- ✅ **Metrics** - `@SagaStepMetric` AOP aspect + Micrometer/Prometheus
- ✅ **Actuator** - Health check endpoint

## Quick Start

### 1. Khởi động databases

```bash
docker-compose up -d
```

### 2. Chạy từng service

```bash
# Terminal 1 - Order Service (Orchestrator)
cd order-service && ./mvnw spring-boot:run

# Terminal 2 - Payment Service
cd payment-service && ./mvnw spring-boot:run

# Terminal 3 - Inventory Service
cd inventory-service && ./mvnw spring-boot:run
```

## API Examples

### Tạo đơn hàng (Happy Path)

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "productId": "PROD-001",
    "quantity": 2,
    "unitPrice": 999.99,
    "idempotencyKey": "order-key-001"
  }'
```

**Response khi thành công:**
```json
{
  "success": true,
  "orderId": "uuid...",
  "message": "Saga completed successfully",
  "order": {
    "status": "COMPLETED",
    ...
  }
}
```

### Simulate failure (Payment vượt limit > 10000)

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "productId": "PROD-001",
    "quantity": 1,
    "unitPrice": 15000.00,
    "idempotencyKey": "order-key-002"
  }'
```

**Response khi fail (saga tự rollback):**
```json
{
  "success": false,
  "message": "Saga failed at step: process-payment - Payment amount exceeds limit: 15000.00",
  "failedStep": "process-payment"
}
```

### Simulate failure (Inventory không đủ)

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "productId": "PROD-999",
    "quantity": 1,
    "unitPrice": 100.00,
    "idempotencyKey": "order-key-003"
  }'
```

### Idempotency Test (gửi lại request cũ)

```bash
# Gửi lần 2 với cùng idempotencyKey - sẽ trả về kết quả cũ, KHÔNG tạo order mới
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "productId": "PROD-001",
    "quantity": 2,
    "unitPrice": 999.99,
    "idempotencyKey": "order-key-001"
  }'
```

### Lấy thông tin order

```bash
curl http://localhost:8081/api/orders/{orderId}
curl http://localhost:8081/api/orders/customer/CUST-001
```

### Metrics (Prometheus)

```bash
curl http://localhost:8081/actuator/prometheus | grep saga
```

## Saga Flow Chi Tiết

```
Order PENDING
     │
     ├─[Step 1: process-payment]──────────────────────────────────────────┐
     │   Order → PAYMENT_PROCESSING                                        │
     │   paymentClient.processPayment(...)                                 │
     │   ✅ Success → tiếp tục                                             │
     │   ❌ Fail → Order CANCELLED (không cần compensate step trước)       │
     │                                                                      │
     ├─[Step 2: reserve-inventory]─────────────────────────────────────────┤
     │   Order → INVENTORY_RESERVING                                       │
     │   inventoryClient.reserveStock(...)                                 │
     │   ✅ Success → tiếp tục                                             │
     │   ❌ Fail → compensate step 1: paymentClient.refundPayment(...)     │
     │             Order CANCELLED                                          │
     │                                                                      │
     └─[All steps OK] → Order COMPLETED ◄────────────────────────────────-┘
```

## Cấu trúc code

```
spring-saga-orchestration/
├── docker-compose.yml
├── order-service/               # Orchestrator
│   └── src/main/java/.../
│       ├── saga/
│       │   ├── SagaStep.java        # Generic saga step
│       │   └── SagaExecutor.java    # Orchestrator engine
│       ├── monitoring/
│       │   ├── SagaStepMetric.java  # Custom annotation
│       │   └── SagaMetricsAspect.java # AOP metrics
│       ├── client/
│       │   ├── PaymentClient.java
│       │   └── InventoryClient.java
│       ├── service/OrderService.java # Saga builder
│       └── controller/OrderController.java
├── payment-service/             # Participant
│   └── src/main/java/.../
│       ├── domain/Payment.java
│       ├── service/PaymentService.java  # Idempotent
│       └── controller/PaymentController.java
└── inventory-service/           # Participant
    └── src/main/java/.../
        ├── domain/{Product,Reservation}.java
        ├── service/InventoryService.java  # Pessimistic lock
        └── controller/InventoryController.java
```

## Sản phẩm mặc định (auto-seed)

inventory-service tự seed 3 sản phẩm khi khởi động:

| ID | Tên | Số lượng |
|----|-----|----------|
| PROD-001 | Laptop Pro | 100 |
| PROD-002 | Wireless Mouse | 500 |
| PROD-003 | USB-C Hub | 200 |
