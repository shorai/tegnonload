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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton pattern since we always deal with a single instance at a time
 *
 * Later versions may optimise by reading an array for an attachment or device
 *
 * @author chris.rowse
 */
public class SensorDataNormal {
static final Logger logger = Logger.getLogger("SensorDataNormal");

    static final String findSql = "select ID,attachmentId, SensorValue, SensorCalculatedType, SensorCalculatedValue "
            + " from SensorDataNormal"
            + " where SensorID = ? and DateTimeStamp = ? and SensorType = ?";
    static PreparedStatement findStatement = null;

    static final String insertSql = "insert into SensorDataNormal(AttachmentID, "
            + "SensorId, DeviceID,DateTimeStamp,SensorType"
            + "SensorValue, SensorCalculatedType,SensorCalculatedValue)"
            + "values(?,?,?,?,?,?,?,?)";
    static PreparedStatement insertStatement = null;

    static final String updateSql = "update SensorDataNormal set"
            + " SensorValue=?, SemsorCalculatedType=?, SensorCalculatedValue=?"
            + " wher id = ?";
    static PreparedStatement updateStatement = null;

    static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static int numInserts = 0;
    static int numUpdates = 0;
    static int numErrors = 0;
    
    Integer id;
    int attachmentId = -1;
    Sensor sensor;
    //Device device;
    Integer sensorType;
    Double value;
    Integer calcType;
    Double calcValue;

    static public SensorDataNormal instance = new SensorDataNormal();

    static void init(Connection conn) throws SQLException {
        instance = new SensorDataNormal();
        findStatement = TegnonLoad.conn.prepareStatement(findSql);
        insertStatement = TegnonLoad.conn.prepareStatement(insertSql);
        updateStatement = TegnonLoad.conn.prepareStatement(updateSql);
    }
    
    
    static void zeroStat() {
        int numInserts = 0;
     numUpdates = 0;
     numErrors = 0;
    }
/*
    public void save(Sensor s, PiLine pi, ArduinoSensor data) throws SQLException, ParseException {
        int i = 1;

        java.sql.Timestamp time = new java.sql.Timestamp(df.parse(pi.timeStamp).getTime());
        
        findStatement.setInt(i++, s.id);
        findStatement.setTimestamp(i++, time);
        findStatement.setInt(i++, s.typeTID);

        ResultSet rs = findStatement.executeQuery();
        if (rs.next()) {
            i = 1;
            id = rs.getInt(i++);
            attachmentId = rs.getInt(i++);
            sensorType = s.typeTID;
            value = rs.getDouble(i++);
            calcType = rs.getInt(i++);
            calcValue = rs.getDouble(i++);
            i = 1;
            updateStatement.setDouble(i++, value);
            updateStatement.setInt(i++, calcType);
            updateStatement.setDouble(i++, calcValue);

            updateStatement.setInt(i++, id);

            updateStatement.execute();
        } else {
            i = 1;
            insertStatement.setInt(i++, attachmentId);
            insertStatement.setInt(i++, s.id);
            insertStatement.setInt(i++,s.device.deviceID);
            insertStatement.setTimestamp(i++,time);
            insertStatement.setInt(i++,s.typeTID);
            insertStatement.setDouble(i++,value);
            insertStatement.setInt(i++,calcType);
            insertStatement.setDouble(i++,calcValue);
            
            insertStatement.execute();
        }
    }
*/
    public void save(Statistic stat,Calendar time) { // throws SQLException, ParseException {
        int i = 1;
        try {
        //java.sql.Timestamp time = new java.sql.Timestamp(stat.startTime.getTimeInMillis()); //new java.sql.Timestamp(df.parse(pi.timeStamp).getTime());
        Sensor s = stat.sensor;
        
        findStatement.setInt(i++, s.id);
        findStatement.setTimestamp(i++, new java.sql.Timestamp(time.getTimeInMillis()));
        findStatement.setInt(i++, s.typeTID);

        ResultSet rs = findStatement.executeQuery();
        if (rs.next()) {
            i = 1;
            id = rs.getInt(i++);
            attachmentId = rs.getInt(i++);
            sensorType = s.typeTID;
            value = rs.getDouble(i++);
            calcType = rs.getInt(i++);
            calcValue = rs.getDouble(i++);
            i = 1;
            updateStatement.setDouble(i++, value);
            updateStatement.setInt(i++, calcType);
            updateStatement.setDouble(i++, calcValue);

            updateStatement.setInt(i++, id);

            updateStatement.execute();
            numUpdates++;
        } else {
            i = 1;
            insertStatement.setInt(i++, attachmentId);
            insertStatement.setInt(i++, s.id);
            insertStatement.setInt(i++,s.device.deviceID);
            insertStatement.setTimestamp(i++,new java.sql.Timestamp(time.getTimeInMillis()));
            insertStatement.setInt(i++,s.typeTID);
            insertStatement.setDouble(i++,value);
            insertStatement.setInt(i++,calcType);
            insertStatement.setDouble(i++,calcValue);
            
            insertStatement.execute();
            numInserts++;
        }
    } catch (SQLException sexc) {
        logger.log(Level.SEVERE,"SensorDataNormal "+ sexc.getMessage(),sexc);
        numErrors++;
    }
    }
}
