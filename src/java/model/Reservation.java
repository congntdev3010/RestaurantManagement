package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Đơn đặt bàn của guest (userId = null) hoặc customer đã đăng nhập. */
public class Reservation {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private int reservationId;
    private Integer userId;
    private Integer tableId;
    private String tableName;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private int guestCount;
    private String status;
    private String note;
    private String tableTypeRequested;
    private String mealShift;
    private String staffNote;
    private String cancelReason;
    private LocalDateTime contactedAt;
    private LocalDateTime checkedInAt;
    private LocalDateTime createdAt;
    private String confirmedByName;

    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getTableId() { return tableId; }
    public void setTableId(Integer tableId) { this.tableId = tableId; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public LocalDate getReservationDate() { return reservationDate; }
    public void setReservationDate(LocalDate reservationDate) { this.reservationDate = reservationDate; }
    public LocalTime getReservationTime() { return reservationTime; }
    public void setReservationTime(LocalTime reservationTime) { this.reservationTime = reservationTime; }
    public int getGuestCount() { return guestCount; }
    public void setGuestCount(int guestCount) { this.guestCount = guestCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getTableTypeRequested() { return tableTypeRequested; }
    public void setTableTypeRequested(String tableTypeRequested) { this.tableTypeRequested = tableTypeRequested; }
    public String getMealShift() { return mealShift; }
    public void setMealShift(String mealShift) { this.mealShift = mealShift; }
    public String getStaffNote() { return staffNote; }
    public void setStaffNote(String staffNote) { this.staffNote = staffNote; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public LocalDateTime getContactedAt() { return contactedAt; }
    public void setContactedAt(LocalDateTime contactedAt) { this.contactedAt = contactedAt; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getConfirmedByName() { return confirmedByName; }
    public void setConfirmedByName(String confirmedByName) { this.confirmedByName = confirmedByName; }

    // Các getter định dạng sẵn để JSP hiển thị theo kiểu Việt Nam.
    public String getDateText() { return reservationDate == null ? "" : reservationDate.format(DATE_FMT); }
    public String getTimeText() { return reservationTime == null ? "" : reservationTime.format(TIME_FMT); }
    /** Ngày đến là hôm nay (giờ máy chủ). */
    public boolean isToday() { return reservationDate != null && reservationDate.equals(LocalDate.now()); }
    public String getCheckedInAtText() { return checkedInAt == null ? "" : checkedInAt.format(DATETIME_FMT); }
    public String getCreatedAtText() { return createdAt == null ? "" : createdAt.format(DATETIME_FMT); }
    public String getContactedAtText() { return contactedAt == null ? "" : contactedAt.format(DATETIME_FMT); }

    /** Tên ca tiếng Việt cho view. */
    public String getMealShiftText() {
        if ("LUNCH".equals(mealShift)) return "Ca trưa";
        if ("DINNER".equals(mealShift)) return "Ca tối";
        return "";
    }

    /** Nhãn trạng thái tiếng Việt cho view. */
    public String getStatusText() {
        if (status == null) return "";
        switch (status) {
            case "PENDING": return "Chờ xác nhận";
            case "APPROVED": return "Đã duyệt";
            case "REJECTED": return "Từ chối";
            case "CANCELLED": return "Đã huỷ";
            case "COMPLETED": return "Đã hoàn thành";
            case "NO_SHOW": return "Khách không đến";
            default: return status;
        }
    }
}
