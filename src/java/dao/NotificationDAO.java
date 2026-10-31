package dao;

import dal.DBContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.Notification;

/** Thông báo trong app: đếm chưa đọc, lấy danh sách mới nhất, đánh dấu đã đọc. */
public class NotificationDAO extends DBContext {

    public int countUnread(int userId) throws SQLException {
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM Notifications WHERE user_id = ? AND is_read = 0")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Thông báo mới nhất trước, tối đa limit dòng. */
    public List<Notification> listRecent(int userId, int limit) throws SQLException {
        requireConnection();
        List<Notification> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT TOP (?) notification_id, title, body, is_read, created_at "
                + "FROM Notifications WHERE user_id = ? ORDER BY created_at DESC, notification_id DESC")) {
            ps.setInt(1, limit);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Notification n = new Notification();
                    n.setNotificationId(rs.getInt("notification_id"));
                    n.setTitle(rs.getNString("title"));
                    n.setBody(rs.getNString("body"));
                    n.setRead(rs.getBoolean("is_read"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    n.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
                    list.add(n);
                }
            }
        }
        return list;
    }

    public void markAllRead(int userId) throws SQLException {
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE Notifications SET is_read = 1, read_at = SYSDATETIME() WHERE user_id = ? AND is_read = 0")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private void requireConnection() throws SQLException {
        if (connection == null) {
            throw new SQLException("Không kết nối được CSDL.");
        }
    }
}
