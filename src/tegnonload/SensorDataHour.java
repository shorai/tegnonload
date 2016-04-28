/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tegnonload;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author chris.rowse
 *
 * The primary purpose of this class is to maintain the SensorDataHour table
 *
 * It does so by responding to a SensorDataHalfHour update(statistic) The object
 * is singleton since we only ever deal with a single instance at a time
 *
 */
public class SensorDataHour {

    static final Logger logger = Logger.getLogger("Statistic");

    static PreparedStatement insertStatement = null;
    static PreparedStatement updateStatement = null;
    static PreparedStatement findStatement = null;

    static final String insertSql = "insert into SensorDataHour( sensorID, startTime, sensorType, RecordCount, Value,Maximum, Minimum, squares,RMS,standardDeviation) values(?,?,?,?,?,?,?,?,?,?)";
    static final String updateSql = "update SensorDataHour set  recordCount=?, Value=?,Maximum=?, Minimum=?, squares=?, RMS=?, standardDeviation=? where sensorID=? and startTime=? and sensorType=?";
    static final String findSql = "select count(*) from SensorDataHour where sensorID=? and startTime=? and sensorType=?";
    static final DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static int numInserted = 0;
    static int numUpdated = 0;
    static int numErrors = 0;
    static int numFlow = 0;
    static int numEnergy = 0;

    static SensorDataHour instance = null;

    Sensor sensor;
    //java.sql.Date startTime;
    Calendar startTime;
    //Calendar endTime; 
    int count;

    Double sum;
    Double sumSquares;
    Double max;
    Double min;

    static {
        try {
            instance = new SensorDataHour();
        } catch (SQLException exc) {
            logger.log(Level.SEVERE, "Startup cant create SensorDataHour ", exc);
        }
    }

    SensorDataHour() throws SQLException {
        insertStatement = TegnonLoad.conn.prepareStatement(insertSql);
        updateStatement = TegnonLoad.conn.prepareStatement(updateSql);
        findStatement = TegnonLoad.conn.prepareStatement(findSql);

        instance = this;
    }

    public String toString() {
        return "SensorDataHour:" + sensor + " Time:" + startTime + "Type:" + sensor.typeTID
                + " Count:" + count + " Val:" + sum + " max:" + max
                + " min:" + min + "squares:" + sumSquares;
    }

    static void zeroStat() {
        numInserted = 0;
        numUpdated = 0;
        numErrors = 0;
        numFlow = 0;
        numEnergy = 0;
    }

    /**
     * Finds the matching half hour record if it exists Adds the values and
     * updates or creates a new from called parameter
     *
     * The messages may not be processed in order, particularly if we rerun some
     * email files. The strategy is therefor to look for th data fro the other
     * half hour and use the found data as the basis for further calculation
     *
     * @param stat
     */
    public void addHalfHour(Statistic stat) { //throws SQLException {

        Statistic other;
        Calendar t = stat.startTime;

        if (t.get(Calendar.MINUTE) == 0) {
            t.set(Calendar.MINUTE, 30);
        } else {
            t.set(Calendar.MINUTE, 0);
        }
        int update = 1;
        try {
            other = new Statistic(stat.sensor.id, t, stat.sensor.typeTID);
        } catch (SQLException exc) {
            update = 0;
            other = new Statistic(stat.sensor, t);
        }

        sensor = stat.sensor;
        startTime = stat.startTime;
        count = stat.count + other.count;
        sum = (stat.sum + other.sum)/count;
        sumSquares = stat.sumSquares + other.sumSquares;
        try {
            if (count > 0) {
                // @TODO: Why do we get here with zero count??
                if (update == 1) {
                    updateSql();
                } else {
                    insertSql();
                }
                if ((sensor.typeTID == 19) || (sensor.typeTID == 20)) {
                    numEnergy++;
                } else {
                    numFlow++;
                }
            } else {
                logger.info("SensorDataHour Count is Zero" + toString());
            }
        } catch (SQLException sexc) {
            logger.info("SensorDataHour:" + toString());
            logger.log(Level.SEVERE, "" + sexc.getMessage(), sexc);
            numErrors++;
        }
    }

    public void insertSql() throws SQLException {
        if (insertStatement == null) {
            insertStatement = TegnonLoad.conn.prepareStatement(insertSql);
        }
        int i = 1;
        java.util.Date dt = startTime.getTime();
        java.sql.Timestamp ts = new java.sql.Timestamp(dt.getTime());
        if (max == null) {
            max = 0.00;
        }
        if (min == null) {
            min = 0.00;
        }
       
        insertStatement.setInt(i++, sensor.id);
        insertStatement.setTimestamp(i++, ts);
        insertStatement.setInt(i++, sensor.typeTID);
        insertStatement.setInt(i++, count);
        insertStatement.setDouble(i++, sum);
        insertStatement.setDouble(i++, sumSquares);
        insertStatement.setDouble(i++, max);
        insertStatement.setDouble(i++, min);

        Double rms = 0.00;
        Double sd = 0.00;
        if (count > 0) {
            Math.sqrt(sumSquares / count);
            Math.sqrt(Math.abs(sumSquares - sum * sum) / count);
        }
        insertStatement.setDouble(i++, rms);
        insertStatement.setDouble(i++, sd);
//java.sql.SQLException: The incoming tabular data stream (TDS) remote procedure call (RPC) protocol stream 
//is incorrect. Parameter 11 (""): The supplied value is not a valid instance of data type float. Check the source
//data for invalid values. An example of an invalid value is data of numeric type with scale greater than precision.        
        insertStatement.execute();
        numInserted++;
    }

    public void updateSql() throws SQLException {
        if (updateStatement == null) {
            updateStatement = TegnonLoad.conn.prepareStatement(updateSql);
        }
        int i = 1;
        java.util.Date dt = startTime.getTime();
        java.sql.Timestamp ts = new java.sql.Timestamp(dt.getTime());
        updateStatement.setInt(i++, count);
        updateStatement.setDouble(i++, sum);
        updateStatement.setDouble(i++, sumSquares);
        updateStatement.setDouble(i++, max);
        updateStatement.setDouble(i++, min);
        Double rms = 0.00;
        Double sd = 0.00;
        if (count > 0) {
            Math.sqrt(sumSquares / count);
            Math.sqrt(Math.abs(sumSquares - sum * sum) / count);
        }
        updateStatement.setDouble(i++, rms);
        updateStatement.setDouble(i++, sd);

        updateStatement.setInt(i++, sensor.id);
        updateStatement.setTimestamp(i++, ts);
        updateStatement.setInt(i++, sensor.typeTID);

        updateStatement.execute();
        numUpdated++;
    }
}
