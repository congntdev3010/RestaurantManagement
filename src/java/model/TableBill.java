package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TableBill {
    private int tableId; private String tableName;
    private int orderCount;
    private int unfinishedItemCount;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private LocalDateTime reservationTime;
    private LocalDateTime checkedInAt;
    private String customerName;
    private String customerPhone;
    private BigDecimal cashReceived;
    private BigDecimal cashChange;
    private String paymentMethod;
    // Điểm tích lũy: khách được lễ tân gắn vào bill, điểm dùng, tiền giảm, số phải trả, điểm sẽ được cộng.
    private int loyaltyCustomerId;
    private String loyaltyCustomerName;
    private int loyaltyBalance;
    private int pointsToUse;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal payableAmount;
    private int pointsToEarn;
    private List<TableBillItem> items = new ArrayList<>();

    public int getTableId() { return tableId; } public String getTableName() { return tableName; } public void setTableName(String tableName) { this.tableName = tableName; }
    public void setTableId(int tableId) { this.tableId = tableId; }
    public int getOrderCount() { return orderCount; }
    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }
    public int getUnfinishedItemCount() { return unfinishedItemCount; }
    public void setUnfinishedItemCount(int unfinishedItemCount) { this.unfinishedItemCount = unfinishedItemCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public boolean isReadyForPayment() {
        return unfinishedItemCount == 0
                && orderCount > 0
                && totalAmount != null
                && totalAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    public LocalDateTime getReservationTime() { return reservationTime; }
    public void setReservationTime(LocalDateTime reservationTime) { this.reservationTime = reservationTime; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public List<TableBillItem> getItems() { return items; }
    public BigDecimal getCashReceived() { return cashReceived; }
    public void setCashReceived(BigDecimal cashReceived) { this.cashReceived = cashReceived; }
    public BigDecimal getCashChange() { return cashChange; }
    public void setCashChange(BigDecimal cashChange) { this.cashChange = cashChange; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public int getLoyaltyCustomerId() { return loyaltyCustomerId; }
    public void setLoyaltyCustomerId(int v) { this.loyaltyCustomerId = v; }
    public String getLoyaltyCustomerName() { return loyaltyCustomerName; }
    public void setLoyaltyCustomerName(String v) { this.loyaltyCustomerName = v; }
    public int getLoyaltyBalance() { return loyaltyBalance; }
    public void setLoyaltyBalance(int v) { this.loyaltyBalance = v; }
    public int getPointsToUse() { return pointsToUse; }
    public void setPointsToUse(int v) { this.pointsToUse = v; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal v) { this.discountAmount = v; }
    /** Số tiền khách thực trả = tổng bill - giảm giá điểm. */
    public BigDecimal getPayableAmount() { return payableAmount != null ? payableAmount : totalAmount; }
    public void setPayableAmount(BigDecimal v) { this.payableAmount = v; }
    public int getPointsToEarn() { return pointsToEarn; }
    public void setPointsToEarn(int v) { this.pointsToEarn = v; }
    public boolean isHasLoyaltyCustomer() { return loyaltyCustomerId > 0; }
}
