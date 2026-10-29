package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Một đơn đã thanh toán (dòng ServiceHistory), dùng cho màn lịch sử thanh toán của Manager. */
public class PaidBill {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int historyId;
    private String tableName;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    /** Khách thành viên (đã gắn tài khoản khi tích điểm); 0 = khách vãng lai. */
    private int loyaltyCustomerId;
    private LocalDateTime paidAt;
    private String paymentMethod;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private int pointsUsed;
    private int pointsEarned;
    private String paidByName;
    private BigDecimal cashReceived;
    private BigDecimal cashChange;
    private List<TableBillItem> items = new ArrayList<>();

    public int getHistoryId() { return historyId; }
    public void setHistoryId(int historyId) { this.historyId = historyId; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    /** SĐT đã che cho lễ tân: giữ 3 số đầu và 3 số cuối. */
    public String getMaskedPhone() {
        if (customerPhone == null) return null;
        if (customerPhone.length() < 7) return "***";
        return customerPhone.substring(0, 3) + "****" + customerPhone.substring(customerPhone.length() - 3);
    }
    /** Gmail đã che cho lễ tân: giữ 2 ký tự đầu của tên, giữ nguyên tên miền. */
    public String getMaskedEmail() {
        if (customerEmail == null) return null;
        int at = customerEmail.indexOf('@');
        if (at <= 0) return "***";
        String local = customerEmail.substring(0, at);
        String keep = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return keep + "***" + customerEmail.substring(at);
    }
    public int getLoyaltyCustomerId() { return loyaltyCustomerId; }
    public void setLoyaltyCustomerId(int loyaltyCustomerId) { this.loyaltyCustomerId = loyaltyCustomerId; }
    public boolean isMember() { return loyaltyCustomerId > 0; }
    /** Nhãn loại khách hiển thị trên màn lịch sử và hóa đơn. */
    public String getMemberTypeLabel() { return isMember() ? "Thành viên" : "Vãng lai"; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getPaidAtText() { return paidAt == null ? "" : paidAt.format(FORMAT); }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    /** Khách thực trả = tổng bill - giảm giá điểm. */
    public BigDecimal getPayableAmount() { return totalAmount.subtract(discountAmount); }
    public int getPointsUsed() { return pointsUsed; }
    public void setPointsUsed(int pointsUsed) { this.pointsUsed = pointsUsed; }
    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }
    public String getPaidByName() { return paidByName; }
    public void setPaidByName(String paidByName) { this.paidByName = paidByName; }
    public BigDecimal getCashReceived() { return cashReceived; }
    public void setCashReceived(BigDecimal cashReceived) { this.cashReceived = cashReceived; }
    public BigDecimal getCashChange() { return cashChange; }
    public void setCashChange(BigDecimal cashChange) { this.cashChange = cashChange; }
    public List<TableBillItem> getItems() { return items; }
}
