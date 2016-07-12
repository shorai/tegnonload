/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tegnonload;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
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
 
    static int numInserted = 0;
    static int numUpdated = 0;
    static int numErrors = 0;
    static int numFlow = 0;
    static int numEnergy = 0;

    Sensor sensor;
    int sensortypeTID;
    LocalDateTime startTime;
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
        logger.addHandler(TegnonLoad.logHandler);

        try {
            insertStatement = TegnonLoad.conn.prepareStatement(insertSql);
            updateStatement = TegnonLoad.conn.prepareStatement(updateSql);
            findStatement = TegnonLoad.conn.prepareStatement(findSql);

        } catch (SQLException exc) {
            logger.log(Level.SEVERE, "Startup cant create SensorDataHour ", exc);
        }
    }

    private SensorDataHour() {
        count = 0;
        sum = 0.0;
        max = 0.0;
        min = 0.0;
        sumSquares = 0.0;
        rms = 0.0;
        sd = 0.0;
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
        LocalDateTime ldt = LocalDateTime.from(startTime).minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS);

        try {
            findStatement.setInt(1, sensor.id);
            findStatement.setTimestamp(2, java.sql.Timestamp.valueOf(ldt));
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
                return instance;
            } else {
                return null;
            }
        } catch (SQLException exc) {
            logger.severe("Problem locating SDH " + instance.sensor.toString() + "  " + ldt.toString() + " " + exc.toString());
        }
        return null;

    }

    static SensorDataHour factory(Sensor sensor, LocalDateTime ldt) {
        SensorDataHour instance = new SensorDataHour();

        instance.sensor = sensor;
        instance.sensortypeTID = sensor.typeTID;
        if (instance.sensortypeTID == 20) {
            instance.sensortypeTID = 19;
        }
        instance.startTime = LocalDateTime.from(ldt).truncatedTo(ChronoUnit.HOURS);

        try {
            findStatement.setInt(1, sensor.id);
            findStatement.setTimestamp(2, java.sql.Timestamp.valueOf(instance.startTime));
            findStatement.setInt(3, ((instance.sensortypeTID == 20) ? 19 : instance.sensortypeTID));
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
                logger.info("SDH Record found " + sensor.id + " " + instance.startTime + " " + sensor.typeTID);
            } else {
                logger.info("SDH Record Not found on database" + sensor.id + " " + instance.startTime + " " + sensor.typeTID);
            }
        } catch (SQLException exc) {
            logger.severe("Problem locating SDH " + sensor.toString() + "  " + instance.startTime + " " + exc.toString());
        }
        return instance;
    }

    public String toString() {
        return " SensorDataHour:" + sensor + " Time:" + startTime + "Type:" + sensor.typeTID
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
        logger.info("-------------------------------------------------------------------------------------------------------SDH.AddHalfHour ");
        try {
            Statistic other;

            LocalDateTime ldt = LocalDateTime.from(startTime).truncatedTo(ChronoUnit.MINUTES);
            if (ldt.getMinute() >= 30) {
                ldt = ldt.withMinute(0);
            } else {
                ldt = ldt.withMinute(30);
            }

            other = new Statistic(stat.sensor, ldt);
            count = stat.count + other.count;

            max = ((stat.max > other.max) ? stat.max : other.max);
            min = stat.min;
            if ((stat.min > other.min) && (other.min >= 0)) {
                min = other.min;
            }

            StringBuilder sb = new StringBuilder("Stat\t\tcount\ttime\tvalue\tmax\tmin\tRMS\tSD");
            sb.append("\r\nFile :").append(startTime).append("\t").append(count).append("\t").append(sum).append("\t").append(max).append("\t").append(min).append("\t").append(rms).append("\t").append(sd);

            sb.append("\r\nRec  :").append(stat.startTime).append("\t").append(stat.count).append("\t").append(stat.sum).append("\t").append(stat.max).append("\t").append(stat.min).append("\t").append(rms).append("\t").append(sd);
            sb.append("\r\nOther:").append(other.startTime).append("\t").append(other.count).append("\t").append(other.sum).append("\t").append(other.max).append("\t").append(other.min).append("\t").append(rms).append("\t").append(sd);

            if ((sensortypeTID == 19) || (sensortypeTID == 20)) {
                sum = max - min;
                if (sum < 0.0) {
                    sum += 32767.0;
                }

            } else {
                sum = (stat.sum + other.sum) / count;    // 2016-07-07   fixup
            }
            sumSquares = stat.sumSquares + other.sumSquares;
            if (count > 0) {
                rms = Math.sqrt((sum * sum) / count);
                sd = Math.sqrt(Math.abs(sumSquares - sum * sum) / count);
            }

            sb.append("\r\nWrite:").append(startTime).append("\t").append(count).append("\t").append(sum).append("\t").append(max).append("\t").append(min).append("\t").append(rms).append("\t").append(sd);
            logger.info(sb.toString());
        } catch (Exception exc) {
            logger.log(Level.SEVERE, "addHalfHour failed ", exc);
        }

        try {
            if (onDatabase == true) {
                updateSql();
            } else {
                insertSql();
            }
        } catch (SQLException exc) {
            logger.severe("SensorDataHour failed : " + exc);
        }
        if ((sensor.typeTID == 19) || (sensortypeTID == 19)) {
            numEnergy++;
        } else {
            numFlow++;
        }

    }

    public void insertSql() throws SQLException {
        if (insertStatement == null) {
            insertStatement = TegnonLoad.conn.prepareStatement(insertSql);
        }
        int i = 1;

        java.sql.Timestamp ts = java.sql.Timestamp.valueOf(startTime.truncatedTo(ChronoUnit.HOURS)); //dt.getTime());

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

        insertStatement.execute();
        logger.info("SensorDataHour inserted for:" + sensor.id + " " + sensor.typeTID + "  @ " + ts.toLocalDateTime());
        numInserted++;
    }

    public void updateSql() throws SQLException {
        if (updateStatement == null) {
            updateStatement = TegnonLoad.conn.prepareStatement(updateSql);
        }
        int i = 1;

        java.sql.Timestamp ts = java.sql.Timestamp.valueOf(startTime.truncatedTo(ChronoUnit.HOURS)); //dt.getTime());
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
        logger.info("SensorDataHour updated for:" + sensor.id + " " + sensor.typeTID + "  @ " + ts.toLocalDateTime());
        numUpdated++;
    }
}
