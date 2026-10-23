-- Stop the web and back up the selected database before importing.
-- Do not infer customer ownership from legacy staff user_id or matching email.
SET @f6s_customer_column = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='order_draft' AND column_name='customer_id')=0,
  'ALTER TABLE order_draft ADD COLUMN customer_id BIGINT NULL', 'SELECT 1');
PREPARE f6s_customer_stmt FROM @f6s_customer_column;
EXECUTE f6s_customer_stmt;
DEALLOCATE PREPARE f6s_customer_stmt;
SET @f6s_customer_column = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='invoice' AND column_name='customer_id')=0,
  'ALTER TABLE invoice ADD COLUMN customer_id BIGINT NULL', 'SELECT 1');
PREPARE f6s_customer_stmt FROM @f6s_customer_column;
EXECUTE f6s_customer_stmt;
DEALLOCATE PREPARE f6s_customer_stmt;
