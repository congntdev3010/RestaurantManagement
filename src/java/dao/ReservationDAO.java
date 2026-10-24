package dao;

import dal.DBContext;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import model.Reservation;

/** Truy cập dữ liệu đặt bàn. Dùng try-with-resources vì DBContext là AutoCloseable. */
public class ReservationDAO extends DBContext {

    public static final String OK = "OK";
    public static final String NOT_PENDING = "NOT_PENDING";
    public static final String NO_TABLE = "NO_TABLE";
    public static final String CAPACITY = "CAPACITY";
    public static final String CONFLICT = "CONFLICT";
    public static final String ERROR = "ERROR";
    public static final String NOT_TODAY = "NOT_TODAY";
    public static final String TABLE_BUSY = "TABLE_BUSY";
    public static final String ALREADY_IN = "ALREADY_IN";

    private static final String SELECT_BASE
            = "SELECT r.reservation_id, r.user_id, r.table_id, r.customer_name, r.customer_email, r.customer_phone, "
            + "r.reservation_date, r.reservation_time, r.guest_count, r.reservation_status, r.note, "
            + "r.table_type_requested, r.meal_shift, r.staff_note, r.cancel_reason, r.contacted_at, r.checked_in_at, r.created_at, "
            + "t.table_name + ISNULL((SELECT ', ' + t2.table_name FROM ReservationTables rt "
            + "JOIN RestaurantTables t2 ON t2.table_id = rt.table_id WHERE rt.reservation_id = r.reservation_id "
            + "ORDER BY t2.table_name FOR XML PATH(''), TYPE).value('.', 'NVARCHAR(MAX)'), N'') AS table_name, "
            + "u.full_name AS confirmed_name "
            + "FROM Reservations r "
            + "LEFT JOIN RestaurantTables t ON t.table_id = r.table_id "
            + "LEFT JOIN Users u ON u.user_id = r.confirmed_by ";

