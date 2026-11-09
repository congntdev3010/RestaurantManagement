USE AlacarteRestaurantDB;
GO

-- Demo data from 2026-03-01 through the date this script is run.
-- Creates 10-20 paid orders on 10-20 existing restaurant tables per day,
-- with varied dishes, quantities, customers, and payment times.
-- Does not create physical restaurant tables or change their current status.
-- Run once; the unique note prefix prevents duplicate seeding.

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
DECLARE @startDate DATE = '20260301';
DECLARE @endDate DATE = @today;
DECLARE @tagPrefix NVARCHAR(40) = N'DASHBOARD_2026_MAR_TO_NOW_';
DECLARE @managerId INT;
DECLARE @serverId INT;
DECLARE @tableCount INT;
DECLARE @dishCount INT;

IF OBJECT_ID('dbo.RestaurantTables', 'U') IS NULL
    THROW 50101, 'RestaurantTables does not exist. Create the restaurant tables first.', 1;
IF OBJECT_ID('dbo.Orders', 'U') IS NULL
    THROW 50102, 'Orders does not exist. Run the order schema scripts first.', 1;
IF OBJECT_ID('dbo.OrderItems', 'U') IS NULL
    THROW 50103, 'OrderItems does not exist. Run add_order_items_and_workflow.sql first.', 1;
IF OBJECT_ID('dbo.ServiceHistory', 'U') IS NULL
    THROW 50104, 'ServiceHistory does not exist. Run the payment schema script first.', 1;
IF OBJECT_ID('dbo.ServiceHistoryItems', 'U') IS NULL
    THROW 50105, 'ServiceHistoryItems does not exist. Run the payment schema script first.', 1;
IF COL_LENGTH('dbo.Orders', 'customer_email') IS NULL
    OR COL_LENGTH('dbo.Orders', 'paid_at') IS NULL
    OR COL_LENGTH('dbo.Orders', 'payment_method') IS NULL
    THROW 50106, 'Orders is missing customer or payment columns. Run the order/payment schema scripts first.', 1;
IF COL_LENGTH('dbo.OrderItems', 'item_status') IS NULL
    OR COL_LENGTH('dbo.OrderItems', 'requested_quantity') IS NULL
    THROW 50107, 'OrderItems workflow columns are missing. Run add_order_items_and_workflow.sql first.', 1;
IF COL_LENGTH('dbo.ServiceHistory', 'customer_email') IS NULL
    OR COL_LENGTH('dbo.ServiceHistory', 'loyalty_points_earned') IS NULL
    THROW 50108, 'ServiceHistory is missing customer or loyalty columns. Run the payment/loyalty schema scripts first.', 1;

IF EXISTS (
    SELECT 1
    FROM dbo.Orders
    WHERE LEFT(note, LEN(@tagPrefix)) = @tagPrefix
)
    THROW 50109, 'This March-to-current-day seed already exists. It will not insert duplicate dashboard data.', 1;

SELECT TOP (1) @managerId = u.user_id
FROM dbo.Users u
JOIN dbo.Roles r ON r.role_id = u.role_id
WHERE r.role_name = N'Manager' AND u.is_active = 1 AND u.is_deleted = 0
ORDER BY u.user_id;

SELECT TOP (1) @serverId = u.user_id
FROM dbo.Users u
JOIN dbo.Roles r ON r.role_id = u.role_id
WHERE r.role_name = N'Server' AND u.is_active = 1 AND u.is_deleted = 0
ORDER BY u.user_id;

IF @managerId IS NULL
    THROW 50110, 'Create an active Manager account before running this seed.', 1;
IF @serverId IS NULL
    SET @serverId = @managerId;

SELECT @tableCount = COUNT(*)
FROM dbo.RestaurantTables
WHERE table_name NOT LIKE N'DEMO-%';

IF @tableCount < 20
    THROW 50111, 'At least 20 existing non-demo restaurant tables are required. No tables will be created by this script.', 1;

SELECT @dishCount = COUNT(*)
FROM dbo.Dish d
JOIN dbo.Category c ON c.category_id = d.category_id
WHERE d.status = 1 AND c.status = 1;

