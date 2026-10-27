package model;

/** Một bàn kèm thông tin còn trống hay không trong ngày + ca đang xét. */
public class TableAvailability {

    private final RestaurantTable table;
    private final boolean booked;
    private final boolean fits;
    private final boolean serving;

    public TableAvailability(RestaurantTable table, boolean booked, boolean fits) {
        this(table, booked, fits, false);
    }

    /** serving = khách của đơn đã đến check-in trong ngày + ca này (bàn đang phục vụ). */
    public TableAvailability(RestaurantTable table, boolean booked, boolean fits, boolean serving) {
        this.table = table;
        this.booked = booked;
        this.fits = fits;
        this.serving = serving;
    }

    public RestaurantTable getTable() { return table; }

    /** Bàn đã được giữ cho đơn khác trong ngày + ca này. */
    public boolean isBooked() { return booked; }

    /** Sức chứa đủ cho số khách của đơn. */
    public boolean isFits() { return fits; }

    /** Đang phục vụ: khách đã đến đúng ngày + ca này. */
    public boolean isServing() { return serving; }

    /** Có thể chọn để duyệt đơn: chưa ai giữ. Bàn nhỏ hơn số khách vẫn chọn được để gộp bàn. */
    public boolean isSelectable() { return !booked && !serving; }
}
