package dao;

import dal.DBContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.News;

public class NewsDAO extends DBContext {
    private static final String COLUMNS = "n.news_id, n.title, n.summary, n.content, n.image_url, "
            + "n.news_status, n.created_by, u.full_name AS created_by_name, n.created_at, n.updated_at, n.published_at";

    public List<News> getPublished(String keyword, int page, int pageSize) throws SQLException {
        requireConnection();
        List<News> result = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM dbo.News n JOIN dbo.Users u ON u.user_id=n.created_by "
                + "WHERE n.news_status='PUBLISHED' AND (? = N'' OR n.title LIKE ?) "
                + "ORDER BY n.published_at DESC, n.news_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String query = keyword == null ? "" : keyword.trim();
            ps.setNString(1, query);
            ps.setNString(2, "%" + query + "%");
            ps.setInt(3, Math.max(0, (page - 1) * pageSize));
            ps.setInt(4, pageSize);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(read(rs)); }
        }
        return result;
    }

    public int countPublished(String keyword) throws SQLException {
        requireConnection();
        String sql = "SELECT COUNT(*) FROM dbo.News WHERE news_status='PUBLISHED' AND (? = N'' OR title LIKE ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String query = keyword == null ? "" : keyword.trim();
            ps.setNString(1, query); ps.setNString(2, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    public News getPublishedById(int newsId) throws SQLException {
        requireConnection();
        String sql = "SELECT " + COLUMNS + " FROM dbo.News n JOIN dbo.Users u ON u.user_id=n.created_by "
                + "WHERE n.news_id=? AND n.news_status='PUBLISHED'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, newsId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? read(rs) : null; }
        }
    }

    public List<News> getAdminNews(String keyword, String status, int page, int pageSize) throws SQLException {
        requireConnection();
        List<News> result = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM dbo.News n JOIN dbo.Users u ON u.user_id=n.created_by "
                + "WHERE (?=N'' OR n.title LIKE ?) AND (?='' OR n.news_status=?) "
                + "ORDER BY n.created_at DESC, n.news_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String query = keyword == null ? "" : keyword.trim();
            String state = status == null ? "" : status;
            ps.setNString(1, query); ps.setNString(2, "%" + query + "%");
            ps.setString(3, state); ps.setString(4, state);
            ps.setInt(5, Math.max(0, (page - 1) * pageSize)); ps.setInt(6, pageSize);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(read(rs)); }
        }
        return result;
    }

    public int countAdminNews(String keyword, String status) throws SQLException {
        requireConnection();
        String sql = "SELECT COUNT(*) FROM dbo.News WHERE (?=N'' OR title LIKE ?) AND (?='' OR news_status=?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String query = keyword == null ? "" : keyword.trim();
            String state = status == null ? "" : status;
            ps.setNString(1, query); ps.setNString(2, "%" + query + "%");
            ps.setString(3, state); ps.setString(4, state);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    public News getById(int newsId) throws SQLException {
        requireConnection();
        String sql = "SELECT " + COLUMNS + " FROM dbo.News n JOIN dbo.Users u ON u.user_id=n.created_by WHERE n.news_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, newsId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? read(rs) : null; }
        }
    }

    public boolean create(News news) throws SQLException {
        requireConnection();
        String sql = "INSERT INTO dbo.News (title,summary,content,image_url,news_status,created_by,published_at) "
                + "VALUES (?,?,?,?,?,?,CASE WHEN ?='PUBLISHED' THEN SYSDATETIME() ELSE NULL END)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setNString(1, news.getTitle()); ps.setNString(2, news.getSummary()); ps.setNString(3, news.getContent());
            ps.setNString(4, news.getImageUrl()); ps.setString(5, news.getStatus()); ps.setInt(6, news.getCreatedBy());
            ps.setString(7, news.getStatus()); return ps.executeUpdate() == 1;
        }
    }

    public boolean update(News news) throws SQLException {
        requireConnection();
        String sql = "UPDATE dbo.News SET title=?, summary=?, content=?, image_url=?, news_status=?, updated_at=SYSDATETIME(), "
                + "published_at=CASE WHEN ?='PUBLISHED' THEN COALESCE(published_at,SYSDATETIME()) ELSE NULL END "
                + "WHERE news_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setNString(1, news.getTitle()); ps.setNString(2, news.getSummary()); ps.setNString(3, news.getContent());
            ps.setNString(4, news.getImageUrl()); ps.setString(5, news.getStatus()); ps.setString(6, news.getStatus());
            ps.setInt(7, news.getNewsId()); return ps.executeUpdate() == 1;
        }
    }

    public boolean delete(int newsId) throws SQLException {
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM dbo.News WHERE news_id=?")) {
            ps.setInt(1, newsId); return ps.executeUpdate() == 1;
        }
    }

    public boolean setStatus(int newsId, String status) throws SQLException {
        requireConnection();
        String sql = "UPDATE dbo.News SET news_status=?, updated_at=SYSDATETIME(), "
                + "published_at=CASE WHEN ?='PUBLISHED' THEN COALESCE(published_at,SYSDATETIME()) ELSE NULL END "
                + "WHERE news_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status); ps.setString(2, status); ps.setInt(3, newsId);
            return ps.executeUpdate() == 1;
        }
    }

    private News read(ResultSet rs) throws SQLException {
        News news = new News();
        news.setNewsId(rs.getInt("news_id")); news.setTitle(rs.getNString("title"));
        news.setSummary(rs.getNString("summary")); news.setContent(rs.getNString("content"));
        news.setImageUrl(rs.getNString("image_url")); news.setStatus(rs.getString("news_status"));
        news.setCreatedBy(rs.getInt("created_by")); news.setCreatedByName(rs.getNString("created_by_name"));
        news.setCreatedAt(rs.getTimestamp("created_at")); news.setUpdatedAt(rs.getTimestamp("updated_at"));
        news.setPublishedAt(rs.getTimestamp("published_at")); return news;
    }

    private void requireConnection() throws SQLException {
        if (connection == null || connection.isClosed()) throw new SQLException("Database connection is unavailable.");
    }
}
