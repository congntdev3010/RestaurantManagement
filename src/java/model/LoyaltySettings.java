package model;

import java.math.BigDecimal;

/** Quy tắc quy đổi điểm: earnSpendAmount đồng -> earnPoints điểm; 1 điểm -> redeemValuePerPoint đồng. */
public class LoyaltySettings {
    private BigDecimal earnSpendAmount = new BigDecimal("1000");
    private int earnPoints = 1;
    private BigDecimal redeemValuePerPoint = new BigDecimal("1000");

    public BigDecimal getEarnSpendAmount() { return earnSpendAmount; }
    public void setEarnSpendAmount(BigDecimal v) { this.earnSpendAmount = v; }
    public int getEarnPoints() { return earnPoints; }
    public void setEarnPoints(int v) { this.earnPoints = v; }
    public BigDecimal getRedeemValuePerPoint() { return redeemValuePerPoint; }
    public void setRedeemValuePerPoint(BigDecimal v) { this.redeemValuePerPoint = v; }

    /** Điểm được cộng cho số tiền thực trả. */
    public int pointsFor(BigDecimal paid) {
        if (paid == null || paid.signum() <= 0 || earnSpendAmount.signum() <= 0) return 0;
        return paid.divideToIntegralValue(earnSpendAmount).intValue() * earnPoints;
    }
}
