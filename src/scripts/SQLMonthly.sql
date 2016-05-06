-- before running, kill any long standing transactions
-- right click DB->Reports->top Transctions by age
-- run this once per week or so 

dbcc sqlperf(LOGSPACE);
-- may be useful to do a checkpoint here https://technet.microsoft.com/en-us/library/ms189085.aspx
-- Need to backup transactions twice with a full DB backup between to get rid of uncomitted transactions
--   Need to get the oldest record pointer needed for roll back moved forward

CHECKPOINT
BACKUP LOG TegnonEfficiency
TO DISK = 'C:\SQLBackups\TegnonEfficiency_monthly_log1.trn' 
WITH INIT; 
GO 

BACKUP DATABASE TegnonEfficiency
TO DISK = 'C:\SQLBackups\TegnonEfficiency_monthly.bak' 


DBCC SQLPERF(LOGSPACE);

BACKUP LOG TegnonEfficiency
TO DISK = 'C:\SQLBackups\TegnonEfficiency_monthly_log2.trn' 
WITH INIT; 
GO 
-- may be useful to do a checkpoint here https://technet.microsoft.com/en-us/library/ms189085.aspx
CHECKPOINT
GO
-- REmove the emrgency log file if it exists
--ALTER DATABASE TegnonEfficiency REMOVE FILE TegnonEfficiency_Log2; 

DBCC SQLPERF(LOGSPACE);
-- Backup a third time to move transaction records to the front of the log
-- GArbage collection is all but useless in microsoft
BACKUP LOG TegnonEfficiency
TO DISK = 'C:\SQLBackups\TegnonEfficiency_monthly_log3.trn' 
WITH INIT; 
GO 

DBCC SQLPERF(LOGSPACE);

-- finally we can shink the beast
DBCC SHRINKFILE (N'TegnonEfficiency_log' , target_size=0); 
DBCC SHRINKFILE(N'TegnonEfficiency_log2' , target_size=0);
-- finally we can shink and remove the beast
-- REmove the emrgency log file if it exists
ALTER DATABASE TegnonEfficiency REMOVE FILE TegnonEfficiency_Log2; 
GO
DBCC SQLPERF(LOGSPACE);
-- if this doesn't work get nasty with microsoft
/*
ALTER DATABASE TegnonEfficiency SET RECOVERY SIMPLE;
DBCC SHRINKFILE(N'TegnonEfficiency_log' , target_size=0);
ALTER DATABASE TegnonEfficiency SET RECOVERY FULL;
DBCC SQLPERF(LOGSPACE);
*/

CHECKPOINT
-- should now rebuild all indices
EXEC sp_MSforeachtable @command1="print '?' DBCC DBREINDEX ('?', ' ', 80)"
GO
EXEC sp_updatestats
GO
CHECKPOINT


