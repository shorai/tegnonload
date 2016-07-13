/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tegnonload;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chris
 */
public class PiLine {

    static final Logger logger = TegnonLoad.tegnonLogger.getLogger("tegnonload.PiLine");
    static final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static int numLines = 0;

    static int numFails = 0;
    static String facility = null;
    static LocalDateTime firstTime = null;

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
        logger.setUseParentHandlers(false);

        logger.addHandler(TegnonLoad.logHandler);
    }

    PiLine(String[] strs) throws Exception {

        int i = 0;
        timeStamp = strs[i++];
        facilityInfo = strs[i++]; // array index out of bounds   1??
        deviceCommonName = strs[i++];
        modbusAddr = Integer.decode(strs[i++]);
        try {
            deviceSerialNumber = Integer.parseInt(strs[i++]);
        } catch (Exception exd) {
            logger.warning("Device Serial nUmber not numeric:" + strs[i - 1] + exd.getMessage());
            deviceSerialNumber = -1;
        }
        try {
            deviceTimeAlive = Integer.decode(strs[i++]);
        } catch (Exception exc) {
            logger.warning("DeviceTimeAlive not numeric :" + strs[i - 1] + " " + exc.getMessage());
            deviceTimeAlive = -100;
        }
        deviceStatus = Integer.decode(strs[i++]);
        dataReadMode = Integer.decode(strs[i++]);
        deviceVoltage = Integer.decode(strs[i++]);
        numberOfAttachedSensors = Integer.decode(strs[i++]);

        facility = facilityInfo;

        firstTime = LocalDateTime.parse(timeStamp, df);

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
                Sensor s = Sensor.find(d, j + 1, as.sensorType);
                if (s == null) {
                    s = new Sensor(d, as, j + 1);
                }

                LocalDateTime ldt = LocalDateTime.parse(timeStamp, df);
                if (ldt == null) {
                    logger.severe("LocalDateTiem wouldn'yt parse PiLine(strs[])");
                }
                Statistic stat = s.getStat(ldt);
                if (as.sensorStatus == 65535) {
                    logger.warning("Sensor status 655535" + show());
                } else {
                    stat.add(ldt, as.sensorValue);
                }
                SensorDataNormal.instance.save(stat, ldt, as.sensorValue);
                j++;
                numLines++;
            } catch (Exception exc) {
                logger.log(Level.SEVERE, "Cant add stat " + strs + " Device:" + facilityInfo + "|" + modbusAddr + "|" + deviceSerialNumber + " Date:" + timeStamp, exc);
                numFails++;
            }
        }
    }

    static public void zeroStat() {
        facility = "Unknown";

        firstTime = LocalDateTime.of(2000, 01, 01, 00, 00);

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
