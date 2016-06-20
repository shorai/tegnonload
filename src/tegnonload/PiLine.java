/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tegnonload;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import static tegnonload.Sensor.logger;

/**
 *
 * @author Chris
 */
public class PiLine {

    static final Logger logger = TegnonLoad.tegnonLogger.getLogger("PiLine");
  static final DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static int numLines = 0;
    
    static int numFails = 0;
    static String facility = null;
    static Calendar firstTime= null;
    
    String timeStamp;          // The date and time of the recorded date
    String facilityInfo;       // Sensor Location
    String deviceCommonName;   //  Sensor Name
    int modbusAddr;         // Modbus Address
    int deviceSerialNumber; // Device Serial Number can be T100 - String
    int deviceTimeAlive;    // Device Type
    int deviceStatus;       // DeviceStatus
    int dataReadMode;       // DataReadMode
    int deviceVoltage;      // DeviceVoltage
    int numberOfAttachedSensors; // Number of sensors attached to device

    ArduinoSensor[] sensors = new ArduinoSensor[10];

    static {
       // logger.addHandler(TegnonLoad.logHandler);
       // logger.setLevel(Level.INFO);
        //TegnonLoad.logHandler.setLevel(Level.INFO);
    }

    PiLine(String[] strs) throws Exception {

        //String[] strs = str.split("[|]");
        int i = 0;
        timeStamp = strs[i++];
        facilityInfo = strs[i++]; // array index out of bounds   1??
        deviceCommonName = strs[i++];
        modbusAddr = Integer.decode(strs[i++]);
        try {
            deviceSerialNumber = Integer.parseInt(strs[i++]);
        } catch (Exception exd) {
            logger.warning("Device Serial nUmber not numeric:" + strs[i-1] + exd.getMessage());
            deviceSerialNumber = -1;
        }
        try {
            deviceTimeAlive = Integer.decode(strs[i++]);
        } catch (Exception exc) { 
            logger.warning("DeviceTimeAlive not numeric :"+ strs[i-1] + " " + exc.getMessage());
            deviceTimeAlive = -100;
          }
        deviceStatus = Integer.decode(strs[i++]);
        dataReadMode = Integer.decode(strs[i++]);
        deviceVoltage = Integer.decode(strs[i++]);
        numberOfAttachedSensors = Integer.decode(strs[i++]);
        
        facility = facilityInfo;
        firstTime = Calendar.getInstance();
        firstTime.setTime(df.parse(timeStamp));
        
        int j = 0;

        if (deviceStatus == 65535) {
            throw new Exception("Device Faulty status (" + deviceStatus + ")");
        }
        while (i < strs.length - 4) {
            ArduinoSensor as = new ArduinoSensor(j, strs, i);
            sensors[j] = as;
            i += 5;

            try {
                Device d = Device.find(facilityInfo, modbusAddr, deviceSerialNumber);
                if (d == null) {
                    d = new Device(this);
                }
                Sensor s = Sensor.find(d, j+1, as.sensorType);
                if (s == null) {
                    s = new Sensor(d, as, j+1);
                }
                /* if (as.sensorType == 20) {
                    logger.info("Energy Sensor " + d.facilityInfo + ":" + d.name + " Sensor:" + s.id + " Key:" +s.key() + " Type:" + as.sensorType + " Value:" + as.sensorValue 
                            + " SensorIs:"+ s.key() + " Sensors.Type:" + s.typeTID);
                }
                 */
                s.stat.add(timeStamp, as.sensorValue);
                j++;
                numLines++;
            } catch (Exception exc) {
                logger.log(Level.SEVERE, "Cant add stat " + strs + " Device:" + facilityInfo + "|" + modbusAddr + "|" + deviceSerialNumber, exc);
                numFails++;
            }
        }
    }
static public void zeroStat() {
        facility = "Unknown";
        try {
            firstTime = Calendar.getInstance();
        firstTime.setTime(df.parse("2000/01/01 00:00:00"));
        } catch (Exception exc) {
            firstTime = Calendar.getInstance();
            firstTime.setTime(new java.util.Date());
            firstTime.set(Calendar.YEAR,2000);
        }
        numLines = 0;
        numFails = 0;
    }


    String show() {
        String str = "";
        String head = "timeStamp \t\t facilityInfo \t\t\t  deviceName modbus "
                + "  Serial TimeAlive Status"
                + " ReadMode Voltage numberOfAttachedSensors\r\n";

        str = str.format("%s \t %s \t %s \t %d "
                + "\t %s \t %d \t %d"
                + "\t %d \t %d \t %d",
                timeStamp, facilityInfo, deviceCommonName, modbusAddr,
                deviceSerialNumber, deviceTimeAlive, deviceStatus,
                dataReadMode, deviceVoltage, numberOfAttachedSensors);
        return head + str;
    }
}
