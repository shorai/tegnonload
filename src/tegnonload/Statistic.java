/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tegnonload;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chris
 */
public class Statistic {

    static final Logger logger = TegnonLoad.tegnonLogger.getLogger("tegnonload.Statistic");

    static PreparedStatement insertStatement = null;
    static PreparedStatement updateStatement = null;
    static PreparedStatement findStatement = null;
    static PreparedStatement loadStatement = null;

    static final String insertSql = "insert into SensorDataHalfHour(AttachmentID, sensorId, startTime, sensorType, recordCount, sensorValue,maximum, minimum, sumOfSquares,RMS,standardDeviation) values(?,?,?,?,?,?,?,?,?,?,?)";
    static final String updateSql = "update SensorDataHalfHour set attachmentID=?,  recordCount=?, sensorValue=?,maximum=?, minimum=?, sumOfSquares=?, RMS=?, standardDeviation=? where sensorId=? and startTime=? and sensorType=?";
    static final String findSql = "select count(*) from SensorDataHalfHour where sensorID=? and startTime=? and sensorType=?";
    static final String loadSql = "select RecordCount,SensorValue,Maximum,Minimum from SensorDataHalfHour where sensorID=? and startTime=? and sensorType=?";

    static final DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static int numInserted = 0;
    static int numUpdated = 0;
    static int numErrors = 0;
    static int numFlow = 0;
    static int numEnergy = 0;

    Sensor sensor;
    //java.sql.Date startTime;
    Calendar startTime;
    Calendar endTime;

    Double first;
    Double last;
    Double sum;
    Double sumSquares;
    Double max;
    Double min;
    int count;

    boolean onServer = false;

    static {
        // logger.setLevel(Level.INFO);
        logger.setUseParentHandlers(false);
        logger.addHandler(TegnonLoad.logHandler);
    }

    public String toString() {
        return " Statistic " + ((sensor == null) ? "Null sensor" : sensor.toString())
                + " StartTime:" + ((startTime == null) ? "Null Date" : df.format(startTime.getTime()))
                + " Value :" + ((count > 0) ? sum / count : "No Data");
    }

    /*  public Statistic(Sensor s) {
        sensor = s;
        //sensorType = s.typeTID;
        //this.startTime = startTime;
        zero();

    }
     */
    public Statistic(Sensor sensor, Calendar startTime) { //throws SQLException {

        zero();
        java.util.Date dt = startTime.getTime();
        java.sql.Timestamp ts = new java.sql.Timestamp(dt.getTime());
        this.startTime = startTime;
        this.sensor = sensor;
        zero();
        onServer = false;

        try {
            if (loadStatement == null) {
                loadStatement = TegnonLoad.conn.prepareStatement(loadSql);
            }
            loadStatement.setInt(1, sensor.id);
            loadStatement.setTimestamp(2, ts);
            loadStatement.setInt(3, sensor.typeTID);
            ResultSet rs = loadStatement.executeQuery();
            int i = 1;
            if (rs.next()) {
                count = rs.getInt(i++);
                sum = rs.getDouble(i++);
                max = rs.getDouble(i++);
                min = rs.getDouble(i++);
                onServer = true;
            }
        } catch (SQLException exc) {

            logger.warning("Statistic lookup failed: " + toString());
        }

    }

    

    static void prepare(Connection conn) {
        try {
            insertStatement = TegnonLoad.conn.prepareStatement(insertSql);
            updateStatement = TegnonLoad.conn.prepareStatement(updateSql);
            findStatement = TegnonLoad.conn.prepareStatement(findSql);

        } catch (Exception exc) {
            //System.out.println("Statistic():" + exc.getMessage());
            logger.log(Level.SEVERE, "Statistic():" + exc.getMessage(), exc);
            TegnonLoad.conn = null;
        }
    }

    static public void zeroStat() {
        numInserted = 0;
        numUpdated = 0;
        numErrors = 0;
        numFlow = 0;
        numEnergy = 0;
    }

    public void zero() {
        first = 0.0;
        last = 0.0;
        sum = 0.0;
        sumSquares = 0.0;
        max = 0.0;
        min = -1.0;
        count = 0;

    }

    public void add(String time, Double value) throws Exception {

        Calendar date = Calendar.getInstance();
        date.setTime(df.parse(time));

        //java.sql.Timestamp sDate = new java.sql.Timestamp(date.getTime());
        if (count == 0) {
            first = value;
            if (value > 0.0) {
                min = value;
            }
            startTime = date;
            endTime = date;
        } else {
            last = value;
            if (date.compareTo(startTime) < 0) {
                startTime = date;
            }
            if (date.compareTo(endTime) > 0) {
                endTime = date;
            }
        }
        sum += value;
        sumSquares += (value * value);
        if (max < value) {
            max = value;
        }
        if (value > 0.0) {
            if ((min > value) || (min <= -1.0)) {
                min = value;
            }
        }
        count++;
        SensorDataNormal.instance.save(this, date, value); //startTime);
        //     SensorDataHour.instance.addHalfHour(this); 
    }

