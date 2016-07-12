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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chris
 */
public class Device {

    static String insertSQL = "insert into Device(facilityInfo,DeviceCommonName,modbusAddr,DeviceSerialNumber,reporting,locationID,NumberOfAttachedSensors) values(?,?,?,?,?,?,?)";
    PreparedStatement insertStatement = null;

    static String findByIdSQL = "select DeviceId from DEvice where facilityInfo=? and modbusAddr=? and DeviceSerialNumber=?";
    PreparedStatement findByIdStatement = null;

    static int inserts = 0;

    static final Logger logger = TegnonLoad.tegnonLogger.getLogger("tegnonload.Device");

    static HashMap<String, Device> devices = new HashMap<String, Device>();
    static HashMap<Integer, Device> devicesById = new HashMap<Integer, Device>();

    int deviceID;
    String facilityInfo;
    String name;
    int networkId;
    String sensorFacilityInfo;
    int modbusAddr;
    int serialNumber;
    String type;
    String version;
    boolean reporting;
    int locationId;
    int numSensors;
    int clientId;

    static {
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);
        logger.addHandler(TegnonLoad.logHandler);
    }

    public Device(ResultSet rs) throws SQLException {
        int i = 1;
        deviceID = rs.getInt(i++);
        facilityInfo = rs.getString(i++);
        name = rs.getString(i++);
        networkId = rs.getInt(i++);
        sensorFacilityInfo = rs.getString(i++);
        modbusAddr = rs.getInt(i++);
        serialNumber = rs.getInt(i++);
        type = rs.getString(i++);
        version = rs.getString(i++);
        reporting = rs.getBoolean(i++);
        locationId = rs.getInt(i++);
        numSensors = rs.getInt(i++);

        devices.put(this.key(), this);
        devicesById.put(deviceID, this);

    }

    public Device(PiLine pi) throws SQLException {
        facilityInfo = pi.facilityInfo;
        name = pi.deviceCommonName;
        modbusAddr = pi.modbusAddr;
        serialNumber = pi.deviceSerialNumber;
        reporting = true;
        Hierarchy h = Hierarchy.find(this.facilityInfo);
        locationId = h.locationID;
        numSensors = pi.numberOfAttachedSensors;

        insert();

        devices.put(this.key(), this);
        devicesById.put(deviceID, this);

    }

    public Device(String params) {

        String[] strs = params.split("[\t ]+");
        int i = 0;
        deviceID = Integer.parseInt(strs[i++]);
        facilityInfo = strs[i++];
        name = strs[i++];
        try {
            networkId = Integer.parseInt(strs[i++]);
        } catch (Exception exc) {
            networkId = -1;
        }
        i++;
        modbusAddr = Integer.parseInt(strs[i++]);
        serialNumber = Integer.parseInt(strs[i++]);
        type = strs[i++];
        version = strs[i++];
        reporting = Boolean.getBoolean(strs[i++]);
        locationId = Integer.parseInt(strs[i++]);
        try {
            numSensors = Integer.parseInt(strs[i++]);
        } catch (Exception exc) {
            numSensors = 0;
        }
    }

    static public void zeroStat() {
        inserts = 0;
    }

    String key() {
        return facilityInfo + "|" + modbusAddr + "|" + serialNumber;
    }

    public String toString() {
        return "Device[" + deviceID + "]" + key();
    }

    static public Device find(String facilityInfo, int modbusAddr, int serialNumber) {

        return devices.get(facilityInfo + "|" + modbusAddr + "|" + serialNumber);
    }

    static public Device findById(Integer id) {

        return devicesById.get(id);
    }

    static void dump() {
        System.out.println("************************************************************ Devices Dump");
        for (Device d : devices.values()) {
            System.out.println(" " + d.deviceID + " " + d.facilityInfo + "\t" + d.name + "\t" + d.type);
        }
    }

    static void loadSQL(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select * from DEvice");
        while (rs.next()) {
            new Device(rs);
        }

    }

    void insert() throws SQLException {
        int i = 1;
        if (findByIdStatement == null) {
            findByIdStatement = TegnonLoad.conn.prepareStatement(findByIdSQL);
            insertStatement = TegnonLoad.conn.prepareStatement(insertSQL);
        }
        //facilityInfo,DeviceCommonName,modbusAddr,DeviceSerialNumber,reporting,NumberOfAttachedSensors
        insertStatement.setString(i++, facilityInfo);
        insertStatement.setString(i++, name);
        insertStatement.setInt(i++, modbusAddr);
        insertStatement.setInt(i++, serialNumber);
        insertStatement.setBoolean(i++, reporting);
        insertStatement.setInt(i++, locationId);
        insertStatement.setInt(i++, numSensors);
        insertStatement.executeUpdate();

        findByIdStatement.setString(1, facilityInfo);
        findByIdStatement.setInt(2, modbusAddr);
        findByIdStatement.setInt(3, serialNumber);
        ResultSet rs = findByIdStatement.executeQuery();
        rs.next();
        this.deviceID = rs.getInt(1);
        logger.fine("Device.Insert " + deviceID + " " + facilityInfo + "\t" + name + "\t" + type);
        inserts++;
    }
}
