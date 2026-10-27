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
