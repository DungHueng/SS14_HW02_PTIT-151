# BÀI TẬP 2: ĐỒNG BỘ TRẠNG THÁI "ĐƠN HÀNG LẠC LỐI"

---

## 1. PHÂN TÍCH NGHIỆP VỤ & VỊ TRÍ CẬP NHẬT

### 1.1. Nguyên nhân sự cố
* **Nguyên nhân:** Khi Payment Service xử lý thanh toán thành công, phản hồi được gửi về nhưng gặp sự cố gián đoạn mạng (Network partition / Timeout) làm mất kết nối.
* **Hậu quả:** Tiền đã trừ thành công ở cổng thanh toán nhưng Order Service không nhận được event, khiến đơn hàng kẹt mãi ở trạng thái `PENDING`.

### 1.2. Vị trí cập nhật trạng thái
Để giải quyết triệt để, việc cập nhật trạng thái cần diễn ra ở **2 vị trí**:
1. **Vị trí 1 (Xử lý Bất đồng bộ):** Ngay tại `PaymentResponseListener` khi nhận được `PaymentResponseEvent` phản hồi từ Payment Service.
2. **Vị trí 2 (Xử lý Timeout / Bồi hoàn):** Tại một **Scheduled Job (Cronjob)** chạy định kỳ. Job này sẽ chủ động quét các đơn hàng `PENDING` bị kẹt quá 5 phút để đối soát hoặc tự động chuyển trạng thái.

---

## 2. SƠ ĐỒ TRẠNG THÁI (STATE MACHINE)

### 2.1. Sơ đồ dạng Text / ASCII Art

```text
       [Tạo đơn hàng]
              |
              v
       +--------------+  ---(Timeout quá hạn > 5 phút)---> +--------------+
       |   PENDING    |                                    |   CANCELED   |
       +--------------+                                    +--------------+
              |
              +--- (Payment SUCCESS) -----> +--------------+ ---> [Giao hàng / SHIPPED]
              |                             |     PAID     |
              |                             +--------------+
              |
              +--- (Payment REJECTED) ----\
              |                            +--> +--------------+
              +--- (Payment FAILED) ------/     |    FAILED    |
                                                +--------------+
```

### 2.2. Giải thích các điều kiện chuyển đổi trạng thái

| Trạng thái gốc | Trạng thái đích | Điều kiện kích hoạt (Trigger) | Hành động xử lý |
| :--- | :--- | :--- | :--- |
| **Bắt đầu** | `PENDING` | Khách hàng khởi tạo đơn hàng thành công. | Giữ hàng trong kho (Reserve stock). |
| `PENDING` | `PAID` | Nhận phản hồi Payment status = `"SUCCESS"`. | Xác nhận đơn hàng, gửi thông báo, chuẩn bị xuất kho. |
| `PENDING` | `CANCELED` | Nhận phản hồi status = `"REJECTED"` **HOẶC** Quá 5 phút không có phản hồi (Timeout). | Hoàn trả số lượng hàng đã giữ trong kho (Unreserve stock). |
| `PENDING` | `FAILED` | Nhận phản hồi status = `"FAILED"`. | Hoàn trả kho, báo lỗi thanh toán cho người dùng. |
| `PAID` | `SHIPPED` | Đã xuất kho và đơn vị vận chuyển lấy hàng thành công. | Cập nhật mã vận đơn, kết thúc chu trình mua hàng. |

---

## 3. SOURCE CODE HOÀN CHỈNH

### 3.1. Code đã sửa: Xử lý phản hồi từ Payment (`PaymentResponseListener.java`)

```java
@Component
public class PaymentResponseListener {

    private final OrderRepository orderRepository;

    public PaymentResponseListener(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @EventListener
    @Transactional
    public void handlePaymentResponse(PaymentResponseEvent event) {
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(event.getOrderId()));

        // Tránh ghi đè nếu đơn hàng đã được xử lý trước đó (Tính Idempotency)
        if (!"PENDING".equals(order.getStatus())) {
            return;
        }

        switch (event.getStatus()) {
            case "SUCCESS":
                order.setStatus("PAID");
                orderRepository.save(order);
                // TODO: Gửi thông báo thành công, chuyển thông tin sang kho hàng
                break;

            case "REJECTED":
                order.setStatus("CANCELED");
                orderRepository.save(order);
                // TODO: Thực hiện hoàn trả số lượng hàng trong kho (Unreserve)
                break;

            case "FAILED":
                order.setStatus("FAILED");
                orderRepository.save(order);
                // TODO: Thực hiện hoàn kho và thông báo thanh toán thất bại
                break;

            default:
                order.setStatus("UNKNOWN");
                orderRepository.save(order);
                break;
        }
    }
}
```

### 3.2. Code bổ sung: Xử lý Timeout đơn hàng quá hạn (`OrderTimeoutScheduler.java`)

```java
@Component
public class OrderTimeoutScheduler {

    private final OrderRepository orderRepository;

    public OrderTimeoutScheduler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Tự động quét định kỳ mỗi 1 phút
     * Tìm các đơn hàng PENDING đã tạo quá 5 phút để chuyển trạng thái
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processExpiredPendingOrders() {
        // Mốc thời gian: Hiện tại trừ đi 5 phút
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(5);

        // Lấy danh sách các đơn hàng PENDING quá hạn
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore("PENDING", timeoutThreshold);

        for (Order order : expiredOrders) {
            // Cập nhật đơn hàng bị timeout sang trạng thái CANCELED (hoặc FAILED)
            order.setStatus("CANCELED");
            orderRepository.save(order);

            // TODO: Gọi dịch vụ Kho (InventoryService) để hoàn lại số lượng hàng đã giữ
            // TODO: (Mở rộng) Gọi API re-check sang Payment Gateway trước khi hủy để tránh hủy nhầm đơn
        }
    }
}
```