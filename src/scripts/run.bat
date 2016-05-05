REM # set CLASSPATH=.;"C:\Program Files\Microsoft JDBC Driver 6.0 for SQL Server\sqljdbc_4.2\enu\sqljdbc42.jar"
REM # set CLASSPATH=.\;C:\jars\sqljdbc42.jar
REM java   -jar TegnonLoad_20160419.jar
java   -jar TegnonLoad.jar

REM sqlcmd -d TegnonEfficiency -U javaUser1 -P sHxXWij02AE4ciJre7yX -i tegnonload.sql
