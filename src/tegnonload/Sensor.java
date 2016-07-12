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
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 * @author Chris
 */
public class Sensor {

    static final String insertSQL = "insert into sensors( DeviceID, SensorTypeTID, SensorUnitTID,MeasurementTypeTID,SensorNumber) values(?,?,?,?,?)";
    static PreparedStatement insertStatement = null;

    static final String findByIdSQL = "select SensorId from Sensors where DeviceID=? and SensorTypeTID = ? and SensorNumber= ?";
    static PreparedStatement findByIdStatement = null;

    static int inserts = 0;

    static final Logger logger = TegnonLoad.tegnonLogger.getLogger("tegnonload.Sensor");

    static HashMap<String, Sensor> sensors = new HashMap<String, Sensor>();

    int id;
    Device device;
    int typeTID;
    int unitTID;
    int measureTID;
    int lineId;
    int netId;
    int columnNumber;

    Statistic stat;

    //SensorID	DeviceID	SensorTypeTID	SensorUnitTID	MeasurementTypeTID	LineID	NetworkID	SensorNumber",
    static {
        logger.setUseParentHandlers(false);

        logger.addHandler(TegnonLoad.logHandler);
    }

    public String toString() {
        return ("Sensor [" + id + "] Device:" + device.toString() + " TypeTID:" + typeTID + " Line:" + lineId + " NetID:" + netId + " ColNumber:" + columnNumber);
    }

    public Sensor(ResultSet rs) throws SQLException {
        int i = 1;
        id = rs.getInt(i++);
        int devId = rs.getInt(i++);
        device = Device.findById(devId);
        typeTID = rs.getInt(i++);
        unitTID = rs.getInt(i++);
        measureTID = rs.getInt(i++);
        lineId = rs.getInt(i++);
        netId = rs.getInt(i++);
        columnNumber = rs.getInt(i++);
        sensors.put(key(), this);
        stat = null;
    }

    public Sensor(Device dev, ArduinoSensor as, int colNumber) throws SQLException {
        //SensorID, DeviceID, SensorTypeTID, SensorUnitTID,MeasurementTypeTID,SensorNumber
        device = dev;
        typeTID = as.sensorType;
        unitTID = as.sensorUnits;
        measureTID = as.measurmentType;
        columnNumber = colNumber;
        sensors.put(key(), this);
        stat = null;
        try {
            insert();
        } catch (SQLException exc) {
            logger.severe("Sensor insert failed Device:" + dev.toString() + " Arduino:" + as.toString() + " Column: " + colNumber);
        }
    }

    String key() {
        return "" + device.key() + "," + columnNumber + "," + typeTID;
    }

    static final Sensor find(Device dev, int columnNumber, int sensorType) {
        return sensors.get("" + dev.key() + "," + columnNumber + "," + sensorType);

    }

    Statistic getStat(LocalDateTime ldt) {
        if (ldt == null) {
            logger.severe("LocalDateTime is null");
        }
        if (stat == null) {
            stat = new Statistic(this, ldt);
        }
        stat.setStartTime(ldt);
        return stat;
    }

    static void zeroStat() {
        inserts = 0;
    }

    static void zeroTots() {
        for (Sensor s : sensors.values()) {
            if (s.stat != null) {
                s.stat.zero();
            }
        }
    }

    static void dump() {
        System.out.println("****************************************************** Sensors Dump");

        for (Sensor s : sensors.values()) {
            System.out.println(s.id + " " + s.device.key() + " Devid:" + s.device.deviceID + " Type:" + s.typeTID);
        }

    }

    static void writeSQL(Integer messageId) {
        for (Sensor s : sensors.values()) {
            Statistic stat = s.stat;
            if (stat != null) {

                if (s.typeTID == 20) {
                    if (stat.last != stat.first) {
                        logger.fine("******************************************************** writeEnergySQL for " + stat.toString());
                        stat.writeEnergySQL(messageId);
                    }
                } else {

                    logger.info("======================================================== writeFlowSQL for " + stat.toString());
                    stat.writeFlowSQL(messageId);
                }
            }
        }
    }

    static void loadSQL(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select * from sensors");
        while (rs.next()) {
            new Sensor(rs);
        }
    }
//java.sql.SQLException: The INSERT statement conflicted with the FOREIGN KEY constraint "FK_Sensors_SensorType". The conflict occurred in database "TegnonEfficiency", table "dbo.SensorType", column 'SensorTypeID'.

    void insert() throws SQLException {
        if (insertStatement == null) {
            insertStatement = TegnonLoad.conn.prepareStatement(insertSQL);
            findByIdStatement = TegnonLoad.conn.prepareStatement(findByIdSQL);
        }
        int i = 1;
        insertStatement.setInt(i++, device.deviceID);
        insertStatement.setInt(i++, typeTID);
        insertStatement.setInt(i++, unitTID);
        insertStatement.setInt(i++, measureTID);
        insertStatement.setInt(i++, columnNumber);

        insertStatement.executeUpdate();

        findByIdStatement.setInt(1, device.deviceID);
        findByIdStatement.setInt(2, typeTID);
        findByIdStatement.setInt(3, columnNumber);
        ResultSet rs = findByIdStatement.executeQuery();
        rs.next();
        id = rs.getInt(1);
        logger.info("Sensor.insert " + id + " " + device.key() + " Devid:" + device.deviceID + " Type:" + typeTID + " Column:" + columnNumber);
        inserts++;
    }
}
