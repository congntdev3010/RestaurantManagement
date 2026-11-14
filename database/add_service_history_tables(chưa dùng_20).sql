-- Chưa chạy: gộp bill là thao tác RIÊNG với gộp bàn lúc check-in.
-- Chạy sau add_order_settled_at(19).sql trên AlacarteRestaurantDB.
-- Nhóm chứa bàn, không đóng băng order: order mới vẫn tính vào bill sau khi duyệt.
-- Order PENDING chỉ báo nhân viên; món PENDING/COOKING của order OPEN chặn thanh toán.
USE AlacarteRestaurantDB;
GO

IF OBJECT_ID('dbo.ServiceHistory', 'U') IS NULL
    THROW 50001, 'ServiceHistory does not exist.', 1;
IF OBJECT_ID('dbo.RestaurantTables', 'U') IS NULL
    THROW 50002, 'RestaurantTables does not exist.', 1;
IF OBJECT_ID('dbo.Users', 'U') IS NULL
    THROW 50003, 'Users does not exist.', 1;
GO

-- Trạng thái và phiên bản QR của bill gộp; số tiền QR được lưu khi phát hành.
-- Khi đổi tiền/món/điểm/voucher, phát hành phiên bản mới; chuyển khoản tới mã
-- cũ phải ghi nhận để đối soát, không tự chốt và không bảo khách chuyển lại.
-- Giao dịch phải khóa nhóm, bàn và order rồi đối chiếu tiền trước OPEN -> PAID.
IF OBJECT_ID('dbo.MergedBills', 'U') IS NULL
    CREATE TABLE dbo.MergedBills (
        merged_bill_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_MergedBills PRIMARY KEY,
        primary_table_id INT NOT NULL,
        bill_status VARCHAR(10) NOT NULL CONSTRAINT DF_MergedBills_Status DEFAULT 'OPEN',
        qr_version INT NOT NULL CONSTRAINT DF_MergedBills_QrVersion DEFAULT 1,
        qr_amount DECIMAL(14,0) NULL,
        created_by INT NOT NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_MergedBills_CreatedAt DEFAULT SYSDATETIME(),
        closed_at DATETIME2(0) NULL,
        CONSTRAINT FK_MergedBills_Table FOREIGN KEY (primary_table_id) REFERENCES dbo.RestaurantTables(table_id),
        CONSTRAINT FK_MergedBills_User FOREIGN KEY (created_by) REFERENCES dbo.Users(user_id),
        CONSTRAINT CK_MergedBills_Status CHECK (bill_status IN ('OPEN', 'PAID', 'CANCELLED')),
        CONSTRAINT CK_MergedBills_QrVersion CHECK (qr_version > 0),
        CONSTRAINT CK_MergedBills_QrAmount CHECK (qr_amount IS NULL OR qr_amount > 0)
    );
GO

-- active=1 chỉ khi bill OPEN; khi chốt/hủy phải đặt active=0 cùng transaction.
-- Không cho một bàn đồng thời nằm trong hai bill gộp đang mở.
IF OBJECT_ID('dbo.MergedBillTables', 'U') IS NULL
    CREATE TABLE dbo.MergedBillTables (
        merged_bill_id INT NOT NULL,
        table_id INT NOT NULL,
        active BIT NOT NULL CONSTRAINT DF_MergedBillTables_Active DEFAULT 1,
        CONSTRAINT PK_MergedBillTables PRIMARY KEY (merged_bill_id, table_id),
        CONSTRAINT FK_MergedBillTables_Bill FOREIGN KEY (merged_bill_id) REFERENCES dbo.MergedBills(merged_bill_id),
        CONSTRAINT FK_MergedBillTables_Table FOREIGN KEY (table_id) REFERENCES dbo.RestaurantTables(table_id)
    );
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.MergedBillTables')
               AND name = 'UX_MergedBillTables_ActiveTable')
    CREATE UNIQUE INDEX UX_MergedBillTables_ActiveTable
        ON dbo.MergedBillTables(table_id) WHERE active = 1;
GO

-- Lưu giao dịch chuyển khoản bill gộp theo ID ngân hàng (dedupe webhook).
-- REVIEW: tiền đã nhận nhưng mã/tiền không khớp, không chốt bill hoặc thu lại.
-- Nhân viên cần đối soát/hoàn khoản REVIEW trước khi bill được chốt/hủy.
IF OBJECT_ID('dbo.MergedBillTransfers', 'U') IS NULL
    CREATE TABLE dbo.MergedBillTransfers (
        transfer_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_MergedBillTransfers PRIMARY KEY,
        merged_bill_id INT NOT NULL,
        provider_transaction_id NVARCHAR(100) NOT NULL,
        payment_reference NVARCHAR(100) NOT NULL,
        amount DECIMAL(14,0) NOT NULL,
        transfer_status VARCHAR(10) NOT NULL,
        received_at DATETIME2(0) NOT NULL CONSTRAINT DF_MergedBillTransfers_At DEFAULT SYSDATETIME(),
        CONSTRAINT FK_MergedBillTransfers_Bill FOREIGN KEY (merged_bill_id) REFERENCES dbo.MergedBills(merged_bill_id),
        CONSTRAINT UQ_MergedBillTransfers_Provider UNIQUE (provider_transaction_id),
        CONSTRAINT CK_MergedBillTransfers_Status CHECK (transfer_status IN ('APPLIED', 'REVIEW')),
        CONSTRAINT CK_MergedBillTransfers_Amount CHECK (amount > 0)
    );
GO

-- Liên kết hóa đơn đã thanh toán với bill gộp, để polling/webhook tìm đúng biên lai.
IF COL_LENGTH('dbo.ServiceHistory', 'merged_bill_id') IS NULL
    ALTER TABLE dbo.ServiceHistory ADD merged_bill_id INT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID('dbo.ServiceHistory')
               AND name = 'FK_ServiceHistory_MergedBill')
    ALTER TABLE dbo.ServiceHistory ADD CONSTRAINT FK_ServiceHistory_MergedBill
        FOREIGN KEY (merged_bill_id) REFERENCES dbo.MergedBills(merged_bill_id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.ServiceHistory')
               AND name = 'UX_ServiceHistory_MergedBill')
    CREATE UNIQUE INDEX UX_ServiceHistory_MergedBill
        ON dbo.ServiceHistory(merged_bill_id) WHERE merged_bill_id IS NOT NULL;
GO

-- Thanh toán gộp bàn: một hóa đơn (ServiceHistory) có thể gồm nhiều bàn.
-- ServiceHistory.table_id vẫn là bàn đại diện; bảng này ghi đủ các bàn đã thanh toán.
-- Các bàn của hóa đơn được lưu ở bảng dưới đây.
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