IF @dishCount < 4
    THROW 50112, 'At least four active dishes are required to create varied daily orders.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    CREATE TABLE #Numbers (
        n INT NOT NULL PRIMARY KEY
    );

    ;WITH numbers AS (
        SELECT 1 AS n
        UNION ALL
        SELECT n + 1
        FROM numbers
        WHERE n < 100
    )
    INSERT INTO #Numbers (n)
    SELECT n
    FROM numbers
    OPTION (MAXRECURSION 100);

    CREATE TABLE #SeedTables (
        table_no INT NOT NULL PRIMARY KEY,
        table_id INT NOT NULL UNIQUE
    );

    INSERT INTO #SeedTables (table_no, table_id)
    SELECT ROW_NUMBER() OVER (ORDER BY t.table_id), t.table_id
    FROM dbo.RestaurantTables t
    WHERE t.table_name NOT LIKE N'DEMO-%';

    CREATE TABLE #ActiveDishes (
        dish_no INT NOT NULL PRIMARY KEY,
        dish_id INT NOT NULL,
        dish_name NVARCHAR(150) NOT NULL,
        unit NVARCHAR(50) NULL,
        price DECIMAL(12, 0) NOT NULL
    );

    INSERT INTO #ActiveDishes (dish_no, dish_id, dish_name, unit, price)
    SELECT ROW_NUMBER() OVER (ORDER BY c.category_id, d.dish_id),
           d.dish_id, d.dish_name, d.unit, d.price
    FROM dbo.Dish d
    JOIN dbo.Category c ON c.category_id = d.category_id
    WHERE d.status = 1 AND c.status = 1;

    CREATE TABLE #Calendar (
        day_no INT NOT NULL PRIMARY KEY,
        service_date DATE NOT NULL UNIQUE
    );

    ;WITH calendar AS (
        SELECT 1 AS day_no, @startDate AS service_date
        UNION ALL
        SELECT day_no + 1, DATEADD(day, 1, service_date)
        FROM calendar
        WHERE service_date < @endDate
    )
    INSERT INTO #Calendar (day_no, service_date)
    SELECT day_no, service_date
    FROM calendar
    OPTION (MAXRECURSION 400);

    CREATE TABLE #Guests (
        guest_no INT NOT NULL PRIMARY KEY,
        customer_name NVARCHAR(100) NOT NULL,
        customer_email NVARCHAR(100) NOT NULL,
        customer_phone NVARCHAR(20) NOT NULL
    );

    INSERT INTO #Guests (guest_no, customer_name, customer_email, customer_phone)
    SELECT n,
           CONCAT(N'Khách demo ', RIGHT(CONCAT(N'000', n), 3)),
           CONCAT(N'dashboard_2026_guest_', RIGHT(CONCAT(N'000', n), 3), N'@example.com'),
           CONCAT(N'090', RIGHT(CONCAT(N'00000000', n), 8))
    FROM #Numbers
    WHERE n <= 60;

    CREATE TABLE #OrderPlan (
        day_no INT NOT NULL,
        service_no INT NOT NULL,
        service_date DATE NOT NULL,
        table_id INT NOT NULL,
        customer_name NVARCHAR(100) NOT NULL,
        customer_email NVARCHAR(100) NOT NULL,
        customer_phone NVARCHAR(20) NOT NULL,
        order_note NVARCHAR(100) NOT NULL PRIMARY KEY,
        payment_method VARCHAR(10) NOT NULL,
        created_at DATETIME2(0) NOT NULL,
        paid_at DATETIME2(0) NOT NULL,
        total_amount DECIMAL(14, 0) NULL
    );

    INSERT INTO #OrderPlan
        (day_no, service_no, service_date, table_id, customer_name, customer_email,
         customer_phone, order_note, payment_method, created_at, paid_at)
    SELECT cal.day_no,
           order_no.n,
           cal.service_date,
           tables.table_id,
           guests.customer_name,
           guests.customer_email,
           guests.customer_phone,
           CONCAT(@tagPrefix, CONVERT(CHAR(8), cal.service_date, 112), N'_',
                  RIGHT(CONCAT(N'00', order_no.n), 2)),
           CASE WHEN (cal.day_no + order_no.n) % 2 = 0 THEN 'CASH' ELSE 'QR' END,
           times.created_at,
           timing.paid_at
    FROM #Calendar cal
    JOIN #Numbers order_no
      ON order_no.n <= 10 + ((cal.day_no - 1) % 11)
    JOIN #SeedTables tables
      ON tables.table_no = (((cal.day_no - 1) * 20 + order_no.n - 1) % @tableCount) + 1
    JOIN #Guests guests
      ON guests.guest_no = ((cal.day_no * 7 + order_no.n * 3 - 1) % 60) + 1
    CROSS APPLY (
        SELECT 10 + ((cal.day_no - 1) % 11) AS daily_order_count
    ) daily
    CROSS APPLY (
        SELECT CASE
            WHEN cal.service_date = @today
                THEN DATEADD(minute, -((daily.daily_order_count - order_no.n) * 2), @now)
            ELSE DATEADD(minute, 35 + ((cal.day_no + order_no.n * 13) % 56),
                         DATEADD(minute, 600 + (order_no.n - 1) * 35,
                                 CONVERT(DATETIME2(0), cal.service_date)))
        END AS paid_at
    ) timing
    CROSS APPLY (
        SELECT CASE
            WHEN cal.service_date = @today
                THEN DATEADD(minute, -35, timing.paid_at)
            ELSE DATEADD(minute, 600 + (order_no.n - 1) * 35,
                         CONVERT(DATETIME2(0), cal.service_date))
        END AS created_at
    ) times;

    CREATE TABLE #OrderLines (
        order_note NVARCHAR(100) NOT NULL,
        line_no INT NOT NULL,
        dish_id INT NOT NULL,
        dish_name NVARCHAR(150) NOT NULL,
        unit NVARCHAR(50) NULL,
        unit_price DECIMAL(12, 0) NOT NULL,
        quantity INT NOT NULL,
        line_total DECIMAL(14, 0) NOT NULL,
        PRIMARY KEY (order_note, line_no)
    );

    INSERT INTO #OrderLines
        (order_note, line_no, dish_id, dish_name, unit, unit_price, quantity, line_total)
    SELECT op.order_note,
           line_no.n,
           dish.dish_id,
           dish.dish_name,
           dish.unit,
           dish.price,
           1 + ((op.day_no + op.service_no * 3 + line_no.n * 5) % 4),
           dish.price * (1 + ((op.day_no + op.service_no * 3 + line_no.n * 5) % 4))
    FROM #OrderPlan op
    JOIN #Numbers line_no
      ON line_no.n <= 2 + ((op.day_no + op.service_no) % 3)
    JOIN #ActiveDishes dish
      ON dish.dish_no = ((op.day_no * 7 + op.service_no * 3 + line_no.n * 11 - 1) % @dishCount) + 1;

    UPDATE op
    SET total_amount = totalsByOrder.total_amount
    FROM #OrderPlan op
    JOIN (
        SELECT order_note, SUM(line_total) AS total_amount
        FROM #OrderLines
        GROUP BY order_note
    ) totalsByOrder ON totalsByOrder.order_note = op.order_note;

    CREATE TABLE #NewOrders (
        order_id INT NOT NULL,
        order_note NVARCHAR(100) NOT NULL
    );

    INSERT INTO dbo.Orders
        (table_id, created_by, customer_name, customer_phone, customer_email,
         order_status, total_amount, note, created_at, closed_at, paid_at,
         paid_by, payment_method, cash_received, cash_change)
    OUTPUT INSERTED.order_id, INSERTED.note
        INTO #NewOrders (order_id, order_note)
    SELECT op.table_id,
           @serverId,
           op.customer_name,
           op.customer_phone,
           op.customer_email,
           'PAID',
           op.total_amount,
           op.order_note,
           op.created_at,
           op.paid_at,
           op.paid_at,
           @managerId,
           op.payment_method,
           CASE WHEN op.payment_method = 'CASH' THEN op.total_amount ELSE NULL END,
           CASE WHEN op.payment_method = 'CASH' THEN 0 ELSE NULL END
    FROM #OrderPlan op;

    INSERT INTO dbo.OrderItems
        (order_id, dish_id, dish_name, unit, unit_price, quantity, requested_quantity,
         line_total, item_status)
    SELECT orders.order_id,
           lines.dish_id,
           lines.dish_name,
           COALESCE(lines.unit, N'Phần'),
           lines.unit_price,
           lines.quantity,
           lines.quantity,
           lines.line_total,
           'SERVED'
    FROM #NewOrders orders
    JOIN #OrderLines lines ON lines.order_note = orders.order_note;

    CREATE TABLE #NewHistory (
        history_id INT NOT NULL,
        table_id INT NOT NULL,
        customer_email NVARCHAR(100) NOT NULL,
        paid_at DATETIME2(0) NOT NULL
    );

    INSERT INTO dbo.ServiceHistory
        (table_id, customer_name, customer_phone, customer_email, checked_in_at, paid_at,
         payment_method, total_amount, cash_received, cash_change, paid_by, created_at,
         loyalty_customer_id, loyalty_points_used, loyalty_discount, loyalty_points_earned,
         voucher_id, voucher_discount_percent, voucher_discount)
    OUTPUT INSERTED.history_id, INSERTED.table_id, INSERTED.customer_email, INSERTED.paid_at
        INTO #NewHistory (history_id, table_id, customer_email, paid_at)
    SELECT op.table_id,
           op.customer_name,
           op.customer_phone,
           op.customer_email,
           op.created_at,
           op.paid_at,
           op.payment_method,
           op.total_amount,
           CASE WHEN op.payment_method = 'CASH' THEN op.total_amount ELSE NULL END,
           CASE WHEN op.payment_method = 'CASH' THEN 0 ELSE NULL END,
           @managerId,
           op.paid_at,
           NULL,
           0,
           0,
           0,
           NULL,
           0,
           0
    FROM #OrderPlan op;

    INSERT INTO dbo.ServiceHistoryItems
        (history_id, dish_name, unit, unit_price, quantity, line_total)
    SELECT history.history_id,
           lines.dish_name,
           COALESCE(lines.unit, N'Phần'),
           lines.unit_price,
           lines.quantity,
           lines.line_total
    FROM #NewHistory history
    JOIN #OrderPlan op
      ON op.table_id = history.table_id
     AND op.customer_email = history.customer_email
     AND op.paid_at = history.paid_at
    JOIN #OrderLines lines ON lines.order_note = op.order_note;

    CREATE TABLE #DailySummary (
        service_date DATE NOT NULL PRIMARY KEY,
        paid_orders INT NOT NULL,
        served_tables INT NOT NULL,
        revenue DECIMAL(18, 0) NOT NULL,
        dishes_sold INT NOT NULL
    );

    INSERT INTO #DailySummary (service_date, paid_orders, served_tables, revenue, dishes_sold)
    SELECT dailyOrders.service_date,
           dailyOrders.paid_orders,
           dailyOrders.served_tables,
           dailyOrders.revenue,
           dailyItems.dishes_sold
    FROM (
        SELECT service_date,
               COUNT(*) AS paid_orders,
               COUNT(DISTINCT table_id) AS served_tables,
               SUM(total_amount) AS revenue
        FROM #OrderPlan
        GROUP BY service_date
    ) dailyOrders
    JOIN (
        SELECT op.service_date, SUM(lines.quantity) AS dishes_sold
        FROM #OrderPlan op
        JOIN #OrderLines lines ON lines.order_note = op.order_note
        GROUP BY op.service_date
    ) dailyItems ON dailyItems.service_date = dailyOrders.service_date;

    COMMIT TRANSACTION;

    SELECT service_date, paid_orders, served_tables, revenue, dishes_sold
    FROM #DailySummary
    ORDER BY service_date;

    SELECT N'Dashboard demo data from March 2026 to current date inserted' AS result,
           MIN(service_date) AS first_day,
           MAX(service_date) AS last_day,
           COUNT(*) AS seeded_days,
           SUM(paid_orders) AS paid_orders,
           SUM(revenue) AS total_revenue,
           SUM(dishes_sold) AS dishes_sold
    FROM #DailySummary;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
