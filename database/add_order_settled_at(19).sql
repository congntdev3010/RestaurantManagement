-- Đánh dấu order HỦY đã được chốt khi bàn thanh toán (settled_at).
-- Order hủy đã chốt vẫn giữ trong DB nhưng bị ẩn khỏi danh sách vận hành "Đã hủy".
-- Chạy một lần trên CSDL AlacarteRestaurantDB.
IF COL_LENGTH('dbo.Orders', 'settled_at') IS NULL
    ALTER TABLE dbo.Orders ADD settled_at DATETIME2(0) NULL;
GO

-- Backfill: order hủy nào đã có một lần thanh toán của cùng bàn sau khi được tạo và hủy
-- thì coi như đã chốt tại thời điểm thanh toán đầu tiên đó.
UPDATE o
SET o.settled_at = (
    SELECT MIN(sh.paid_at) FROM dbo.ServiceHistory sh
    WHERE sh.table_id = o.table_id
      AND o.created_at <= sh.paid_at
      AND sh.paid_at >= COALESCE(o.cancelled_at, o.closed_at))
FROM dbo.Orders o
WHERE o.order_status = 'CANCELLED'
  AND o.settled_at IS NULL
  AND EXISTS (
    SELECT 1 FROM dbo.ServiceHistory sh
    WHERE sh.table_id = o.table_id
      AND o.created_at <= sh.paid_at
      AND sh.paid_at >= COALESCE(o.cancelled_at, o.closed_at));
GO
