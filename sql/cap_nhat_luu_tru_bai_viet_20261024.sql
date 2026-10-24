-- Stop web, select the intended database, back up first. Does not archive data itself.
SET @sports_ddl=IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='blog' AND column_name='archived')=0,'ALTER TABLE blog ADD COLUMN archived BIT NOT NULL DEFAULT 0','SELECT 1');
PREPARE sports_stmt FROM @sports_ddl; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;
