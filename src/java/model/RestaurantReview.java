package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RestaurantReview {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private int reviewId, historyId, customerId, rating;
    private String reviewText, status, customerName, tableName;
    private LocalDateTime createdAt;

    public int getReviewId() { return reviewId; }
    public void setReviewId(int v) { reviewId = v; }
    public int getHistoryId() { return historyId; }
    public void setHistoryId(int v) { historyId = v; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int v) { customerId = v; }
    public int getRating() { return rating; }
    public void setRating(int v) { rating = v; }
    public String getReviewText() { return reviewText; }
    public void setReviewText(String v) { reviewText = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String v) { customerName = v; }
    public String getTableName() { return tableName; }
    public void setTableName(String v) { tableName = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    public String getCreatedAtText() { return createdAt == null ? "" : createdAt.format(FORMAT); }
}
