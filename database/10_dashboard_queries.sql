-- SQL Server read-only reporting examples. Revenue = net sales of COMPLETED orders.
-- Window endpoints are Vietnam local dates; end is exclusive at the following midnight.
USE f6s_shop;
GO
DECLARE @fromLocal date = '2026-10-01', @toLocal date = '2026-10-31';
DECLARE @fromUtc datetimeoffset = TODATETIMEOFFSET(CAST(@fromLocal AS datetime2), '+07:00');
DECLARE @toUtc datetimeoffset = TODATETIMEOFFSET(DATEADD(day, 1, CAST(@toLocal AS datetime2)), '+07:00');

SELECT CAST(SWITCHOFFSET(completed_at, '+07:00') AS date) AS sales_day,
       SUM(total) AS net_revenue, COUNT_BIG(*) AS completed_orders
FROM dbo.orders
WHERE status = N'COMPLETED' AND completed_at >= @fromUtc AND completed_at < @toUtc
GROUP BY CAST(SWITCHOFFSET(completed_at, '+07:00') AS date)
ORDER BY sales_day;

SELECT CONVERT(char(7), SWITCHOFFSET(completed_at, '+07:00'), 126) AS sales_month,
       SUM(total) AS net_revenue, COUNT_BIG(*) AS completed_orders
FROM dbo.orders
WHERE status = N'COMPLETED' AND completed_at >= @fromUtc AND completed_at < @toUtc
GROUP BY CONVERT(char(7), SWITCHOFFSET(completed_at, '+07:00'), 126)
ORDER BY sales_month;

SELECT COUNT_BIG(*) AS created_orders,
       COALESCE(SUM(CASE WHEN status = N'COMPLETED' THEN CAST(1 AS bigint) ELSE 0 END), 0) AS successful_orders,
       COALESCE(100.0 * SUM(CASE WHEN status = N'COMPLETED' THEN 1 ELSE 0 END)
                / NULLIF(COUNT_BIG(*), 0), 0) AS success_rate_percent
FROM dbo.orders
WHERE created_at >= @fromUtc AND created_at < @toUtc;

SELECT TOP (10) p.id, p.code, p.name, SUM(CAST(i.quantity AS bigint)) AS sold_quantity,
       SUM(i.unit_price * i.quantity) AS gross_merchandise_before_order_discount
FROM dbo.order_items i
JOIN dbo.orders o ON o.id = i.order_id
JOIN dbo.product_variants v ON v.id = i.variant_id
JOIN dbo.products p ON p.id = v.product_id
WHERE o.status = N'COMPLETED' AND o.completed_at >= @fromUtc AND o.completed_at < @toUtc
GROUP BY p.id, p.code, p.name
ORDER BY sold_quantity DESC, p.id ASC;
