USE AlacarteRestaurantDB;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;

DECLARE @now DATETIME2(0) = SYSDATETIME();
DECLARE @today DATE = CONVERT(DATE, @now);
DECLARE @todayTag NVARCHAR(30) = N'DASHBOARD_DEMO_' + CONVERT(CHAR(8), @today, 112);
DECLARE @customerRoleId INT;
DECLARE @managerId INT;
DECLARE @serverId INT;
DECLARE @dishCount INT;
DECLARE @earnSpendAmount DECIMAL(14, 0);
DECLARE @earnPoints INT;
DECLARE @redeemValuePerPoint DECIMAL(14, 0);
DECLARE @busyToCreate INT;

IF OBJECT_ID('dbo.RestaurantTables', 'U') IS NULL
    THROW 50001, 'RestaurantTables does not exist. Run the restaurant table schema script first.', 1;
IF OBJECT_ID('dbo.Orders', 'U') IS NULL
    THROW 50002, 'Orders does not exist. Run the reservation/order schema script first.', 1;
IF OBJECT_ID('dbo.OrderItems', 'U') IS NULL
    THROW 50003, 'OrderItems does not exist. Run add_order_items_and_workflow.sql first.', 1;
IF OBJECT_ID('dbo.ServiceHistory', 'U') IS NULL
    THROW 50004, 'ServiceHistory does not exist. Run the payment schema script first.', 1;
IF OBJECT_ID('dbo.ServiceHistoryItems', 'U') IS NULL
    THROW 50005, 'ServiceHistoryItems does not exist. Run the payment schema script first.', 1;
IF OBJECT_ID('dbo.LoyaltyTransactions', 'U') IS NULL
    THROW 50006, 'LoyaltyTransactions does not exist. Run the loyalty schema script first.', 1;
IF OBJECT_ID('dbo.LoyaltySettings', 'U') IS NULL
    THROW 50007, 'LoyaltySettings does not exist. Run the loyalty schema script first.', 1;
IF COL_LENGTH('dbo.Users', 'loyalty_points') IS NULL
    THROW 50008, 'Users.loyalty_points is missing. Run the loyalty schema script first.', 1;
IF COL_LENGTH('dbo.Orders', 'customer_email') IS NULL
    OR COL_LENGTH('dbo.Orders', 'paid_at') IS NULL
    OR COL_LENGTH('dbo.Orders', 'cancelled_by') IS NULL
    THROW 50009, 'Orders is missing workflow/payment columns. Run the order workflow and payment scripts first.', 1;
IF COL_LENGTH('dbo.OrderItems', 'item_status') IS NULL
    OR COL_LENGTH('dbo.OrderItems', 'requested_quantity') IS NULL
    THROW 50010, 'OrderItems workflow columns are missing. Run add_qr_order_approval.sql first.', 1;
IF COL_LENGTH('dbo.ServiceHistory', 'loyalty_customer_id') IS NULL
    OR COL_LENGTH('dbo.ServiceHistory', 'loyalty_points_earned') IS NULL
    OR COL_LENGTH('dbo.ServiceHistory', 'customer_email') IS NULL
    THROW 50011, 'ServiceHistory is missing loyalty fields. Run the loyalty schema script first.', 1;

SELECT @customerRoleId = role_id FROM dbo.Roles WHERE role_name = N'Customer';
SELECT TOP (1) @managerId = user_id
FROM dbo.Users
WHERE role_id = (SELECT role_id FROM dbo.Roles WHERE role_name = N'Manager')
  AND is_active = 1 AND is_deleted = 0
ORDER BY user_id;
SELECT TOP (1) @serverId = user_id
FROM dbo.Users
WHERE role_id = (SELECT role_id FROM dbo.Roles WHERE role_name = N'Server')
  AND is_active = 1 AND is_deleted = 0
ORDER BY user_id;
SELECT @earnSpendAmount = earn_spend_amount,
       @earnPoints = earn_points,
       @redeemValuePerPoint = redeem_value_per_point
FROM dbo.LoyaltySettings
WHERE settings_id = 1;

IF @customerRoleId IS NULL
    THROW 50012, 'Customer role is missing.', 1;
