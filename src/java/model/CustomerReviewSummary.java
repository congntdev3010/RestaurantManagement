package model;

public class CustomerReviewSummary {
    private int totalCount;
    private int pendingCount;
    private int approvedCount;
    private int rejectedCount;
    private double averageRating;
    private int oneStarCount;
    private int twoStarCount;
    private int threeStarCount;
    private int fourStarCount;
    private int fiveStarCount;

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }
    public int getApprovedCount() { return approvedCount; }
    public void setApprovedCount(int approvedCount) { this.approvedCount = approvedCount; }
    public int getRejectedCount() { return rejectedCount; }
    public void setRejectedCount(int rejectedCount) { this.rejectedCount = rejectedCount; }
    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public int getOneStarCount() { return oneStarCount; }
    public void setOneStarCount(int oneStarCount) { this.oneStarCount = oneStarCount; }
    public int getTwoStarCount() { return twoStarCount; }
    public void setTwoStarCount(int twoStarCount) { this.twoStarCount = twoStarCount; }
    public int getThreeStarCount() { return threeStarCount; }
    public void setThreeStarCount(int threeStarCount) { this.threeStarCount = threeStarCount; }
    public int getFourStarCount() { return fourStarCount; }
    public void setFourStarCount(int fourStarCount) { this.fourStarCount = fourStarCount; }
    public int getFiveStarCount() { return fiveStarCount; }
    public void setFiveStarCount(int fiveStarCount) { this.fiveStarCount = fiveStarCount; }
}