    /** Tạo đơn mới ở trạng thái PENDING; trả về id hoặc -1 nếu lỗi. */
    public int createReservation(Reservation r) {
        if (connection == null) return -1;
        String sql = "INSERT INTO Reservations (user_id, customer_name, customer_email, customer_phone, "
                + "reservation_date, reservation_time, guest_count, reservation_status, note, "
                + "table_type_requested, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, ?, SYSDATETIME())";
        try (PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            if (r.getUserId() == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, r.getUserId());
            ps.setNString(2, r.getCustomerName());
            ps.setNString(3, r.getCustomerEmail());
            ps.setString(4, r.getCustomerPhone());
            ps.setDate(5, Date.valueOf(r.getReservationDate()));
            ps.setTime(6, Time.valueOf(r.getReservationTime()));
            ps.setInt(7, r.getGuestCount());
            ps.setNString(8, r.getNote());
            ps.setNString(9, r.getTableTypeRequested());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Lễ tân đặt hộ khách (khách gọi điện): tạo đơn rồi duyệt luôn với các bàn đã chọn.
     * Nếu duyệt thất bại (bàn bị giữ, thiếu chỗ...) thì xoá đơn vừa tạo để không để lại đơn treo.
     *
     * @return {@link #OK} hoặc mã lỗi của {@link #approve}; ERROR nếu không tạo được đơn
     */
    public String createApproved(Reservation r, Collection<Integer> tableIds, String shift, int staffId) {
        int id = createReservation(r);
        if (id < 1) return ERROR;
        String result = approve(id, tableIds, shift, staffId, "Lễ tân đặt hộ qua điện thoại");
        if (!OK.equals(result)) {
            update("DELETE FROM Reservations WHERE reservation_id = ?", ps -> ps.setInt(1, id));
        }
        return result;
    }

    /** Số đơn PENDING của một số điện thoại, dùng để chặn spam từ guest. */
    public int countPendingByPhone(String phone) {
        if (connection == null) return 0;
        String sql = "SELECT COUNT(*) FROM Reservations WHERE customer_phone = ? AND reservation_status = 'PENDING'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /** Các loại bàn hiện có, dùng cho ô chọn loại bàn trong form. */
    public List<String> getTableTypes() {
        List<String> types = new ArrayList<>();
        if (connection == null) return types;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT DISTINCT table_type FROM RestaurantTables ORDER BY table_type");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) types.add(rs.getNString(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return types;
    }

    /** Khung giờ từng ca: mã ca -> {giờ bắt đầu, giờ kết thúc}. */
    public Map<String, LocalTime[]> getShifts() {
        Map<String, LocalTime[]> shifts = new LinkedHashMap<>();
        if (connection == null) return shifts;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT shift_code, start_time, end_time FROM MealShifts ORDER BY start_time");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                shifts.put(rs.getString(1),
                        new LocalTime[]{rs.getTime(2).toLocalTime(), rs.getTime(3).toLocalTime()});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return shifts;
    }

    public Reservation getById(int id) {
        if (connection == null) return null;
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BASE + "WHERE r.reservation_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? read(rs) : null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Lịch sử đặt bàn của customer, mới nhất trước. */
    public List<Reservation> getByUser(int userId) {
        List<Reservation> list = new ArrayList<>();
        if (connection == null) return list;
        try (PreparedStatement ps = connection.prepareStatement(
                SELECT_BASE + "WHERE r.user_id = ? ORDER BY r.created_at DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(read(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Danh sách cho lễ tân; status/date null nghĩa là không lọc. Tối đa 200 dòng. */
    public List<Reservation> search(String status, LocalDate date) {
        List<Reservation> list = new ArrayList<>();
        if (connection == null) return list;
        StringBuilder sql = new StringBuilder("SELECT TOP 200 ")
                .append(SELECT_BASE.substring("SELECT ".length())).append("WHERE 1 = 1 ");
        if (status != null) sql.append("AND r.reservation_status = ? ");
        if (date != null) sql.append("AND r.reservation_date = ? ");
        boolean upcoming = "PENDING".equals(status) || "APPROVED".equals(status);
        sql.append(upcoming ? "ORDER BY r.reservation_date, r.reservation_time"
                : "ORDER BY r.reservation_date DESC, r.reservation_time DESC");
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int i = 1;
            if (status != null) ps.setString(i++, status);
            if (date != null) ps.setDate(i, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(read(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Số đơn theo từng trạng thái để hiện trên các tab. */
    public Map<String, Integer> countByStatus() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (connection == null) return counts;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT reservation_status, COUNT(*) FROM Reservations GROUP BY reservation_status");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) counts.put(rs.getString(1), rs.getInt(2));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return counts;
    }

    /** Id các bàn đã bị giữ trong ngày + ca (đơn APPROVED/COMPLETED). */
    public Set<Integer> getBookedTableIds(LocalDate date, String shift) {
        Set<Integer> ids = new HashSet<>();
        if (connection == null || shift == null) return ids;
        String sql = "SELECT table_id FROM Reservations WHERE reservation_date = ? AND meal_shift = ? "
                + "AND reservation_status IN ('APPROVED', 'COMPLETED') AND table_id IS NOT NULL "
                // Gồm cả các bàn ghép thêm của đơn (ReservationTables), không chỉ bàn chính.
                + "UNION SELECT x.table_id FROM ReservationTables x JOIN Reservations r ON r.reservation_id = x.reservation_id "
                + "WHERE r.reservation_date = ? AND r.meal_shift = ? AND r.reservation_status IN ('APPROVED', 'COMPLETED')";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setString(2, shift);
            ps.setDate(3, Date.valueOf(date));
            ps.setString(4, shift);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    /** Lễ tân đã gọi khách: ghi thời điểm gọi và ghi chú kết quả cuộc gọi. */
    public boolean markContacted(int id, String staffNote) {
        return update("UPDATE Reservations SET contacted_at = SYSDATETIME(), staff_note = ?, "
                + "updated_at = SYSDATETIME() WHERE reservation_id = ? AND reservation_status = 'PENDING'",
                ps -> { ps.setNString(1, staffNote); ps.setInt(2, id); });
    }

    /**
     * Duyệt đơn: gán bàn + ca. Kiểm tra trong một transaction; chỉ mục unique ở DB là chốt chặn
     * cuối cùng nếu hai lễ tân cùng chọn một bàn.
     */
    public String approve(int id, Collection<Integer> tableIdList, String shift, int staffId, String staffNote) {
        if (connection == null) return ERROR;
        List<Integer> tableIds = new ArrayList<>(new LinkedHashSet<>(tableIdList));
        if (tableIds.isEmpty()) return NO_TABLE;
        try {
            connection.setAutoCommit(false);
            int guests;
            Date date;
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT guest_count, reservation_status, reservation_date FROM Reservations WITH (UPDLOCK, ROWLOCK) "
                    + "WHERE reservation_id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !"PENDING".equals(rs.getString(2))) {
                        connection.rollback();
                        return NOT_PENDING;
                    }
                    guests = rs.getInt(1);
                    date = rs.getDate(3);
                }
            }
            // Gộp bàn: tổng sức chứa các bàn được chọn phải đủ cho số khách.
            int totalCapacity = 0;
            for (int tid : tableIds) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT capacity FROM RestaurantTables WHERE table_id = ?")) {
                    ps.setInt(1, tid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            connection.rollback();
                            return NO_TABLE;
                        }
                        totalCapacity += rs.getInt(1);
                    }
                }
                // Bàn không được nằm trong đơn khác (bàn chính hoặc bàn ghép) cùng ngày + ca.
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT 1 FROM Reservations r WITH (UPDLOCK, HOLDLOCK) WHERE r.reservation_id <> ? "
                        + "AND r.reservation_date = ? AND r.meal_shift = ? AND r.reservation_status IN ('APPROVED', 'COMPLETED') "
                        + "AND (r.table_id = ? OR EXISTS (SELECT 1 FROM ReservationTables x "
                        + "WHERE x.reservation_id = r.reservation_id AND x.table_id = ?))")) {
                    ps.setInt(1, id);
                    ps.setDate(2, date);
                    ps.setString(3, shift);
                    ps.setInt(4, tid);
                    ps.setInt(5, tid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            connection.rollback();
                            return CONFLICT;
                        }
                    }
                }
            }
            if (totalCapacity < guests) {
                connection.rollback();
                return CAPACITY;
            }
            int tableId = tableIds.get(0);
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE Reservations SET reservation_status = 'APPROVED', table_id = ?, meal_shift = ?, "
                    + "confirmed_by = ?, confirmed_at = SYSDATETIME(), updated_at = SYSDATETIME(), "
                    + "staff_note = COALESCE(?, staff_note) WHERE reservation_id = ?")) {
                ps.setInt(1, tableId);
                ps.setString(2, shift);
                ps.setInt(3, staffId);
                ps.setNString(4, staffNote);
                ps.setInt(5, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM ReservationTables WHERE reservation_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            for (int i = 1; i < tableIds.size(); i++) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO ReservationTables (reservation_id, table_id) VALUES (?, ?)")) {
                    ps.setInt(1, id);
                    ps.setInt(2, tableIds.get(i));
                    ps.executeUpdate();
                }
            }
            connection.commit();
            return OK;
        } catch (SQLException e) {
            rollbackQuietly();
            // 2601/2627: vi phạm unique index UX_Reservations_TableDateShift -> bàn vừa bị giữ.
            if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) return CONFLICT;
            e.printStackTrace();
            return ERROR;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * Danh sách đơn để check-in: đã duyệt, khách chưa đến. Không có từ khoá thì chỉ lấy đơn HÔM NAY;
     * có từ khoá (tên hoặc số điện thoại) thì tìm cả đơn từ hôm nay trở đi.
     */
    public List<Reservation> searchForCheckin(String keyword) {
        List<Reservation> list = new ArrayList<>();
        if (connection == null) return list;
        boolean hasKeyword = keyword != null && !keyword.isEmpty();
        String sql = SELECT_BASE + "WHERE r.reservation_status = 'APPROVED' AND r.checked_in_at IS NULL "
                + (hasKeyword
                        ? "AND r.reservation_date >= CAST(SYSDATETIME() AS DATE) "
                        + "AND (r.customer_name LIKE ? OR r.customer_phone LIKE ?) "
                        : "AND r.reservation_date = CAST(SYSDATETIME() AS DATE) ")
                + "ORDER BY r.reservation_date, r.reservation_time";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (hasKeyword) {
                String like = "%" + keyword.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]") + "%";
                ps.setNString(1, like);
                ps.setString(2, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(read(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Các đơn hôm nay đã duyệt nhưng khách chưa đến, để cảnh báo bàn đã có người đặt. */
    public List<Reservation> getTodayArrivals() {
        return searchForCheckin(null);
    }

    /**
     * Check-in: khách đã đến. Ghi giờ đến, chuyển MỌI bàn của đơn (kể cả bàn ghép) sang OCCUPIED và
     * đặt đơn thành COMPLETED trong cùng một transaction.
     * Chỉ check-in được đơn APPROVED của HÔM NAY; có bàn đang có khách thì từ chối.
     */
    public String checkIn(int id, int staffId) {
        if (connection == null) return ERROR;
        try {
            connection.setAutoCommit(false);
            Integer tableId;
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT table_id, reservation_status, checked_in_at, "
                    + "CASE WHEN reservation_date = CAST(SYSDATETIME() AS DATE) THEN 1 ELSE 0 END AS is_today "
                    + "FROM Reservations WITH (UPDLOCK, ROWLOCK) WHERE reservation_id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !"APPROVED".equals(rs.getString("reservation_status"))) {
                        connection.rollback();
                        return NOT_PENDING;
                    }
                    if (rs.getTimestamp("checked_in_at") != null) {
                        connection.rollback();
                        return ALREADY_IN;
                    }
                    if (rs.getInt("is_today") != 1) {
                        connection.rollback();
                        return NOT_TODAY;
                    }
                    int t = rs.getInt("table_id");
                    tableId = rs.wasNull() ? null : t;
                }
            }
            if (tableId == null) {
                connection.rollback();
                return NO_TABLE;
            }
            List<Integer> allTables = new ArrayList<>();
            allTables.add(tableId);
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT table_id FROM ReservationTables WHERE reservation_id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) allTables.add(rs.getInt(1));
                }
            }
            for (int tid : allTables) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE RestaurantTables SET table_status = 'OCCUPIED' "
                        + "WHERE table_id = ? AND table_status IN ('AVAILABLE', 'RESERVED')")) {
                    ps.setInt(1, tid);
                    if (ps.executeUpdate() == 0) {
                        connection.rollback();
                        return TABLE_BUSY;
                    }
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE Reservations SET reservation_status = 'COMPLETED', checked_in_at = SYSDATETIME(), checked_in_by = ?, "
                    + "updated_at = SYSDATETIME() WHERE reservation_id = ?")) {
                ps.setInt(1, staffId);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
            connection.commit();
            return OK;
        } catch (SQLException e) {
            rollbackQuietly();
            e.printStackTrace();
            return ERROR;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * Tự huỷ đơn đã duyệt mà khách chưa check-in sau khi quá giờ hẹn {@code graceMinutes} phút.
     * Chuyển sang CANCELLED nên bàn được nhả lại (chỉ mục chống trùng chỉ tính APPROVED/COMPLETED).
     *
     * @return số đơn đã huỷ
     */
    public int cancelLateArrivals(int graceMinutes) {
        if (connection == null) return 0;
        String sql = "UPDATE Reservations SET reservation_status = 'CANCELLED', "
                + "cancel_reason = N'Tự động huỷ: khách đến muộn quá " + graceMinutes + " phút', "
                + "updated_at = SYSDATETIME() "
                + "WHERE reservation_status = 'APPROVED' AND checked_in_at IS NULL "
                + "AND DATEADD(MINUTE, ?, DATEADD(SECOND, DATEDIFF(SECOND, 0, reservation_time), "
                + "CAST(reservation_date AS DATETIME2(0)))) < SYSDATETIME()";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, graceMinutes);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /** Lễ tân từ chối đơn đang chờ. */
    public boolean reject(int id, int staffId, String reason) {
        return update("UPDATE Reservations SET reservation_status = 'REJECTED', cancel_reason = ?, "
                + "confirmed_by = ?, confirmed_at = SYSDATETIME(), updated_at = SYSDATETIME() "
                + "WHERE reservation_id = ? AND reservation_status = 'PENDING'",
                ps -> { ps.setNString(1, reason); ps.setInt(2, staffId); ps.setInt(3, id); });
    }

    /**
     * Chuyển đơn đã duyệt sang COMPLETED (chỉ khi khách đã check-in) hoặc NO_SHOW / CANCELLED
     * (chỉ khi khách chưa check-in).
     */
    public boolean finishApproved(int id, String newStatus, String reason) {
        String arrived = "COMPLETED".equals(newStatus) ? "IS NOT NULL" : "IS NULL";
        return update("UPDATE Reservations SET reservation_status = ?, cancel_reason = COALESCE(?, cancel_reason), "
                + "updated_at = SYSDATETIME() WHERE reservation_id = ? AND reservation_status = 'APPROVED' "
                + "AND checked_in_at " + arrived,
                ps -> { ps.setString(1, newStatus); ps.setNString(2, reason); ps.setInt(3, id); });
    }

    /** Customer tự huỷ đơn của mình (đang chờ hoặc đã duyệt). */
    public boolean cancelByUser(int id, int userId) {
        return update("UPDATE Reservations SET reservation_status = 'CANCELLED', cancel_reason = N'Khách tự huỷ', "
                + "updated_at = SYSDATETIME() WHERE reservation_id = ? AND user_id = ? "
                + "AND reservation_status IN ('PENDING', 'APPROVED')",
                ps -> { ps.setInt(1, id); ps.setInt(2, userId); });
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private boolean update(String sql, Binder binder) {
        if (connection == null) return false;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Reservation read(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setReservationId(rs.getInt("reservation_id"));
        int userId = rs.getInt("user_id");
        r.setUserId(rs.wasNull() ? null : userId);
        int tableId = rs.getInt("table_id");
        r.setTableId(rs.wasNull() ? null : tableId);
        r.setTableName(rs.getNString("table_name"));
        r.setCustomerName(rs.getNString("customer_name"));
        r.setCustomerEmail(rs.getNString("customer_email"));
        r.setCustomerPhone(rs.getString("customer_phone"));
        r.setReservationDate(rs.getDate("reservation_date").toLocalDate());
        r.setReservationTime(rs.getTime("reservation_time").toLocalTime());
        r.setGuestCount(rs.getInt("guest_count"));
        r.setStatus(rs.getString("reservation_status"));
        r.setNote(rs.getNString("note"));
        r.setTableTypeRequested(rs.getNString("table_type_requested"));
        r.setMealShift(rs.getString("meal_shift"));
        r.setStaffNote(rs.getNString("staff_note"));
        r.setCancelReason(rs.getNString("cancel_reason"));
        Timestamp contacted = rs.getTimestamp("contacted_at");
        r.setContactedAt(contacted == null ? null : contacted.toLocalDateTime());
        Timestamp checkedIn = rs.getTimestamp("checked_in_at");
        r.setCheckedInAt(checkedIn == null ? null : checkedIn.toLocalDateTime());
        Timestamp created = rs.getTimestamp("created_at");
        r.setCreatedAt(created == null ? null : created.toLocalDateTime());
        r.setConfirmedByName(rs.getNString("confirmed_name"));
        return r;
    }
}