IF @managerId IS NULL
    THROW 50013, 'Create an active Manager account before running this demo seed.', 1;
IF @serverId IS NULL
    SET @serverId = @managerId;
IF @earnSpendAmount IS NULL OR @earnPoints IS NULL OR @redeemValuePerPoint IS NULL
    THROW 50014, 'Loyalty settings are missing. Configure LoyaltySettings first.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    CREATE TABLE #Numbers (n INT NOT NULL PRIMARY KEY);
    ;WITH numbers AS (
        SELECT 1 AS n
        UNION ALL
        SELECT n + 1 FROM numbers WHERE n < 40
    )
    INSERT INTO #Numbers (n)
    SELECT n FROM numbers OPTION (MAXRECURSION 40);

    CREATE TABLE #DemoTables (
        table_no INT NOT NULL PRIMARY KEY,
        table_id INT NOT NULL UNIQUE,
        table_name NVARCHAR(50) NOT NULL
    );

    INSERT INTO #DemoTables (table_no, table_id, table_name)
    SELECT ROW_NUMBER() OVER (ORDER BY t.table_id), t.table_id, t.table_name
    FROM dbo.RestaurantTables t
    WHERE t.table_name NOT LIKE N'DEMO-%';

    IF (SELECT COUNT(*) FROM #DemoTables) < 40
        THROW 50017, 'At least 40 real restaurant tables are required for this demo seed.', 1;

    UPDATE history
    SET table_id = tables.table_id
    FROM dbo.ServiceHistory history
    JOIN dbo.RestaurantTables old_table ON old_table.table_id = history.table_id
    JOIN #Numbers n ON n.n = TRY_CONVERT(INT, RIGHT(history.customer_name, 2))
    JOIN #DemoTables tables ON tables.table_no = n.n
    WHERE old_table.table_name LIKE N'DEMO-SERVED-%'
      AND history.customer_name LIKE N'DASHBOARD_DEMO_%_SERVED_%';

    UPDATE orders
    SET table_id = tables.table_id
    FROM dbo.Orders orders
    JOIN dbo.RestaurantTables old_table ON old_table.table_id = orders.table_id
    JOIN #Numbers n ON n.n = TRY_CONVERT(INT, RIGHT(orders.note, 2))
    JOIN #DemoTables tables ON tables.table_no = n.n
    WHERE old_table.table_name LIKE N'DEMO-SERVED-%'
      AND orders.note LIKE N'DASHBOARD_DEMO_%_PAID_%';

    UPDATE orders
    SET table_id = tables.table_id
    FROM dbo.Orders orders
    JOIN dbo.RestaurantTables old_table ON old_table.table_id = orders.table_id
    JOIN #Numbers n ON n.n = TRY_CONVERT(INT, RIGHT(orders.note, 2))
    JOIN #DemoTables tables ON tables.table_no = n.n + 25
    WHERE old_table.table_name LIKE N'DEMO-BUSY-%'
      AND orders.note LIKE N'DASHBOARD_DEMO_%_BUSY_%';

    UPDATE t
    SET table_status = 'OCCUPIED'
    FROM dbo.RestaurantTables t
    WHERE EXISTS (
        SELECT 1
        FROM dbo.Orders o
        WHERE o.table_id = t.table_id
          AND o.order_status IN ('PENDING', 'OPEN')
          AND o.note LIKE N'DASHBOARD_DEMO_%_BUSY_%'
    );

    IF EXISTS (
        SELECT 1
        FROM dbo.RestaurantTables old_table
        WHERE old_table.table_name LIKE N'DEMO-%'
          AND (
              EXISTS (SELECT 1 FROM dbo.Reservations r WHERE r.table_id = old_table.table_id)
              OR EXISTS (SELECT 1 FROM dbo.ReservationTables r WHERE r.table_id = old_table.table_id)
              OR EXISTS (SELECT 1 FROM dbo.ServiceHistoryTables h WHERE h.table_id = old_table.table_id)
              OR EXISTS (SELECT 1 FROM dbo.TableBillLoyalty b WHERE b.table_id = old_table.table_id)
              OR EXISTS (SELECT 1 FROM dbo.TableBillVoucher b WHERE b.table_id = old_table.table_id)
          )
    )
        THROW 50018, 'Demo tables have non-demo references and cannot be removed safely.', 1;

    DELETE old_table
    FROM dbo.RestaurantTables old_table
    WHERE old_table.table_name LIKE N'DEMO-%'
      AND NOT EXISTS (SELECT 1 FROM dbo.Orders o WHERE o.table_id = old_table.table_id)
      AND NOT EXISTS (SELECT 1 FROM dbo.ServiceHistory h WHERE h.table_id = old_table.table_id);

    CREATE TABLE #DemoCustomers (
        customer_no INT NOT NULL PRIMARY KEY,
        user_id INT NOT NULL,
        username NVARCHAR(50) NOT NULL,
        email NVARCHAR(100) NOT NULL,
        full_name NVARCHAR(100) NOT NULL,
        phone NVARCHAR(20) NOT NULL
    );

    INSERT INTO dbo.Users (username, email, password, full_name, phone, role_id, is_active)
    SELECT CONCAT(N'dashboard_demo_', RIGHT(CONCAT('00', n), 2)),
           CONCAT(N'dashboard_demo_', RIGHT(CONCAT('00', n), 2), N'@example.com'),
           N'123456',
           CONCAT(N'Demo Customer ', RIGHT(CONCAT('00', n), 2)),
           CONCAT(N'0919', RIGHT(CONCAT('000000', n), 6)),
           @customerRoleId,
           1
    FROM #Numbers n
    WHERE n <= 25
      AND NOT EXISTS (
          SELECT 1 FROM dbo.Users u
          WHERE u.username = CONCAT(N'dashboard_demo_', RIGHT(CONCAT('00', n.n), 2))
      );

    UPDATE u
    SET full_name = CONCAT(N'Demo Customer ', RIGHT(CONCAT('00', n.n), 2))
    FROM dbo.Users u
    JOIN #Numbers n
      ON u.username = CONCAT(N'dashboard_demo_', RIGHT(CONCAT('00', n.n), 2))
    WHERE n.n <= 25;

    INSERT INTO #DemoCustomers (customer_no, user_id, username, email, full_name, phone)
    SELECT n.n, u.user_id, u.username, u.email, u.full_name, u.phone
    FROM #Numbers n
    JOIN dbo.Users u
      ON u.username = CONCAT(N'dashboard_demo_', RIGHT(CONCAT('00', n.n), 2))
    WHERE n.n <= 25 AND u.is_active = 1 AND u.is_deleted = 0;

    IF (SELECT COUNT(*) FROM #DemoCustomers) <> 25
        THROW 50015, 'Could not prepare all 25 demo customer accounts.', 1;

    CREATE TABLE #ActiveDishes (
        dish_rank INT NOT NULL PRIMARY KEY,
        dish_id INT NOT NULL,
        dish_name NVARCHAR(150) NOT NULL,
        unit NVARCHAR(50) NULL,
        price DECIMAL(12, 0) NOT NULL
    );

    INSERT INTO #ActiveDishes (dish_rank, dish_id, dish_name, unit, price)
    SELECT ROW_NUMBER() OVER (ORDER BY c.category_id, d.dish_id),
           d.dish_id, d.dish_name, d.unit, d.price
    FROM dbo.Dish d
    JOIN dbo.Category c ON c.category_id = d.category_id
    WHERE d.status = 1 AND c.status = 1;

    SELECT @dishCount = COUNT(*) FROM #ActiveDishes;
    IF @dishCount < 2
        THROW 50016, 'At least two active dishes are required to create demo orders.', 1;

    CREATE TABLE #ServedPlan (
        service_no INT NOT NULL PRIMARY KEY,
        table_id INT NOT NULL,
        user_id INT NOT NULL,
        customer_name NVARCHAR(100) NOT NULL,
        customer_email NVARCHAR(100) NOT NULL,
        customer_phone NVARCHAR(20) NOT NULL,
        dish_id_1 INT NOT NULL,
        dish_name_1 NVARCHAR(150) NOT NULL,
        unit_1 NVARCHAR(50) NULL,
        price_1 DECIMAL(12, 0) NOT NULL,
        quantity_1 INT NOT NULL,
        dish_id_2 INT NOT NULL,
        dish_name_2 NVARCHAR(150) NOT NULL,
        unit_2 NVARCHAR(50) NULL,
        price_2 DECIMAL(12, 0) NOT NULL,
        quantity_2 INT NOT NULL,
        total_amount DECIMAL(14, 0) NOT NULL,
        points_used INT NOT NULL,
        points_earned INT NOT NULL,
        loyalty_discount DECIMAL(14, 0) NOT NULL,
        payment_method VARCHAR(10) NOT NULL,
        checked_in_at DATETIME2(0) NOT NULL,
        paid_at DATETIME2(0) NOT NULL
    );

    INSERT INTO #ServedPlan
    SELECT n.n,
           t.table_id,
           customer.user_id,
           CONCAT(@todayTag, N'_SERVED_', RIGHT(CONCAT('00', n.n), 2)),
           customer.email,
           customer.phone,
           dish1.dish_id,
           dish1.dish_name,
           dish1.unit,
           dish1.price,
           1 + n.n % 3,
           dish2.dish_id,
           dish2.dish_name,
           dish2.unit,
           dish2.price,
           1 + n.n % 2,
           dish1.price * (1 + n.n % 3) + dish2.price * (1 + n.n % 2),
           5,
           CONVERT(INT, FLOOR(
               (dish1.price * (1 + n.n % 3) + dish2.price * (1 + n.n % 2)
                    - 5 * @redeemValuePerPoint) / @earnSpendAmount
           )) * @earnPoints,
           5 * @redeemValuePerPoint,
           CASE WHEN n.n % 2 = 0 THEN 'QR' ELSE 'CASH' END,
           DATEADD(MINUTE, -(35 + n.n % 56), DATEADD(MINUTE, -(5 + n.n * 3), @now)),
           DATEADD(MINUTE, -(5 + n.n * 3), @now)
    FROM #Numbers n
    JOIN #DemoTables t ON t.table_no = n.n
    JOIN #DemoCustomers customer ON customer.customer_no = n.n
    JOIN #ActiveDishes dish1 ON dish1.dish_rank = ((n.n - 1) % @dishCount) + 1
    JOIN #ActiveDishes dish2 ON dish2.dish_rank = (n.n % @dishCount) + 1
    WHERE n.n <= 25
      AND NOT EXISTS (
          SELECT 1 FROM dbo.ServiceHistory h
          WHERE h.customer_name = CONCAT(@todayTag, N'_SERVED_', RIGHT(CONCAT('00', n.n), 2))
      );

    CREATE TABLE #NewHistory (
        history_id INT NOT NULL,
        customer_name NVARCHAR(100) NOT NULL
    );

    INSERT INTO dbo.ServiceHistory
        (table_id, customer_name, customer_phone, customer_email, checked_in_at, paid_at,
         payment_method, total_amount, cash_received, cash_change, paid_by,
         loyalty_customer_id, loyalty_points_used, loyalty_discount, loyalty_points_earned)
    OUTPUT INSERTED.history_id, INSERTED.customer_name
        INTO #NewHistory (history_id, customer_name)
    SELECT sp.table_id,
           sp.customer_name,
           sp.customer_phone,
           sp.customer_email,
           sp.checked_in_at,
           sp.paid_at,
           sp.payment_method,
           sp.total_amount,
           CASE WHEN sp.payment_method = 'CASH'
                THEN sp.total_amount - sp.loyalty_discount ELSE NULL END,
           CASE WHEN sp.payment_method = 'CASH' THEN 0 ELSE NULL END,
           @managerId,
           sp.user_id,
           sp.points_used,
           sp.loyalty_discount,
           sp.points_earned
    FROM #ServedPlan sp;

    INSERT INTO dbo.ServiceHistoryItems
        (history_id, dish_name, unit, unit_price, quantity, line_total)
    SELECT h.history_id, sp.dish_name_1, COALESCE(sp.unit_1, N'Portion'),
           sp.price_1, sp.quantity_1, sp.price_1 * sp.quantity_1
    FROM #NewHistory h
    JOIN #ServedPlan sp ON sp.customer_name = h.customer_name
    UNION ALL
    SELECT h.history_id, sp.dish_name_2, COALESCE(sp.unit_2, N'Portion'),
           sp.price_2, sp.quantity_2, sp.price_2 * sp.quantity_2
    FROM #NewHistory h
    JOIN #ServedPlan sp ON sp.customer_name = h.customer_name;

    INSERT INTO dbo.LoyaltyTransactions
        (user_id, history_id, txn_type, points, balance_after, bill_amount, money_value,
         earn_spend_amount, earn_points, redeem_value_per_point, note, created_by)
    SELECT sp.user_id, h.history_id, 'EARN', sp.points_earned,
           u.loyalty_points + sp.points_earned,
           sp.total_amount, sp.total_amount - sp.loyalty_discount,
           @earnSpendAmount, @earnPoints, @redeemValuePerPoint,
           CONCAT(N'Demo earn points invoice ', sp.customer_name), @managerId
    FROM #NewHistory h
    JOIN #ServedPlan sp ON sp.customer_name = h.customer_name
    JOIN dbo.Users u ON u.user_id = sp.user_id;

    INSERT INTO dbo.LoyaltyTransactions
        (user_id, history_id, txn_type, points, balance_after, bill_amount, money_value,
         earn_spend_amount, earn_points, redeem_value_per_point, note, created_by)
    SELECT sp.user_id, h.history_id, 'REDEEM', -sp.points_used,
           u.loyalty_points + sp.points_earned - sp.points_used,
           sp.total_amount, sp.loyalty_discount,
           @earnSpendAmount, @earnPoints, @redeemValuePerPoint,
           CONCAT(N'Demo redeem points invoice ', sp.customer_name), @managerId
    FROM #NewHistory h
    JOIN #ServedPlan sp ON sp.customer_name = h.customer_name
    JOIN dbo.Users u ON u.user_id = sp.user_id;

    UPDATE u
    SET loyalty_points = u.loyalty_points + totals.points_earned - totals.points_used
    FROM dbo.Users u
    JOIN (
        SELECT sp.user_id, SUM(sp.points_earned) AS points_earned,
               SUM(sp.points_used) AS points_used
        FROM #NewHistory h
        JOIN #ServedPlan sp ON sp.customer_name = h.customer_name
        GROUP BY sp.user_id
    ) totals ON totals.user_id = u.user_id;

    INSERT INTO dbo.Orders
        (table_id, user_id, created_by, customer_name, customer_phone, customer_email,
         order_status, total_amount, note, created_at, closed_at, paid_at,
         paid_by, payment_method)
    SELECT sp.table_id,
           sp.user_id,
           @serverId,
           sp.customer_name,
           sp.customer_phone,
           sp.customer_email,
           'PAID',
           sp.total_amount,
           CONCAT(@todayTag, N'_PAID_', RIGHT(CONCAT('00', sp.service_no), 2)),
           DATEADD(MINUTE, -5, sp.paid_at),
           sp.paid_at,
           sp.paid_at,
           @managerId,
           sp.payment_method
    FROM #ServedPlan sp;

    INSERT INTO dbo.OrderItems
        (order_id, dish_id, dish_name, unit, unit_price, quantity, requested_quantity,
         line_total, item_status)
    SELECT o.order_id, sp.dish_id_1, sp.dish_name_1, COALESCE(sp.unit_1, N'Portion'),
           sp.price_1, sp.quantity_1, sp.quantity_1,
           sp.price_1 * sp.quantity_1, 'SERVED'
    FROM #ServedPlan sp
    JOIN dbo.Orders o
      ON o.note = CONCAT(@todayTag, N'_PAID_', RIGHT(CONCAT('00', sp.service_no), 2))
    UNION ALL
    SELECT o.order_id, sp.dish_id_2, sp.dish_name_2, COALESCE(sp.unit_2, N'Portion'),
           sp.price_2, sp.quantity_2, sp.quantity_2,
           sp.price_2 * sp.quantity_2, 'SERVED'
    FROM #ServedPlan sp
    JOIN dbo.Orders o
      ON o.note = CONCAT(@todayTag, N'_PAID_', RIGHT(CONCAT('00', sp.service_no), 2));

    CREATE TABLE #BusyTables (
        table_no INT NOT NULL PRIMARY KEY,
        table_id INT NOT NULL,
        user_id INT NOT NULL,
        customer_name NVARCHAR(100) NOT NULL,
        customer_email NVARCHAR(100) NOT NULL,
        customer_phone NVARCHAR(20) NOT NULL,
        dish_id_1 INT NOT NULL,
        dish_name_1 NVARCHAR(150) NOT NULL,
        unit_1 NVARCHAR(50) NULL,
        price_1 DECIMAL(12, 0) NOT NULL,
        dish_id_2 INT NOT NULL,
        dish_name_2 NVARCHAR(150) NOT NULL,
        unit_2 NVARCHAR(50) NULL,
        price_2 DECIMAL(12, 0) NOT NULL
    );

    CREATE TABLE #AvailableBusyTables (
        table_no INT NOT NULL PRIMARY KEY,
        table_id INT NOT NULL UNIQUE
    );

    INSERT INTO #AvailableBusyTables (table_no, table_id)
    SELECT ROW_NUMBER() OVER (ORDER BY t.table_id), t.table_id
    FROM dbo.RestaurantTables t
    WHERE t.table_name NOT LIKE N'DEMO-%'
      AND t.table_status = 'AVAILABLE'
      AND NOT EXISTS (
          SELECT 1 FROM dbo.Orders o
          WHERE o.table_id = t.table_id AND o.order_status IN ('PENDING', 'OPEN')
      );

    SELECT @busyToCreate = CASE
        WHEN COUNT(DISTINCT o.table_id) >= 15 THEN 0
        ELSE 15 - COUNT(DISTINCT o.table_id)
    END
    FROM dbo.Orders o
    WHERE o.order_status IN ('PENDING', 'OPEN')
      AND o.note LIKE N'DASHBOARD_DEMO_%_BUSY_%';

    INSERT INTO #BusyTables
    SELECT n.n,
           t.table_id,
           customer.user_id,
           customer.full_name,
           customer.email,
           customer.phone,
           dish1.dish_id,
           dish1.dish_name,
           dish1.unit,
           dish1.price,
           dish2.dish_id,
           dish2.dish_name,
           dish2.unit,
           dish2.price
    FROM #Numbers n
    JOIN #AvailableBusyTables t ON t.table_no = n.n
    JOIN #DemoCustomers customer ON customer.customer_no = n.n
    JOIN #ActiveDishes dish1 ON dish1.dish_rank = ((n.n - 1) % @dishCount) + 1
    JOIN #ActiveDishes dish2 ON dish2.dish_rank = (n.n % @dishCount) + 1
    WHERE n.n <= @busyToCreate;

    CREATE TABLE #NewBusyOrders (
        order_id INT NOT NULL,
        note NVARCHAR(100) NOT NULL
    );

    INSERT INTO dbo.Orders
        (table_id, user_id, created_by, customer_name, customer_phone, customer_email,
         order_status, total_amount, note, created_at)
    OUTPUT INSERTED.order_id, INSERTED.note INTO #NewBusyOrders (order_id, note)
    SELECT busy.table_id,
           busy.user_id,
           @serverId,
           busy.customer_name,
           busy.customer_phone,
           busy.customer_email,
           'OPEN',
           busy.price_1 + busy.price_2,
           CONCAT(@todayTag, N'_BUSY_', RIGHT(CONCAT('00', busy.table_no), 2)),
           DATEADD(MINUTE, -(10 + busy.table_no), @now)
    FROM #BusyTables busy;

    INSERT INTO dbo.OrderItems
        (order_id, dish_id, dish_name, unit, unit_price, quantity, requested_quantity,
         line_total, item_status)
    SELECT o.order_id, busy.dish_id_1, busy.dish_name_1, COALESCE(busy.unit_1, N'Portion'),
           busy.price_1, 1, 1, busy.price_1, 'COOKING'
    FROM #NewBusyOrders o
    JOIN #BusyTables busy
      ON o.note = CONCAT(@todayTag, N'_BUSY_', RIGHT(CONCAT('00', busy.table_no), 2))
    UNION ALL
    SELECT o.order_id, busy.dish_id_2, busy.dish_name_2, COALESCE(busy.unit_2, N'Portion'),
           busy.price_2, 1, 1, busy.price_2, 'COOKING'
    FROM #NewBusyOrders o
    JOIN #BusyTables busy
      ON o.note = CONCAT(@todayTag, N'_BUSY_', RIGHT(CONCAT('00', busy.table_no), 2));

    UPDATE t
    SET table_status = 'OCCUPIED'
    FROM dbo.RestaurantTables t
    JOIN #BusyTables busy ON busy.table_id = t.table_id
    WHERE EXISTS (
        SELECT 1 FROM dbo.Orders o
        WHERE o.table_id = t.table_id AND o.order_status IN ('PENDING', 'OPEN')
    );

    INSERT INTO dbo.Orders
        (user_id, created_by, customer_name, customer_phone, customer_email,
         order_status, total_amount, note, created_at, closed_at, cancelled_at,
         cancelled_by, cancellation_reason)
    SELECT customer.user_id,
           @serverId,
           customer.full_name,
           customer.phone,
           customer.email,
           'CANCELLED',
           0,
           CONCAT(@todayTag, N'_CANCELLED_', RIGHT(CONCAT('00', n.n), 2)),
           DATEADD(MINUTE, -(30 + n.n), @now),
           DATEADD(MINUTE, -(20 + n.n), @now),
           DATEADD(MINUTE, -(20 + n.n), @now),
           @managerId,
           N'Demo cancelled order for dashboard testing'
    FROM #Numbers n
    JOIN #DemoCustomers customer ON customer.customer_no = n.n
    WHERE n.n <= 5
      AND NOT EXISTS (
          SELECT 1 FROM dbo.Orders o
          WHERE o.note = CONCAT(@todayTag, N'_CANCELLED_', RIGHT(CONCAT('00', n.n), 2))
      );

    UPDATE txn
    SET note = CONCAT(
        N'Demo ',
        CASE WHEN txn.txn_type = 'EARN' THEN N'earn points invoice '
             ELSE N'redeem points invoice ' END,
        history.customer_name
    )
    FROM dbo.LoyaltyTransactions txn
    JOIN dbo.ServiceHistory history ON history.history_id = txn.history_id
    WHERE history.customer_name LIKE @todayTag + N'_SERVED_%'
      AND txn.txn_type IN ('EARN', 'REDEEM');

    COMMIT TRANSACTION;

    SELECT N'Demo data inserted' AS result,
           @todayTag AS demo_tag,
           (SELECT COUNT(*) FROM dbo.Users WHERE username LIKE N'dashboard_demo_%') AS demo_customers,
           (SELECT COUNT(DISTINCT h.table_id)
            FROM dbo.ServiceHistory h
            WHERE h.customer_name LIKE @todayTag + N'_SERVED_%'
              AND h.paid_at >= CONVERT(DATE, @now)
              AND h.paid_at < DATEADD(DAY, 1, CONVERT(DATE, @now))) AS served_demo_tables,
           (SELECT COUNT(DISTINCT o.table_id)
            FROM dbo.Orders o
            WHERE o.note LIKE @todayTag + N'_BUSY_%'
              AND o.order_status IN ('PENDING', 'OPEN')) AS busy_demo_tables,
           (SELECT COUNT(*) FROM dbo.ServiceHistory
            WHERE customer_name LIKE @todayTag + N'_SERVED_%'
              AND paid_at >= CONVERT(DATE, @now)
              AND paid_at < DATEADD(DAY, 1, CONVERT(DATE, @now))) AS services_today,
           (SELECT COUNT(*) FROM dbo.Orders
            WHERE note LIKE @todayTag + N'_%'
              AND created_at >= CONVERT(DATE, @now)
              AND created_at < DATEADD(DAY, 1, CONVERT(DATE, @now))) AS demo_orders_today;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
