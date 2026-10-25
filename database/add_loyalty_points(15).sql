USE AlacarteRestaurantDB;
GO

IF OBJECT_ID('dbo.Users', 'U') IS NULL
    THROW 50001, 'Users does not exist.', 1;
IF OBJECT_ID('dbo.ServiceHistory', 'U') IS NULL
    THROW 50001, 'ServiceHistory does not exist. Run add_payment(14).sql first.', 1;
GO

/* 1. Số dư điểm tích lũy hiện tại của từng tài khoản (khách mới = 0 điểm). */
IF COL_LENGTH('dbo.Users', 'loyalty_points') IS NULL
    ALTER TABLE dbo.Users ADD loyalty_points INT NOT NULL
        CONSTRAINT DF_Users_LoyaltyPoints DEFAULT 0 WITH VALUES;
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID('dbo.Users') AND name = 'CK_Users_LoyaltyPoints')
    ALTER TABLE dbo.Users ADD CONSTRAINT CK_Users_LoyaltyPoints CHECK (loyalty_points >= 0);
GO

/* 2. Quy tắc quy đổi điểm: đúng 1 dòng (id = 1), chỉ Manager được sửa.
      - Tích điểm: mỗi earn_spend_amount đồng thanh toán -> earn_points điểm.
      - Dùng điểm: mỗi 1 điểm -> giảm redeem_value_per_point đồng.
   Mặc định: 1.000đ = 1 điểm; 1 điểm = giảm 1.000đ. */
