/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tegnonload;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import static tegnonload.Statistic.loadStatement;
import static tegnonload.TegnonLoad.tegnonLogger;

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

    static final Logger logger = tegnonLogger.getLogger("tegnonload.SensorDataHour");

    static PreparedStatement insertStatement = null;
    static PreparedStatement updateStatement = null;
    static PreparedStatement findStatement = null;

    static final String insertSql = "insert into SensorDataHour( sensorID, startTime, sensorType, RecordCount, Value,Maximum, Minimum, squares,RMS,standardDeviation) values(?,?,?,?,?,?,?,?,?,?)";
    static final String updateSql = "update SensorDataHour set  recordCount=?, Value=?,Maximum=?, Minimum=?, squares=?, RMS=?, standardDeviation=? where sensorID=? and startTime=? and sensorType=?";
    static final String findSql = "select  RecordCount, Value,Maximum, Minimum, squares,RMS,standardDeviation from SensorDataHour where sensorID=? and startTime=? and sensorType=?";
    static final DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static int numInserted = 0;
    static int numUpdated = 0;
    static int numErrors = 0;
    static int numFlow = 0;
    static int numEnergy = 0;

   // static private SensorDataHour instance = null;

    Sensor sensor;
    int sensortypeTID;
    //java.sql.Date startTime;
    Calendar startTime;
    //Calendar endTime; 
    int count;

    Double sum;
    Double sumSquares;
    Double max;
    Double min;

    Double rms;
    Double sd;
    boolean onDatabase = false;

    static {
        logger.setUseParentHandlers(false);
        // logger.setLevel(Level.INFO);
        logger.addHandler(TegnonLoad.logHandler);

        try {
            //instance = new SensorDataHour();
            insertStatement = TegnonLoad.conn.prepareStatement(insertSql);
            updateStatement = TegnonLoad.conn.prepareStatement(updateSql);
            findStatement = TegnonLoad.conn.prepareStatement(findSql);

        } catch (SQLException exc) {
            logger.log(Level.SEVERE, "Startup cant create SensorDataHour ", exc);
        }
    }

    private SensorDataHour() {
             count = 0;//rs.getInt(i++);
        sum = 0.0; //rs.getDouble(i++);
        max = 0.0; //rs.getDouble(i++);
        min = 0.0; //rs.getDouble(i++);  
        sumSquares = 0.0; //rs.getDouble(i++);
        rms = 0.0; //rs.getDouble(i++);
        sd = 0.0; //rs.getDouble(i++);
        onDatabase = false;

    }
    
    /**
     * Look for sensorDataHour for previous hour
     * 
     * @param sensor
     * @param cal
     * @return record or null
     */
    SensorDataHour findPrevious() {
        SensorDataHour instance = new SensorDataHour();
        Calendar cal = startTime;
        cal.add(Calendar.HOUR,-1);
        try {
            findStatement.setInt(1, sensor.id);
            findStatement.setDate(2, new java.sql.Date(startTime.getTimeInMillis())); 
            findStatement.setInt(3, sensortypeTID);
          
            ResultSet rs = findStatement.executeQuery();
            int i = 1;
            // RecordCount, Value,Maximum, Minimum, squares,RMS,standardDeviation
            if (rs.next()) {
                instance.count = rs.getInt(i++);
                instance.sum = rs.getDouble(i++);
                instance.max = rs.getDouble(i++);
                instance.min = rs.getDouble(i++);
                instance.sumSquares = rs.getDouble(i++);
                instance.rms = rs.getDouble(i++);
                instance.sd = rs.getDouble(i++);
                instance.onDatabase = true;
                // @TODO: Why do we get here 
            }
        } catch (SQLException exc) {
            logger.severe("Problem locating SDH " + instance.sensor.toString() + "  " + df.format(cal.getTime()) + " " + exc.toString());
        }
        return instance;
        
    }
    
    static SensorDataHour factory(Sensor sensor, Calendar cal) {
        SensorDataHour instance = new SensorDataHour();
    
        instance.sensor = sensor;
        instance.sensortypeTID = sensor.typeTID;
        if (instance.sensortypeTID ==20)
            instance.sensortypeTID=19;
        instance.startTime = cal;
        instance.startTime.set(Calendar.MINUTE, 0);
        instance.startTime.set(Calendar.SECOND, 0);
        instance.startTime.set(Calendar.MILLISECOND, 0);

        try {
            findStatement.setInt(1, sensor.id);
            findStatement.setDate(2, new java.sql.Date(cal.getTimeInMillis())); 
            findStatement.setInt(3, instance.sensortypeTID);
          
            ResultSet rs = findStatement.executeQuery();
            int i = 1;
            // RecordCount, Value,Maximum, Minimum, squares,RMS,standardDeviation
            if (rs.next()) {
                instance.count = rs.getInt(i++);
                instance.sum = rs.getDouble(i++);
                instance.max = rs.getDouble(i++);
                instance.min = rs.getDouble(i++);
                instance.sumSquares = rs.getDouble(i++);
                instance.rms = rs.getDouble(i++);
                instance.sd = rs.getDouble(i++);
                instance.onDatabase = true;
                // @TODO: Why do we get here 
            }
        } catch (SQLException exc) {
            logger.severe("Problem locating SDH " + sensor.toString() + "  " + df.format(cal.getTime()) + " " + exc.toString());
        }
        return instance;
    }

    public String toString() {
        return " SensorDataHour:" + sensor + " Time:" + df.format(startTime.getTime()) + "Type:" + sensor.typeTID
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
     * updates or create s SensorDataHour record from called parameter
     *
     * The messages may not be processed in order, particularly if we rerun some
     * email files. The strategy is therefor to look for th data fro the other
     * half hour and use the found data as the basis for further calculation
     *
     * @param stat
     */
    public void addHalfHour(Statistic stat) { //throws SQLException {
        if (stat.count == 0) {
            return;
        }
        logger.info("-------------------------------------------------------------------------------------------------------SDH.AddHalfHour " + stat.toString());
        Statistic other;
        Calendar t = stat.startTime;

        if (t.get(Calendar.MINUTE) == 0) {
            t.set(Calendar.MINUTE, 30);

        } else {
            t.set(Calendar.MINUTE, 0);
        }
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MILLISECOND, 0);

        other = new Statistic(stat.sensor, t);

        logger.info("Other:" + other.toString());
        //sensor = stat.sensor;
        //startTime = t; //stat.startTime;
        //startTime.set(Calendar.MINUTE, 0);

        count = stat.count + other.count;

        max = ((stat.max > other.max) ? stat.max : other.max);
        min = stat.min;
        if ((stat.min > other.min) &&(other.min >=0))
            min=other.min;
        // this is strictly unnecessary since we fix it in the statistic
        if (this.sensortypeTID ==19) {
                SensorDataHour prev = findPrevious();
                if (prev != null) 
            min = prev.max;
        }
        if (sensortypeTID == 19) {
            //sum = (stat.sum + other.sum);
            sum = max - min;
        } else {
            sum = (stat.sum + other.sum) / count;    // 2016-07-07   fixup
        }
        sumSquares = stat.sumSquares + other.sumSquares;
        if (count > 0) {
            rms = Math.sqrt((sum * sum) / count);
            sd = Math.sqrt(Math.abs(sumSquares - sum * sum) / count);
        }
        try {
            if (onDatabase==true) {
                updateSql();
                logger.info("SensorDataHour updated :" + toString() + " addedValue:" + other.sum + " count:" + other.count
                        + " from " + df.format(stat.startTime.getTime()) + " to " + df.format(other.startTime.getTime()));
            } else {
                insertSql();
                logger.info("SensorDataHour inserted :" + toString() + "  From:" + df.format(stat.startTime.getTime()));
            }
        } catch (SQLException exc) {
            logger.severe("SensorDataHour failed : F" + exc + " " + toString());
        }
        if (sensor.typeTID == 19)  {
            numEnergy++;
        } else {
            numFlow++;
        }

    }

    ////   else {
    //          logger.info("SensorDataHour Count is Zero (No data for this sensor)" + toString());
    //// }
