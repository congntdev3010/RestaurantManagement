package model;

/** Một bàn kèm thông tin còn trống hay không trong ngày + ca đang xét. */
public class TableAvailability {

    private final RestaurantTable table;
    private final boolean booked;
    private final boolean fits;

    public TableAvailability(RestaurantTable table, boolean booked, boolean fits) {
        this.table = table;
        this.booked = booked;
        this.fits = fits;
    }

    public RestaurantTable getTable() { return table; }

    /** Bàn đã được giữ cho đơn khác trong ngày + ca này. */
    public boolean isBooked() { return booked; }

    /** Sức chứa đủ cho số khách của đơn. */
    public boolean isFits() { return fits; }

    /** Có thể chọn để duyệt đơn: chưa ai giữ và đủ chỗ. */
    public boolean isSelectable() { return !booked && fits; }
}
