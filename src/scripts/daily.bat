echo "*********************************************************************************************************************** START  " 
date /T
Time /T

REM # set CLASSPATH=.;"C:\Program Files\Microsoft JDBC Driver 6.0 for SQL Server\sqljdbc_4.2\enu\sqljdbc42.jar"
REM # set CLASSPATH=.\;C:\jars\sqljdbc42.jar
REM java   -jar TegnonLoad.jar
del c:\SQLBAckups\TegnonEfficiency_prev.bak
del c:\SQLBAckups\TegnonEfficiency_log1_prev.trn

ren c:\SQLBAckups\TegnonEfficiency.bak TegnonEfficiency_prev.bak
ren c:\SQLBAckups\TegnonEfficiency_log1.trn  TegnonEfficiency_log1_prev.trn

sqlcmd -d TegnonEfficiency -i SQLLogBackup.sql 


REM - at this point we should zip the logs and move them to s3 storage
REM - since windows does not have a command line compression tool, and the batches proposed on stackechange are obtuse
REM -  I will leave this as an exercise for the future
REM - perhaps the s3 utilities have such functionality
REM echo Archive_%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%.zip
REM c:\"program files"\7-zip\7z.exe a c:\SQLBackups\TegnonEfficiency_%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%.zip c:\SQLBackups\log.txt c:\SQLBackups\TegnonEfficiency.bak c:\SQLBackups\TegnonEfficiency_log1.trn
c:\"program files"\7-zip\7z.exe a c:\SQLBackups\TegnonEfficiency_%date:~-4,4%%date:~-10,2%%date:~-7,2%.zip c:\SQLBackups\log.txt c:\SQLBackups\TegnonEfficiency.bak c:\SQLBackups\TegnonEfficiency_log1.trn

REM aws s3 cp c:\SQLBackups\TegnonEfficiency_%date:~-4,4%%date:~-10,2%%date:~-7,2%.zip s3://t
aws s3 cp c:/sqlBackups/TegnonEfficiency_%date:~-4,4%%date:~-10,2%%date:~-7,2%.zip s3://tegnon/tegnonEfficiency/mssql --profile b2
aws s3 sync ./* s3://tegnon/tegnonEfficiency/scripts --profile b2
date /T
Time /T
echo "*********************************************************************************************************************** FINISH  " + date