//}
    public void insertSql() throws SQLException {
        if (insertStatement == null) {
            insertStatement = TegnonLoad.conn.prepareStatement(insertSql);
        }
        int i = 1;

        // change 10 June 2016 to ensure we only get hourly records on file 
        startTime.set(Calendar.MINUTE, 0);
        startTime.set(Calendar.SECOND, 0);
        startTime.set(Calendar.MILLISECOND, 0);

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
        insertStatement.setInt(i++, sensortypeTID);
        insertStatement.setInt(i++, count);
        insertStatement.setDouble(i++, sum);
        insertStatement.setDouble(i++, max);
        insertStatement.setDouble(i++, min);
        insertStatement.setDouble(i++, sumSquares);

        Double rms = 0.00;
        Double sd = 0.00;
        if (count > 0) {
            rms = Math.sqrt(sumSquares / count);
            sd = Math.sqrt(Math.abs(sumSquares - sum * sum) / count);
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
        // change 10 June 2016 to ensure we only get hourly records on file 
        startTime.set(Calendar.MINUTE, 0);
        startTime.set(Calendar.SECOND, 0);
        startTime.set(Calendar.MILLISECOND, 0);

        java.util.Date dt = startTime.getTime();
        java.sql.Timestamp ts = new java.sql.Timestamp(dt.getTime());
        updateStatement.setInt(i++, count);
        updateStatement.setDouble(i++, sum);
        updateStatement.setDouble(i++, max);
        updateStatement.setDouble(i++, min);
        updateStatement.setDouble(i++, sumSquares);
        Double rms = 0.00;
        Double sd = 0.00;
        if (count > 0) {
            rms = Math.sqrt(sumSquares / count);
            sd = Math.sqrt(Math.abs(sumSquares - sum * sum) / count);
        }
        updateStatement.setDouble(i++, rms);
        updateStatement.setDouble(i++, sd);

        updateStatement.setInt(i++, sensor.id);
        updateStatement.setTimestamp(i++, ts);
        updateStatement.setInt(i++, sensortypeTID);

        updateStatement.execute();
        numUpdated++;
    }
}
