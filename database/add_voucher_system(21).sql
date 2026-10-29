-- =========================================================
-- Voucher giảm giá theo % tổng hóa đơn.
-- Chạy một lần trên CSDL AlacarteRestaurantDB (idempotent: chạy lại không lỗi).
--
-- Luồng:
--   1. Manager cấu hình chiến dịch (% giảm, tổng đã chi tối thiểu) rồi chọn khách.
--   2. Mỗi khách được sinh 1 mã 8 ký tự, lưu vào Vouchers và gửi qua email.
--   3. Lễ tân nhập mã cho bàn đang thanh toán -> lưu TableBillVoucher (giữ chỗ).
--   4. Khi thanh toán xong: Vouchers.status = USED, ServiceHistory ghi voucher_id + số tiền giảm.
-- =========================================================
USE AlacarteRestaurantDB;
GO

/* 1. Chiến dịch gửi voucher: mỗi lần Manager cấu hình và bấm gửi = 1 chiến dịch.
      discount_percent     : % giảm trên tổng hóa đơn, 0 < % <= 100.
      required_total_spent : khách phải đã chi TRÊN (lớn hơn) giá trị này mới được nhận voucher. */
IF OBJECT_ID('dbo.VoucherCampaigns', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.VoucherCampaigns (
        campaign_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_VoucherCampaigns PRIMARY KEY,
        discount_percent DECIMAL(5,2) NOT NULL,
        required_total_spent DECIMAL(14,0) NOT NULL,
        created_by INT NOT NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_VoucherCampaigns_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT CK_VoucherCampaigns_Percent CHECK (discount_percent > 0 AND discount_percent <= 100),
        CONSTRAINT CK_VoucherCampaigns_RequiredSpent CHECK (required_total_spent >= 0),
        CONSTRAINT FK_VoucherCampaigns_CreatedBy FOREIGN KEY (created_by) REFERENCES dbo.Users(user_id)
    );
END;
GO

/* 2. Từng mã voucher gửi cho từng khách.
      code                 : đúng 8 ký tự A-Z / 0-9 (CHAR(8) nên mã ngắn hơn sẽ bị đệm khoảng trắng và bị CHECK từ chối).
      customer_total_spent : tổng khách đã chi tại thời điểm gửi (snapshot để kiểm chứng điều kiện).
      status               : ACTIVE = còn dùng được, USED = đã dùng (không bao giờ quay lại ACTIVE).
      Snapshot discount_percent / required_total_spent giữ nguyên giá trị lúc gửi dù chiến dịch về sau có đổi. */
IF OBJECT_ID('dbo.Vouchers', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Vouchers (
        voucher_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Vouchers PRIMARY KEY,
        campaign_id INT NOT NULL,
        code CHAR(8) NOT NULL,
        customer_id INT NOT NULL,
        sent_to_email NVARCHAR(100) NOT NULL,
        discount_percent DECIMAL(5,2) NOT NULL,
        required_total_spent DECIMAL(14,0) NOT NULL,
        customer_total_spent DECIMAL(14,0) NOT NULL,
        status VARCHAR(10) NOT NULL CONSTRAINT DF_Vouchers_Status DEFAULT 'ACTIVE',
        sent_at DATETIME2(0) NOT NULL CONSTRAINT DF_Vouchers_SentAt DEFAULT SYSDATETIME(),
        used_at DATETIME2(0) NULL,
        CONSTRAINT UX_Vouchers_Code UNIQUE (code),
        CONSTRAINT CK_Vouchers_Code CHECK (code COLLATE Latin1_General_BIN2 NOT LIKE '%[^A-Z0-9]%'),
        CONSTRAINT CK_Vouchers_Email CHECK (sent_to_email LIKE '%_@_%._%'),
        CONSTRAINT CK_Vouchers_Percent CHECK (discount_percent > 0 AND discount_percent <= 100),
        CONSTRAINT CK_Vouchers_Eligibility CHECK (
            required_total_spent >= 0 AND customer_total_spent > required_total_spent),
        CONSTRAINT CK_Vouchers_Status CHECK (status IN ('ACTIVE', 'USED')),
        CONSTRAINT CK_Vouchers_UsedAt CHECK (
            (status = 'USED' AND used_at IS NOT NULL) OR (status = 'ACTIVE' AND used_at IS NULL)),
        CONSTRAINT FK_Vouchers_Campaign FOREIGN KEY (campaign_id) REFERENCES dbo.VoucherCampaigns(campaign_id),
        CONSTRAINT FK_Vouchers_Customer FOREIGN KEY (customer_id) REFERENCES dbo.Users(user_id)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID('dbo.Vouchers') AND name = 'IX_Vouchers_Customer')
    CREATE INDEX IX_Vouchers_Customer ON dbo.Vouchers(customer_id, status);
GO

/* 3. Voucher đang được áp cho bàn chưa thanh toán (lễ tân nhập mã trước khi thanh toán;
      QR hoàn tất sau qua webhook nên phải lưu lại, giống TableBillLoyalty).
      UNIQUE(voucher_id): một mã không thể đang gắn cho hai bàn cùng lúc.
      Dòng này bị xóa khi bàn thanh toán xong hoặc lễ tân bỏ áp mã. */
IF OBJECT_ID('dbo.TableBillVoucher', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.TableBillVoucher (
        table_id INT NOT NULL CONSTRAINT PK_TableBillVoucher PRIMARY KEY,
        voucher_id INT NOT NULL,
        set_by INT NOT NULL,
        updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_TableBillVoucher_UpdatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT UX_TableBillVoucher_Voucher UNIQUE (voucher_id),
        CONSTRAINT FK_TableBillVoucher_Table FOREIGN KEY (table_id) REFERENCES dbo.RestaurantTables(table_id),
        CONSTRAINT FK_TableBillVoucher_Voucher FOREIGN KEY (voucher_id) REFERENCES dbo.Vouchers(voucher_id),
        CONSTRAINT FK_TableBillVoucher_SetBy FOREIGN KEY (set_by) REFERENCES dbo.Users(user_id)
    );
END;
GO

/* 4. Hóa đơn đã thanh toán ghi lại voucher đã dùng và số tiền giảm.
      voucher_discount = ROUND(total_amount * voucher_discount_percent / 100, 0)
      (% tính trên tổng tiền món, trước khi trừ điểm tích lũy; làm tròn đồng).
      Số tiền khách trả = total_amount - loyalty_discount - voucher_discount. */
IF COL_LENGTH('dbo.ServiceHistory', 'voucher_id') IS NULL
    ALTER TABLE dbo.ServiceHistory ADD voucher_id INT NULL;
IF COL_LENGTH('dbo.ServiceHistory', 'voucher_discount_percent') IS NULL
    ALTER TABLE dbo.ServiceHistory ADD voucher_discount_percent DECIMAL(5,2) NOT NULL
        CONSTRAINT DF_ServiceHistory_VoucherPercent DEFAULT 0 WITH VALUES;
IF COL_LENGTH('dbo.ServiceHistory', 'voucher_discount') IS NULL
    ALTER TABLE dbo.ServiceHistory ADD voucher_discount DECIMAL(14,0) NOT NULL
        CONSTRAINT DF_ServiceHistory_VoucherDiscount DEFAULT 0 WITH VALUES;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_ServiceHistory_Voucher')
    ALTER TABLE dbo.ServiceHistory ADD CONSTRAINT FK_ServiceHistory_Voucher
        FOREIGN KEY (voucher_id) REFERENCES dbo.Vouchers(voucher_id);
GO

-- Một mã chỉ được ghi vào tối đa MỘT hóa đơn.
IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID('dbo.ServiceHistory') AND name = 'UX_ServiceHistory_Voucher')
    CREATE UNIQUE INDEX UX_ServiceHistory_Voucher ON dbo.ServiceHistory(voucher_id)
        WHERE voucher_id IS NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID('dbo.ServiceHistory') AND name = 'CK_ServiceHistory_Voucher')
    ALTER TABLE dbo.ServiceHistory ADD CONSTRAINT CK_ServiceHistory_Voucher CHECK (
        voucher_discount_percent BETWEEN 0 AND 100
        AND voucher_discount >= 0
        AND (voucher_id IS NOT NULL OR (voucher_discount = 0 AND voucher_discount_percent = 0))
        AND voucher_discount = ROUND(total_amount * voucher_discount_percent / 100, 0)
        AND loyalty_discount + voucher_discount <= total_amount);
GO
