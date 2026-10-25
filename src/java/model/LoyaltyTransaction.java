package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Một dòng lịch sử biến động điểm (EARN / REDEEM / ADJUST). points có dấu. */
public class LoyaltyTransaction {
    private int txnId;
    private String txnType;
    private int points;
    private int balanceAfter;
    private BigDecimal billAmount;
    private BigDecimal moneyValue;
    private String note;
    private LocalDateTime createdAt;

    public int getTxnId() { return txnId; }
    public void setTxnId(int v) { this.txnId = v; }
    public String getTxnType() { return txnType; }
    public void setTxnType(String v) { this.txnType = v; }
    public int getPoints() { return points; }
    public void setPoints(int v) { this.points = v; }
    public int getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(int v) { this.balanceAfter = v; }
    public BigDecimal getBillAmount() { return billAmount; }
    public void setBillAmount(BigDecimal v) { this.billAmount = v; }
    public BigDecimal getMoneyValue() { return moneyValue; }
    public void setMoneyValue(BigDecimal v) { this.moneyValue = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    /** Cho JSP hiển thị tên loại. */
    public String getTypeLabel() {
        if ("EARN".equals(txnType)) return "Tích điểm";
        if ("REDEEM".equals(txnType)) return "Dùng điểm";
        return "Điều chỉnh";
    }
    /** java.time không tự format được trong JSTL fmt; cung cấp chuỗi sẵn. */
    public String getCreatedAtText() {
        return createdAt == null ? "" : createdAt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
