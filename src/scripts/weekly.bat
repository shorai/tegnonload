echo "*********************************************************************************************************************** START  " 
date /T
Time /T
REM # set CLASSPATH=.;"C:\Program Files\Microsoft JDBC Driver 6.0 for SQL Server\sqljdbc_4.2\enu\sqljdbc42.jar"
REM # set CLASSPATH=.\;C:\jars\sqljdbc42.jar
REM java   -jar TegnonLoad.jar

sqlcmd -d TegnonEfficiency -i SQLLogBackupAndTrunc.sql


REM - at this point we should zip the logs and move them to s3 storage


date /T
Time /T
echo "*********************************************************************************************************************** FINISHED  " 
