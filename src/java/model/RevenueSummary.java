package model;

import java.math.BigDecimal;

public class RevenueSummary {
    private BigDecimal today = BigDecimal.ZERO;
    private BigDecimal month = BigDecimal.ZERO;
    private BigDecimal year = BigDecimal.ZERO;

    public BigDecimal getToday() {
        return today;
    }

    public void setToday(BigDecimal today) {
        this.today = today;
    }

    public BigDecimal getMonth() {
        return month;
    }

    public void setMonth(BigDecimal month) {
        this.month = month;
    }

    public BigDecimal getYear() {
        return year;
    }

    public void setYear(BigDecimal year) {
        this.year = year;
    }
}
