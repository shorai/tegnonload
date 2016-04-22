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
import java.util.logging.Logger;

/**
 *
 * @author chris.rowse
 *
 * The primary purpose of this class is to maintain the SnesorDataHour table
 *
 * It does so by responding to a SensorDataHalfHour update(statistic) The object
 * is singleton since we only ever deal with a single instance at a time
 *
 */
public class SensorDataHour {

    static final Logger logger = Logger.getLogger("Stitistic");

    static PreparedStatement insertStatement = null;
    static PreparedStatement updateStatement = null;
    static PreparedStatement findStatement = null;

    static final String insertSql = "insert into SensorDataHour( sensorId, startTime, sensorType, recordCount, sensorValue,maximum, minimum, sumOfSquares,RMS,standardDeviation) values(?,?,?,?,?,?,?,?,?,?)";
    static final String updateSql = "update SensorDataHour set  recordCount=?, sensorValue=?,maximum=?, minimum=?, sumOfSquares=?, RMS=?, standardDeviation=? where sensorId=? and startTime=? and sensorType=?";
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
    
    SensorDataHour() throws SQLException {
    insertStatement = TegnonLoad.conn.prepareStatement(insertSql);
    updateStatement = TegnonLoad.conn.prepareStatement(updateSql);
    findStatement = TegnonLoad.conn.prepareStatement(findSql);
    
    instance = this;
    }
    
    /**
     * Finds the matching half hour record if it exists
     *    Adds the values and updates 
     *    or creates a new from called parameter
     * 
     * @param stat 
     */
    public void addHalfHour(Statistic stat) throws SQLException {
        
        Statistic other;
        Calendar t = stat.startTime;
                
         if (t.get(Calendar.MINUTE)== 0) {
            t.set(Calendar.MINUTE,30);
        } else {
            t.set(Calendar.MINUTE,0);
        }
         int update = 1;
         try { 
            other = new Statistic(stat.sensor.id, t, stat.sensor.typeTID);
         } catch(SQLException exc) { 
             update = 0;
            other = new Statistic(stat.sensor);
         }
        
         sensor = stat.sensor;
         startTime = stat.startTime;
         count = stat.count+other.count;
         sum =  stat.sum + other.sum;
         sumSquares = stat.sumSquares + other.sumSquares;
        
         if (update ==0) {
             updateSql();
             
         } else {
             insertSql();
         }
    }

    public void insertSql() throws SQLException {
        int i = 1;
         java.util.Date dt = startTime.getTime();
        java.sql.Timestamp ts = new java.sql.Timestamp(dt.getTime());
        insertStatement.setInt(i++, sensor.id);
        insertStatement.setTimestamp(i++,ts);
        insertStatement.setInt(i++, sensor.typeTID);
        insertStatement.setInt(i++,count);
        insertStatement.setDouble(i++,sum);
        insertStatement.setDouble(i++,sumSquares);
        insertStatement.setDouble(i++,max);
        insertStatement.setDouble(i++,min);
        insertStatement.setDouble(i++,Math.sqrt(sumSquares/count));
        insertStatement.setDouble(i++,Math.sqrt((sumSquares-sum*sum)/count));
        
        insertStatement.executeUpdate();
    }
    
    public void updateSql() throws SQLException {
         int i = 1;
         java.util.Date dt = startTime.getTime();
        java.sql.Timestamp ts = new java.sql.Timestamp(dt.getTime());
        updateStatement.setInt(i++,count);
        updateStatement.setDouble(i++,sum);
        updateStatement.setDouble(i++,sumSquares);
        updateStatement.setDouble(i++,max);
        updateStatement.setDouble(i++,min);
        updateStatement.setDouble(i++,Math.sqrt(sumSquares/count));
        updateStatement.setDouble(i++,Math.sqrt((sumSquares-sum*sum)/count));
     
           updateStatement.setInt(i++, sensor.id);
        updateStatement.setTimestamp(i++,ts);
        updateStatement.setInt(i++, sensor.typeTID);
     
        updateStatement.executeUpdate();
    }
}
