-- before running, kill any long standing transactions
-- right click DB->Reports->top Transctions by age


dbcc sqlperf(LOGSPACE);
-- may be useful to do a checkpoint here https://technet.microsoft.com/en-us/library/ms189085.aspx
-- Need to backup transactions twice with a full DB backup between to get rid of uncomitted transactions
--   Need to get the oldest record pointer needed for roll back moved forward

CHECKPOINT
BACKUP LOG TegnonEfficiency
TO DISK = 'C:\SQLBackups\TegnonEfficiency_log1.trn' 
WITH INIT; 
GO 

BACKUP DATABASE TegnonEfficiency
TO DISK = 'C:\SQLBackups\TegnonEfficiency.bak' 
GO

DBCC SQLPERF(LOGSPACE);