IF OBJECT_ID('dbo.LoyaltySettings', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.LoyaltySettings (
        settings_id INT NOT NULL CONSTRAINT PK_LoyaltySettings PRIMARY KEY
            CONSTRAINT CK_LoyaltySettings_Single CHECK (settings_id = 1),
        earn_spend_amount DECIMAL(14,0) NOT NULL CONSTRAINT DF_LoyaltySettings_Spend DEFAULT 1000,
        earn_points INT NOT NULL CONSTRAINT DF_LoyaltySettings_Points DEFAULT 1,
        redeem_value_per_point DECIMAL(14,0) NOT NULL CONSTRAINT DF_LoyaltySettings_Redeem DEFAULT 1000,
        updated_by INT NULL,
        updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_LoyaltySettings_UpdatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT CK_LoyaltySettings_Positive CHECK
            (earn_spend_amount > 0 AND earn_points > 0 AND redeem_value_per_point > 0),
        CONSTRAINT FK_LoyaltySettings_UpdatedBy FOREIGN KEY (updated_by) REFERENCES dbo.Users(user_id)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.LoyaltySettings WHERE settings_id = 1)
    INSERT INTO dbo.LoyaltySettings (settings_id, earn_spend_amount, earn_points, redeem_value_per_point)
    VALUES (1, 1000, 1, 1000);
GO

/* 3. Lễ tân gắn tài khoản customer + số điểm muốn dùng cho bill của một bàn
      TRƯỚC khi thanh toán (cần lưu lại vì thanh toán QR hoàn tất sau qua webhook).
      Xóa dòng này khi bàn đã thanh toán hoặc lễ tân bỏ gắn. */
IF OBJECT_ID('dbo.TableBillLoyalty', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.TableBillLoyalty (
        table_id INT NOT NULL CONSTRAINT PK_TableBillLoyalty PRIMARY KEY,
        customer_id INT NOT NULL,
        points_to_use INT NOT NULL CONSTRAINT DF_TableBillLoyalty_Use DEFAULT 0,
        set_by INT NOT NULL,
        updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_TableBillLoyalty_UpdatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT CK_TableBillLoyalty_Use CHECK (points_to_use >= 0),
        CONSTRAINT FK_TableBillLoyalty_Table FOREIGN KEY (table_id) REFERENCES dbo.RestaurantTables(table_id),
        CONSTRAINT FK_TableBillLoyalty_Customer FOREIGN KEY (customer_id) REFERENCES dbo.Users(user_id),
        CONSTRAINT FK_TableBillLoyalty_SetBy FOREIGN KEY (set_by) REFERENCES dbo.Users(user_id)
    );
END;
GO

/* 4. Hóa đơn đã thanh toán (ServiceHistory) ghi lại điểm: khách nào, dùng bao nhiêu,
      giảm bao nhiêu tiền, được cộng bao nhiêu.
      total_amount = tổng tiền món (trước giảm); số tiền khách trả = total_amount - loyalty_discount. */
IF COL_LENGTH('dbo.ServiceHistory', 'loyalty_customer_id') IS NULL
    ALTER TABLE dbo.ServiceHistory ADD loyalty_customer_id INT NULL;
IF COL_LENGTH('dbo.ServiceHistory', 'loyalty_points_used') IS NULL
    ALTER TABLE dbo.ServiceHistory ADD loyalty_points_used INT NOT NULL
        CONSTRAINT DF_ServiceHistory_PointsUsed DEFAULT 0 WITH VALUES;
IF COL_LENGTH('dbo.ServiceHistory', 'loyalty_discount') IS NULL
    ALTER TABLE dbo.ServiceHistory ADD loyalty_discount DECIMAL(14,0) NOT NULL
        CONSTRAINT DF_ServiceHistory_LoyaltyDiscount DEFAULT 0 WITH VALUES;
IF COL_LENGTH('dbo.ServiceHistory', 'loyalty_points_earned') IS NULL
    ALTER TABLE dbo.ServiceHistory ADD loyalty_points_earned INT NOT NULL
        CONSTRAINT DF_ServiceHistory_PointsEarned DEFAULT 0 WITH VALUES;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_ServiceHistory_LoyaltyCustomer')
    ALTER TABLE dbo.ServiceHistory ADD CONSTRAINT FK_ServiceHistory_LoyaltyCustomer
        FOREIGN KEY (loyalty_customer_id) REFERENCES dbo.Users(user_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID('dbo.ServiceHistory') AND name = 'CK_ServiceHistory_Loyalty')
    ALTER TABLE dbo.ServiceHistory ADD CONSTRAINT CK_ServiceHistory_Loyalty CHECK (
        loyalty_points_used >= 0 AND loyalty_discount >= 0 AND loyalty_points_earned >= 0
        AND loyalty_discount <= total_amount
        AND (loyalty_customer_id IS NOT NULL
             OR (loyalty_points_used = 0 AND loyalty_discount = 0 AND loyalty_points_earned = 0)));
GO

/* 5. Lịch sử biến động điểm (khách xem ở màn "Điểm tích lũy").
      points có dấu: EARN > 0, REDEEM < 0, ADJUST +/- (điều chỉnh tay nếu cần).
      Lưu cả tỷ lệ quy đổi lúc đó để sau này Manager đổi quy tắc, lịch sử cũ vẫn đúng. */
IF OBJECT_ID('dbo.LoyaltyTransactions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.LoyaltyTransactions (
        txn_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_LoyaltyTransactions PRIMARY KEY,
        user_id INT NOT NULL,
        history_id INT NULL,
        txn_type VARCHAR(10) NOT NULL,
        points INT NOT NULL,
        balance_after INT NOT NULL,
        bill_amount DECIMAL(14,0) NULL,
        money_value DECIMAL(14,0) NULL,
        earn_spend_amount DECIMAL(14,0) NULL,
        earn_points INT NULL,
        redeem_value_per_point DECIMAL(14,0) NULL,
        note NVARCHAR(255) NULL,
        created_by INT NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_LoyaltyTransactions_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT CK_LoyaltyTransactions_Type CHECK (txn_type IN ('EARN', 'REDEEM', 'ADJUST')),
        CONSTRAINT CK_LoyaltyTransactions_Sign CHECK (
            (txn_type = 'EARN' AND points > 0) OR (txn_type = 'REDEEM' AND points < 0) OR txn_type = 'ADJUST'),
        CONSTRAINT CK_LoyaltyTransactions_Balance CHECK (balance_after >= 0),
        CONSTRAINT FK_LoyaltyTransactions_User FOREIGN KEY (user_id) REFERENCES dbo.Users(user_id),
        CONSTRAINT FK_LoyaltyTransactions_History FOREIGN KEY (history_id) REFERENCES dbo.ServiceHistory(history_id),
        CONSTRAINT FK_LoyaltyTransactions_CreatedBy FOREIGN KEY (created_by) REFERENCES dbo.Users(user_id)
    );
    CREATE INDEX IX_LoyaltyTransactions_User ON dbo.LoyaltyTransactions(user_id, created_at DESC);
    -- Mỗi hóa đơn chỉ cộng điểm một lần và trừ điểm một lần (chặn webhook/bấm trùng).
    CREATE UNIQUE INDEX UX_LoyaltyTransactions_History_Type ON dbo.LoyaltyTransactions(history_id, txn_type)
        WHERE history_id IS NOT NULL AND txn_type IN ('EARN', 'REDEEM');
END;
GO