    static String getSqlStat() {
        return "Inserted:" + numInserted + " Updated:" + numUpdated + " Errors:" + numErrors;
    }

    void setTimes() {

        startTime.set(Calendar.SECOND, 0);
        startTime.set(Calendar.MILLISECOND, 0);

        if (startTime.get(Calendar.MINUTE) < 30) {
            startTime.set(Calendar.MINUTE, 0);
        } else {
            startTime.set(Calendar.MINUTE, 30);
        }

        if (endTime == null) {
            endTime = startTime;
        }

        endTime.set(Calendar.SECOND, 59);
        endTime.set(Calendar.MILLISECOND, 999);

        if (endTime.get(Calendar.MINUTE) < 30) {
            endTime.set(Calendar.MINUTE, 29);
        } else {
            endTime.set(Calendar.MINUTE, 59);
        }

    }

    String dump() {
        String timeStr = startTime.toString();
        try {
            timeStr = df.format(startTime.getTime());
        } catch (Exception ext) {
            logger.severe(timeStr + " df.format " + ext.getMessage());
        }
        return sensor.key() + "\t" + df.format(startTime.getTime()) + "\t" + df.format(endTime.getTime())
                + "\t First:" + first + "\tLast:" + last + "\tSum:" + sum
                + "\tMax:" + max + "\tMin:" + min + "\tCount:" + count;
    }
    static final String retrySql = "update SensorDataHalfHour set attachmentID=%d,  recordCount=%d,"
            + " sensorValue=%f,maximum=%f, minimum=%f, sumOfSquares=%f, RMS=%f, standardDeviation=%f "
            + "where sensorId=%d and startTime='%s' and sensorType=%d";

    void retryUpdate(Integer messageId, Sensor sensor, Integer sensorType, Double rms, Double sd) throws SQLException {

        String sts = df.format(startTime.getTime());

        String s = String.format(retrySql, messageId, count, sum, max, min, sumSquares, rms, sd, sensor.id, sts, sensorType);

        TegnonLoad.conn.createStatement().execute(s);

        logger.info("Retry Update completed:" + s);
    }

