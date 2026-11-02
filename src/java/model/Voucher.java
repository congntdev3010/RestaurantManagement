package model;

import java.math.BigDecimal;

/** Một mã voucher đã phát cho khách (dòng Vouchers). */
public class Voucher {

    private int voucherId;
    private String code;
    private BigDecimal discountPercent = BigDecimal.ZERO;
    private String customerName;
    private String customerEmail;

    public int getVoucherId() { return voucherId; }
    public void setVoucherId(int voucherId) { this.voucherId = voucherId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
}
