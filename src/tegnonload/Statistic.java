/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tegnonload;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
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

    static final String insertSql = "insert into SensorDataHalfHour(AttachmentID, sensorId, startTime, sensorType, recordCount, sensorValue,maximum, minimum, sumOfSquares) values(?,?,?,?,?,?,?,?,?)";
    static final String updateSql = "update SensorDataHalfHour set attachmentID=?,  recordCount=?, sensorValue=?,maximum=?, minimum=?, sumOfSquares=? where sensorId=? and startTime=? and sensorType=?";
    static final String findSql = "select count(*) from SensorDataHalfHour where sensorID=? and startTime=? and sensorType=?";
    static final DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static int numInserted = 0;
    static int numUpdated = 0;
    static int numErrors = 0;

    Sensor sensor;
    //java.sql.Date startTime;
    java.sql.Timestamp startTime;
    java.sql.Timestamp endTime;

    Double first;
    Double last;
    Double sum;
    Double sumSquares;
    Double max;
    Double min;
    int count;

    static {
        logger.addHandler(TegnonLoad.logHandler);
    }

    public Statistic(Sensor s) {
        sensor = s;
        zero();

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

    public void zero() {
        first = 0.0;
        last = 0.0;
        sum = 0.0;
        sumSquares = 0.0;
        max = 0.0;
        min = 0.0;
        count = 0;

        numInserted = 0;
        numUpdated = 0;
        numErrors = 0;

    }

    public void add(String time, Double value) throws Exception {

        java.util.Date date = df.parse(time);

        java.sql.Timestamp sDate = new java.sql.Timestamp(date.getTime());

        if (count == 0) {
            first = value;
            min = value;
            startTime = sDate;
            endTime = sDate;
        } else {
            last = value;
            if (date.compareTo(startTime) < 0) {
                startTime = sDate;
            }
            if (date.compareTo(endTime) > 0) {
                endTime = sDate;
            }
        }
        sum += value;
        sumSquares += (value * value);
        if (max < value) {
            max = value;
        }
        if (min > value) {
            min = value;
        }
        count++;
    }

    static String getSqlStat() {
        return "Inserted:" + numInserted + " Updated:" + numUpdated + " Errors:" + numErrors;
    }

    String dump() {
        String timeStr = startTime.toString();
        try {
            timeStr = df.format(startTime);
        } catch (Exception ext) {
            logger.severe(timeStr + " df.format " + ext.getMessage() );
        }
        return sensor.key() + "\t" + df.format(startTime) + "\t" + df.format(endTime) 
                + "\t First:"  + first + "\tLast:" + last + "\tSum:" + sum 
                + "\tMax:" + max + "\tMin:" + min + "\tCount:" + count;
    }

    void toDb(Integer messageId, Integer sensorId, java.sql.Timestamp date, Integer sensorType, Integer recordCount,
            Double sensorValue, Double sumOfSquares, Double max, Double min) {

        try {
            // look for record on DB
            findStatement.setInt(1, sensorId);
            findStatement.setTimestamp(2, date);
            findStatement.setInt(3, sensorType);

            ResultSet rs = findStatement.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            int i = 1;
            if (count == 0) {
                if (TegnonLoad.UPDATE_DATA) {
                    insertStatement.setInt(i++, messageId);
                    insertStatement.setInt(i++, sensorId);
                    insertStatement.setTimestamp(i++, date);
                    insertStatement.setInt(i++, sensorType);
                    insertStatement.setInt(i++, recordCount);
                    insertStatement.setDouble(i++, sensorValue);
                    insertStatement.setDouble(i++, sumOfSquares);
                    insertStatement.setDouble(i++, max);
                    insertStatement.setDouble(i++, min);
                    insertStatement.executeUpdate();
                   // logger.log(Level.FINEST,"InsertRecord " + dump());
                } else {
                 logger.log(Level.INFO,"Dummy InsertRecord " + dump());
                    
                }
                numInserted++;

            } else {
                if (TegnonLoad.UPDATE_DATA) {
                    updateStatement.setInt(i++, messageId);
                    updateStatement.setInt(i++, recordCount);
                    updateStatement.setDouble(i++, sensorValue);
                    updateStatement.setDouble(i++, sumOfSquares);
                    updateStatement.setDouble(i++, max);
                    updateStatement.setDouble(i++, min);

                    updateStatement.setInt(i++, sensorId);
                    updateStatement.setTimestamp(i++, date);
                    updateStatement.setInt(i++, sensorType);

                    updateStatement.executeUpdate();
                    //logger.log(Level.FINEST,"Updated Record " + dump());
                } else {
                    logger.log(Level.INFO,"Dummy UpdateRecord " + dump());
                }
                numUpdated++;
            }
        } catch (Exception exc) {
            logger.log(Level.SEVERE,"Statistic.toDB():SQL error "+exc.getMessage(),exc);
            numErrors++;
        }

    }

    void writeFlowSQL(Integer messageId) {
        if (sum > 0.00) {
            toDb(messageId, sensor.id, startTime, sensor.typeTID, count, sum, max, min, sumSquares);
           //  logger.log(Level.INFO,"STat.writeFlow  " +  dump());
        }
    }

    void writeEnergySQL(Integer messageId) {

        Double val = last - first;
        if (val < 0) {
            val += 32767.0;
        }
        if (val > 0.00) {
            toDb(messageId, sensor.id, startTime, 19, 60, val, last, first, val * val);
            logger.log(Level.INFO,"Stat.writeen Energy  " +  dump() + " Energy Calc:" + val);
        } else  {
            if (val < 1.00)
            try { //if (startTime != null)
                logger.log(Level.WARNING,"Stat.writeEnergy less than zero " + val + " " +  dump());
            } catch (Exception exx) {
                    logger.severe("Stat.writeEnergy Exception"+exx.getMessage());
                            
                    }
        }
    }

}
