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

    static final Logger logger = Logger.getLogger("Stitistic");

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

    static {
        logger.setLevel(Level.INFO);
        logger.addHandler(TegnonLoad.logHandler);
    }

    public Statistic(Sensor s) {
        sensor = s;
        zero();

    }
    
    public Statistic(int sensorId, Calendar startTime, int sensorType) throws SQLException {
        zero();
       java.util.Date dt = startTime.getTime();
        java.sql.Timestamp ts = new java.sql.Timestamp(dt.getTime());
        loadStatement.setInt(1,sensorId);
        loadStatement.setTimestamp(1,ts);
        loadStatement.setInt(1,sensorType);
        
        ResultSet rs = loadStatement.executeQuery();
        int i = 1;
        if (rs.next()) {
                this.startTime = startTime;
                sensorId = sensorId;
                sensorType = sensorType;
                count = rs.getInt(i++);
                sum = rs.getDouble(i++);
                max = rs.getDouble(i++);
                min = rs.getDouble(i++);
                
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
                startTime.set(Calendar.MINUTE, 0);
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
            timeStr = df.format(startTime);
        } catch (Exception ext) {
            logger.severe(timeStr + " df.format " + ext.getMessage());
        }
        return sensor.key() + "\t" + df.format(startTime) + "\t" + df.format(endTime)
                + "\t First:" + first + "\tLast:" + last + "\tSum:" + sum
                + "\tMax:" + max + "\tMin:" + min + "\tCount:" + count;
    }

    void toDb(Integer messageId, Integer sensorId, Calendar cal, Integer sensorType, Integer recordCount,
            Double sensorValue, Double sumOfSquares, Double max, Double min) {

      
        java.sql.Timestamp ts = new java.sql.Timestamp(cal.getTimeInMillis());
        
        try {
            // look for record on DB
            findStatement.setInt(1, sensorId);
            findStatement.setTimestamp(2, ts);
            findStatement.setInt(3, sensorType);

            ResultSet rs = findStatement.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            int i = 1;
            if (count == 0) {
                if (TegnonLoad.UPDATE_DATA) {
                    insertStatement.setInt(i++, messageId);
                    insertStatement.setInt(i++, sensorId);
                    insertStatement.setTimestamp(i++, ts);
                    insertStatement.setInt(i++, sensorType);
                    insertStatement.setInt(i++, recordCount);
                    insertStatement.setDouble(i++, sensorValue);
                    insertStatement.setDouble(i++, sumOfSquares);
                    insertStatement.setDouble(i++, max);
                    insertStatement.setDouble(i++, min);
                    insertStatement.setDouble(i++, sumOfSquares / recordCount);
                    insertStatement.setDouble(i++, (sumOfSquares - sensorValue * sensorValue) / recordCount);

                    insertStatement.executeUpdate();
                    logger.log(Level.FINE, "InsertRecord " + dump());
                } else {
                    logger.log(Level.INFO, "Dummy InsertRecord " + dump());

                }
                numInserted++;

            } else {
                if (TegnonLoad.UPDATE_DATA) {
                    updateStatement.setInt(i++, messageId);
                    updateStatement.setInt(i++, recordCount);
                    updateStatement.setDouble(i++, sensorValue);
                    updateStatement.setDouble(i++, max);
                    updateStatement.setDouble(i++, min);
                    updateStatement.setDouble(i++, sumOfSquares);
                    insertStatement.setDouble(i++, sumOfSquares / recordCount);
                    insertStatement.setDouble(i++, (sumOfSquares - sensorValue * sensorValue) / recordCount);

                    updateStatement.setInt(i++, sensorId);
                    updateStatement.setTimestamp(i++, ts);
                    updateStatement.setInt(i++, sensorType);

                    int cnt = updateStatement.executeUpdate();

                    if (cnt == 1) {
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
            logger.log(Level.SEVERE, "Statistic.toDB():SQL error " + exc.getMessage(), exc);
            numErrors++;
        }

    }

    void writeFlowSQL(Integer messageId) {
        if (sum > 0.00) {
            toDb(messageId, sensor.id, startTime, sensor.typeTID, count, sum, max, min, sumSquares);
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
            this.sum = val;
            this.sumSquares = val * val;
            toDb(messageId, sensor.id, startTime, 19, 60, val, val * val, last, first);
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