    void toDb(Integer messageId, Sensor sensor, Integer sensorType) { //, Integer recordCount,
        //Double sensorValue, Double sumOfSquares, Double max, Double min) {

        java.sql.Timestamp ts = new java.sql.Timestamp(startTime.getTimeInMillis());
        /*
        java.sql.SQLException: Violation of PRIMARY KEY constraint 'PK_SensorDataHalfHour'. 
        Cannot insert duplicate key in object 'dbo.SensorDataHalfHour'. 
        The duplicate key value is (785, Apr 25 2016  5:00PM, 19).

         */
        int recordsOnFile = -1;
        try {
            // look for record on DB
            findStatement.setInt(1, sensor.id);
            findStatement.setTimestamp(2, ts); //new java.sql.Time(startTime.getTime().getTime()));
            findStatement.setInt(3, sensorType);

            ResultSet rs = findStatement.executeQuery();
            rs.next();
            recordsOnFile = rs.getInt(1);
            rs.close();

            logger.info("Lookup SensorDataHalfHour SensorId:" + sensor.id
                    + " Time:" + ts + " typeTID:" + sensorType + " Count:" + recordsOnFile);

            Double rms = 0.00;
            Double sd = 0.00;

            if (recordsOnFile > 0) {
                rms = Math.sqrt(sumSquares / count);
                sd = Math.sqrt(Math.abs((sumSquares - sum * sum)) / count);
            }

            sum = Math.round(sum * 10000.00) / 10000.00;
            sumSquares = Math.round(sumSquares * 10000.00) / 10000.00;

            rms = Math.round(rms * 10000.00) / 10000.00;
            sd = Math.round(sd * 10000.00) / 10000.00;

            if ((rms == null) || (rms.isNaN()) || (rms.isInfinite())) {
                logger.warning("Computed RMS value was not acceptable:" + rms);
                ;
                rms = 0.00;
            }
            if ((sd == null) || (sd.isNaN()) || (sd.isInfinite())) {
                logger.warning("Computed StandardDeviation was unacceptable:" + sd);
                sd = 0.00;
            }

            int i = 1;
            if (recordsOnFile == 0) {
                if (TegnonLoad.UPDATE_DATA) {
                    insertStatement.setInt(i++, messageId);
                    insertStatement.setInt(i++, sensor.id);
                    insertStatement.setTimestamp(i++, ts);
                    insertStatement.setInt(i++, sensorType);
                    insertStatement.setInt(i++, count);
                    insertStatement.setDouble(i++, sum);
                    insertStatement.setDouble(i++, sumSquares);
                    insertStatement.setDouble(i++, max);
                    insertStatement.setDouble(i++, min);
                    insertStatement.setDouble(i++, rms);
                    insertStatement.setDouble(i++, sd);
                    /*
SEVERE: Statistic.toDB():SQL error 
                    SensorID:785 Time:2016-04-25 20:00:00.0 Type:20 Count:0 
                    Violation of PRIMARY KEY constraint 'PK_SensorDataHalfHour'. 
                    Cannot insert duplicate key in object 'dbo.SensorDataHalfHour'. 
                    The duplicate key value is (785, Apr 25 2016  8:00PM, 19).
java.sql.SQLException: Violation of PRIMARY KEY constraint 'PK_SensorDataHalfHour'. 
                    Cannot insert duplicate key in object 'dbo.SensorDataHalfHour'. 
                    The duplicate key value is (785, Apr 25 2016  8:00PM, 19).

                     */
                    insertStatement.execute();
                    logger.log(Level.FINE, "InsertRecord " + dump());
                } else {
                    logger.log(Level.INFO, "Dummy InsertRecord " + dump());

                }
                numInserted++;

            } else {
                if (TegnonLoad.UPDATE_DATA) {
                    i = 1;
                    updateStatement.setInt(i++, messageId);
                    updateStatement.setInt(i++, count);

                    updateStatement.setDouble(i++, sum);
                    updateStatement.setDouble(i++, max);
                    updateStatement.setDouble(i++, min);
                    updateStatement.setDouble(i++, sumSquares);
                    insertStatement.setDouble(i++, rms);
                    insertStatement.setDouble(i++, sd);

                    updateStatement.setInt(i++, sensor.id);
                    updateStatement.setTimestamp(i++, ts);
                    // updateStatement.setDate(i++, startTime.getTime());
                    updateStatement.setInt(i++, sensorType);

                    //int cnt = updateStatement.executeUpdate();
                    logger.info(" Updating " + messageId + "," + count + ","
                            + sum + "," + max + "," + min + ","
                            + sumSquares + "," + rms + "," + sd + ","
                            + sensor.id + "," + ts + "," + sensorType);

                    boolean cnt = false;
                    try {
                        updateStatement.execute();
                    } catch (SQLException xx) {
                        logger.warning(" Failed update - retrying" + xx.getMessage());
                        retryUpdate(messageId, sensor, sensorType, rms, sd);
                    }

//if (cnt == 1) {
                    if (cnt) {
                        logger.log(Level.FINE, " One Energy Record updated:" + dump());
                    } else {
                        logger.log(Level.INFO, "Energy Record not updated[" + cnt + "]" + dump());
                    }
                    //logger.log(Level.FINEST,"Updated Record " + dump());
                } else {
                    logger.log(Level.INFO, "Dummy UpdateRecord " + dump());
                }
                numUpdated++;
            }

        } catch (Exception exc) {
            logger.log(Level.SEVERE, "Statistic.toDB():SQL error "
                    + " SensorID:" + sensor.id + " Time:" + ts //df.format(startTime.getTime()) 
                    + " Type:" + sensorType + " Count:" + count + " "
                    + exc.getMessage(), exc);
            numErrors++;
        }

    }

    void writeFlowSQL(Integer messageId) {
        if (sum > 0.00) {
            toDb(messageId, sensor, sensor.typeTID); //, count, sum, max, min, sumSquares);
            logger.log(Level.INFO, "Stat.writeFlow  " + dump());
            numFlow++;
        }
    }

    void writeEnergySQL(Integer messageId) {

        Double val = last - first;
        if (val < 0) {
            val += 32767.0;
        }
        if (val > 0.00) {
            //sensorType = 19;
            max = last;
            min = first;
            count = 60;
            this.sum = val;
            this.sumSquares = val * val;
            toDb(messageId, sensor, 19);
            logger.log(Level.INFO, "Stat.written Energy  " + dump() + " Energy Calc:" + val);
            numEnergy++;
        }
        /*else  {
            
            if (val < -1.00)
            try { //if (startTime != null)
               // logger.log(Level.WARNING,"Stat.writeEnergy less than zero " + val + " " +  dump());
            } catch (Exception exx) {
                    logger.severe("Stat.writeEnergy Exception"+exx.getMessage());
                            
                    }
        } */
    }

}
