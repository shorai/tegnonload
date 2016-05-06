echo "*********************************************************************************************************************** START  " 
date /T
Time /T
del c:\SQLBackups\*.*

sqlcmd -d TegnonEfficiency -i SQLMonthly.sql

del c:\sqlBackups\TegnonEfficiency_monthly_*.zip
REM - at this point we zip the logs and move them to s3 storage
REM - we first clear the backup directory to stop it growing
REM del c:/sqlBackups/*.*

REM should include all scipts and wwwroot
REM c:\"program files"\7-zip\7z.exe a c:\SQLBackups\TegnonEfficiency_monthly_%date:~-4,4%%date:~-10,2%%date:~-7,2%.zip c:\SQLBackups\log.txt c:\SQLBackups\TegnonEfficiency_monthly_*.* c:\inetpub\wwwroot "c:\users\chris.rowse\my documents\NetbeansProjects\tegnonload"
c:\"program files"\7-zip\7z.exe a c:\SQLBackups\TegnonEfficiency_monthly_%date:~-4,4%%date:~-10,2%%date:~-7,2%.zip c:\SQLBackups\log.txt c:\SQLBackups\TegnonEfficiency_monthly.bak c:\SQLBackups\TegnonEfficiency_monthly_log1.trn c:\SQLBackups\TegnonEfficiency_monthly_log2.trn c:\SQLBackups\TegnonEfficiency_monthly_log3.trn

REM aws s3 cp c:\SQLBackups\TegnonEfficiency_%date:~-4,4%%date:~-10,2%%date:~-7,2%.zip s3://t
aws s3 cp c:/sqlBackups/TegnonEfficiency_monthly_%date:~-4,4%%date:~-10,2%%date:~-7,2%.zip s3://tegnon/tegnonEfficiency/monthly/ --profile b2

REM this should copy to glacier storage automatically after 10 days - Keep for 24 months



date /T
Time /T
echo "*********************************************************************************************************************** FINISHED  " 