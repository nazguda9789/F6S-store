-- Back up and stop web first. Add nullable cash metadata; no OTP/VNPAY changes.
SET @f6s_cash_sql=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='invoice' AND column_name='cash_received')=0,'ALTER TABLE invoice ADD COLUMN cash_received DECIMAL(19,0) NULL','SELECT 1');
PREPARE f6s_cash_stmt FROM @f6s_cash_sql; EXECUTE f6s_cash_stmt; DEALLOCATE PREPARE f6s_cash_stmt;
SET @f6s_cash_sql=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='invoice' AND column_name='cash_change')=0,'ALTER TABLE invoice ADD COLUMN cash_change DECIMAL(19,0) NULL','SELECT 1');
PREPARE f6s_cash_stmt FROM @f6s_cash_sql; EXECUTE f6s_cash_stmt; DEALLOCATE PREPARE f6s_cash_stmt;
