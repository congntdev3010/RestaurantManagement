package model;

/** Số bàn còn trống / tổng số bàn phù hợp trong một ca của ngày khách đặt. */
public class ShiftAvailability {

    private final String code;
    private final String name;
    private final int free;
    private final int total;

    public ShiftAvailability(String code, String name, int free, int total) {
        this.code = code;
        this.name = name;
        this.free = free;
        this.total = total;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public int getFree() { return free; }
    public int getTotal() { return total; }
}
