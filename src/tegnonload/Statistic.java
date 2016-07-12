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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
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
    static final String loadSql = "select RecordCount,SensorValue,Maximum,Minimum,SUmOfSquares from SensorDataHalfHour where sensorID=? and startTime=? and sensorType=?";

    static final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static int numInserted = 0;
    static int numUpdated = 0;
    static int numErrors = 0;
    static int numFlow = 0;
    static int numEnergy = 0;

    Sensor sensor;

    LocalDateTime startTime;
    LocalDateTime endTime;

    Double first;
    Double last;
    Double sum;
    Double sumSquares;
    Double max;
    Double min;
    int count;

    boolean onServer = false;

    static {

        logger.setUseParentHandlers(false);
        logger.addHandler(TegnonLoad.logHandler);
    }

    public String toString() {
        return " Statistic " + ((sensor == null) ? "Null sensor" : sensor.toString())
                + " StartTime:" + ((startTime == null) ? "Null Date" : startTime)
                + " Value :" + ((count > 0) ? sum : "No Data") + " OnFile:" + onServer;
    }

    public void setStartTime(LocalDateTime ldt) {

        startTime = LocalDateTime.from(ldt).truncatedTo(ChronoUnit.MINUTES);

        if (startTime.getMinute() >= 30) {
            startTime = startTime.with(ChronoField.MINUTE_OF_HOUR, 30);
            endTime = startTime.with(ChronoField.MINUTE_OF_HOUR, 59);

        } else {
            startTime = startTime.with(ChronoField.MINUTE_OF_HOUR, 0);
            endTime = startTime.with(ChronoField.MINUTE_OF_HOUR, 29);
        }
        endTime = endTime.withSecond(59).withNano(999999999);
    }

    public Statistic(Sensor sensor, LocalDateTime ldt) { //throws SQLException {

        zero();
        this.sensor = sensor;
        setStartTime(ldt);
        onServer = false;
        tryLoad();
    }

    void tryLoad() {
        try {
            if (loadStatement == null) {
                loadStatement = TegnonLoad.conn.prepareStatement(loadSql);
            }
            loadStatement.setInt(1, sensor.id);
            loadStatement.setTimestamp(2, java.sql.Timestamp.valueOf(startTime));
            loadStatement.setInt(3, ((sensor.typeTID == 20) ? 19 : sensor.typeTID));

            ResultSet rs = loadStatement.executeQuery();
            int i = 1;
            if (rs.next()) {
                count = rs.getInt(i++);
                sum = rs.getDouble(i++);
                max = rs.getDouble(i++);
                min = rs.getDouble(i++);
                sumSquares = rs.getDouble(i++);
                onServer = true;
                logger.info("Statistic loaded from DB " + sensor.id + "  " + startTime);
            } else {
                logger.info("Statistic load no results found " + sensor.id + "  " + startTime);
            }
        } catch (SQLException exc) {

            logger.warning("Statistic lookup failed: " + sensor.id + "  " + startTime);
        }

    }

    /**
     * ret
     *
     * @return the statistic for the immediately preceding half hour
     */
    Statistic getPrevious() {
        LocalDateTime ldt = startTime.minusMinutes(30);

        Statistic stat = new Statistic(this.sensor, ldt);
        logger.info("Find Prev SD Half hour:" + sensor.id + " for:" + startTime + " @:" + ldt + ((stat.onServer) ? " Found" : " Not Found") + " Max:" + stat.max);

        return stat;
    }

    static void prepare(Connection conn) {
        try {
            insertStatement = TegnonLoad.conn.prepareStatement(insertSql);
            updateStatement = TegnonLoad.conn.prepareStatement(updateSql);
            findStatement = TegnonLoad.conn.prepareStatement(findSql);

        } catch (Exception exc) {
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

    public void add(LocalDateTime ldt, Double value) throws Exception {
        if (count == 0) {
            first = value;
            last = value;
            if (value > 0.0) {
                min = value;
            }
            endTime = LocalDateTime.from(ldt);
            sum = value;
            sumSquares += (value * value);

        } else {

            last = value;
            sum += value;
            sumSquares += (value * value);
            if (max < value) {
                max = value;
            }
            if ((value > 0.0)
                    && ((min > value) || (min <= -1.0))) {
                min = value;

            }
        }
        count++;
    }

    static String getSqlStat() {
        return "Inserted:" + numInserted + " Updated:" + numUpdated + " Errors:" + numErrors;
    }

    String dump() {
        String timeStr = startTime.toString();

        return sensor.key() + "\t" + startTime + "\t" + endTime
                + "\t First:" + first + "\tLast:" + last + "\tSum:" + sum
                + "\tMax:" + max + "\tMin:" + min + "\tCount:" + count;
    }
    static final String retrySql = "update SensorDataHalfHour set attachmentID=%d,  recordCount=%d,"
            + " sensorValue=%f,maximum=%f, minimum=%f, sumOfSquares=%f, RMS=%f, standardDeviation=%f "
            + "where sensorId=%d and startTime='%s' and sensorType=%d";
/**
 * WARNING:  Failed update - retrying Parameter #7 has not been set.
 * @TODO Why on earth are we getting this error only from this update??
 * @param messageId
 * @param sensor
 * @param sensorType
 * @param rms
 * @param sd
 * @throws SQLException 
 */
    void retryUpdate(Integer messageId, Sensor sensor, Integer sensorType, Double rms, Double sd) throws SQLException {

        String sts = df.format(startTime);

        String s = String.format(retrySql, messageId, count, sum, max, min, sumSquares, rms, sd, sensor.id, sts, sensorType);

        TegnonLoad.conn.createStatement().execute(s);

        logger.info("Retry Update completed:" + s);
    }

    void toDb(Integer messageId) { //, Sensor sensor, Integer sensorType) { //, Integer recordCount,

        int recordsOnFile = -1;
        try {
            // look for record on DB
            findStatement.setInt(1, sensor.id);
            findStatement.setTimestamp(2, java.sql.Timestamp.valueOf(startTime)); //new java.sql.Time(startTime.getTime().getTime()));
            findStatement.setInt(3, sensor.typeTID); //sensorType);

            ResultSet rs = findStatement.executeQuery();
            rs.next();
            recordsOnFile = rs.getInt(1);
            rs.close();

            logger.info("Lookup SensorDataHalfHour SensorId:" + sensor.id
                    + " Time:" + startTime + " typeTID:" + sensor.typeTID + " Count:" + recordsOnFile);

            Double rms = 0.00;
            Double sd = 0.00;

            if (recordsOnFile > 0) {
                rms = Math.sqrt(sumSquares / count);
                sd = Math.sqrt(Math.abs(sumSquares - sum * sum) / count);
            }

            if ((rms == null) || (rms.isNaN()) || (rms.isInfinite())) {
                logger.warning("Computed RMS value was not acceptable:" + rms);
                rms = new Double(0.00);
            }
            if ((sd == null) || (sd.isNaN()) || (sd.isInfinite())) {
                logger.warning("Computed StandardDeviation was unacceptable:" + sd);
                sd = new Double(0.00);
            }

            int i = 1;
            if (recordsOnFile == 0) {
                if (TegnonLoad.UPDATE_DATA) {
                    insertStatement.setInt(i++, messageId);
                    insertStatement.setInt(i++, sensor.id);
                    insertStatement.setTimestamp(i++, java.sql.Timestamp.valueOf(startTime));
                    insertStatement.setInt(i++, sensor.typeTID);
                    insertStatement.setInt(i++, count);
                    insertStatement.setDouble(i++, sum);
                    insertStatement.setDouble(i++, max);
                    insertStatement.setDouble(i++, min);
                    insertStatement.setDouble(i++, sumSquares);
                    insertStatement.setDouble(i++, rms);
                    insertStatement.setDouble(i++, sd);

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
                    updateStatement.setTimestamp(i++, java.sql.Timestamp.valueOf(startTime));
                    // updateStatement.setDate(i++, startTime.getTime());
                    updateStatement.setInt(i++, sensor.typeTID);

                    //int cnt = updateStatement.executeUpdate();
                    logger.info(" Updating " + messageId + "," + count + ","
                            + sum + "," + max + "," + min + ","
                            + sumSquares + "," + rms + "," + sd + ","
                            + sensor.id + "," + startTime + "," + sensor.typeTID);

                    try {
                        updateStatement.execute();
                        logger.log(Level.FINE, " One Statistic updated:" + dump());
                    } catch (SQLException xx) {
                        logger.warning(" Failed update - retrying " + xx.getMessage());
                        /*
                        WARNING:  Failed update - retrying Parameter #7 has not been set.
                        @TODO:   why do we get this message?? everything seems to be set ok, other updates succeed
                        */
                        retryUpdate(messageId, sensor, sensor.typeTID, rms, sd);
                    }

                } else {
                    logger.log(Level.INFO, "Dummy Update Statistic " + dump());
                }
                numUpdated++;
            }

        } catch (Exception exc) {
            logger.log(Level.SEVERE, "Statistic.toDB():SQL error "
                    + " SensorID:" + sensor.id + " Time:" + startTime //startTime.getTime()) 
                    + " Type:" + sensor.typeTID + " Count:" + count + " "
                    + exc.getMessage(), exc);
            numErrors++;
        }

    }

    void writeFlowSQL(Integer messageId) {
        if (sum > 0.00) {
            toDb(messageId); //, sensor, sensor.typeTID); //, count, sum, max, min, sumSquares);
            logger.log(Level.INFO, "Stat.writeFlow  " + dump());
            SensorDataHour instance = SensorDataHour.factory(this.sensor, startTime);// this.startTime);
            instance.addHalfHour(this);
            numFlow++;
        }
    }

    /**
     * The Half hour and hour tables contain consumption figures for the period
     * Energy readings are cumulative modulo 65535 Here we convert cumulative
     * readings to usage assuming no more than 1 rollover per period This is a
     * fudge but works OK We should really create a clone Sensor and statistic
     *
     * @param messageId
     */
    void writeEnergySQL(Integer messageId) {

        Double val = last - first;
        logger.info("******************************************************** writeEnergySQL for\r\n " + this.toString());

        max = last;
        min = first;
        Statistic prev = getPrevious();
        if (prev.onServer) {
            logger.info("Previous Stat found change min energy from " + min + " to :" + prev.max + "  value from" + val + " to" + (max - prev.max));
            min = prev.max;
            val = max - min;
        } else {
            logger.info("Previous Stat not found min energy is  " + min + " Value is:" + val);
        }
        if (val < 0.0) {
            val += 32767.0;
        }
        if (val > 0.00) {
            count = 60;
            this.sum = val;
            this.sumSquares = val * val;

            int type = sensor.typeTID;
            sensor.typeTID = 19;
            toDb(messageId);
            logger.info("Stat.writeEnergy  " + dump() + " Energy Calc:" + val);
            SensorDataHour instance = SensorDataHour.factory(sensor, startTime);
            instance.addHalfHour(this);
            sensor.typeTID = type;
            numEnergy++;
        } else if (val < -1.00) {
            try {
                logger.log(Level.WARNING, "Stat.writeEnergy (last:" + last + " -first:" + first + ") less than zero " + val + " " + dump());
            } catch (Exception exx) {
                logger.severe("Stat.writeEnergy Exception" + exx.getMessage());
            }
        }
    }

}
