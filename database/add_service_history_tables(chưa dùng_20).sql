-- Thanh toán gộp bàn: một hóa đơn (ServiceHistory) có thể gồm nhiều bàn.
-- ServiceHistory.table_id vẫn là BÀN CHÍNH (bàn đầu tiên khi gộp); bảng này ghi đủ các bàn của hóa đơn.
-- Chạy một lần trên CSDL AlacarteRestaurantDB, sau add_order_settled_at(19).sql.
IF OBJECT_ID('dbo.ServiceHistoryTables', 'U') IS NULL
    CREATE TABLE dbo.ServiceHistoryTables (
        history_id INT NOT NULL,
        table_id   INT NOT NULL,
        CONSTRAINT PK_ServiceHistoryTables PRIMARY KEY (history_id, table_id),
        CONSTRAINT FK_ServiceHistoryTables_History FOREIGN KEY (history_id) REFERENCES dbo.ServiceHistory(history_id),
        CONSTRAINT FK_ServiceHistoryTables_Table FOREIGN KEY (table_id) REFERENCES dbo.RestaurantTables(table_id)
    );
GO

-- Hóa đơn cũ: mỗi hóa đơn chỉ có bàn chính của chính nó.
INSERT INTO dbo.ServiceHistoryTables (history_id, table_id)
SELECT h.history_id, h.table_id
FROM dbo.ServiceHistory h
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.ServiceHistoryTables x
    WHERE x.history_id = h.history_id AND x.table_id = h.table_id);
GO
