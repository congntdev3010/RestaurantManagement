package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CustomerReview {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private int reviewId;
    private int historyId;
    private String customerName;
    private String tableName;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
    private String status;

    public int getReviewId() { return reviewId; }
    public void setReviewId(int reviewId) { this.reviewId = reviewId; }
    public int getHistoryId() { return historyId; }
    public void setHistoryId(int historyId) { this.historyId = historyId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedAtText() { return createdAt == null ? "" : createdAt.format(FORMAT); }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
