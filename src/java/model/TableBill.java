package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TableBill {
    private int tableId;
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
    private List<TableBillItem> items = new ArrayList<>();

    public int getTableId() { return tableId; }
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
}
