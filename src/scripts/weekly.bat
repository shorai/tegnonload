echo "*********************************************************************************************************************** START  " 
date /T
Time /T

sqlcmd -d TegnonEfficiency -i SQLWeekly.sql


REM - at this point we zip the logs and move them to s3 storage
REM - we first clear the backup directory to stop it growing
REM del c:\sqlBackups\*.*


c:\"program files"\7-zip\7z.exe a c:\SQLBackups\TegnonEfficiency_weekly_%date:~-4,4%%date:~-10,2%%date:~-7,2%.zip c:\SQLBackups\log.txt c:\SQLBackups\TegnonEfficiency_weekly.bak c:\SQLBackups\TegnonEfficiency_weekly_log1.trn c:\SQLBackups\TegnonEfficiency_weekly_log2.trn c:\SQLBackups\TegnonEfficiency_weekly_log3.trn
REM aws s3 cp c:\SQLBackups\TegnonEfficiency_%date:~-4,4%%date:~-10,2%%date:~-7,2%.zip s3://t
aws s3 cp c:/sqlBackups/TegnonEfficiency_weekly_%date:~-4,4%%date:~-10,2%%date:~-7,2%.zip s3://tegnon/tegnonEfficiency/weekly/ --profile b2


date /T
Time /T
echo "*********************************************************************************************************************** FINISHED  " 
