/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tegnonload;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chris
 */
public class ArduinoSensor {

    static final Logger logger = Logger.getLogger("ArduinoSensor");

    int sensorType;     //Refers to TID in SensorType Table
    int sensorStatus;   //Refers to TID in Sensor Status Table
    Double sensorValue;    //Raw reading
    int sensorUnits;    //Refers to TID in Sensor Units Table
    int measurmentType; //Refers to TID in MeasureType Table

    static {
        logger.addHandler(TegnonLoad.logHandler);
    }

    ArduinoSensor( int lineNumber, String[] strs, int index) throws Exception {
        if (strs.length >= index + 4) {
            sensorType = Integer.decode(strs[index++]);
            //if (sensorType==20) {
             //   sensorType = 19;
            //}
            sensorStatus = Integer.decode(strs[index++]);
            try {
                sensorValue = Double.parseDouble(strs[index++]);
            } catch (Exception exc) {
                logger.log(Level.SEVERE, "LineNumber "+ (lineNumber+1) + ":" + index, exc);
                if (strs[index].equals("T100")) {
                    sensorValue = -100.0;
                } else {
                    sensorValue = -999.0;
                }
            }
            sensorUnits = Integer.decode(strs[index++]);
            measurmentType = Integer.decode(strs[index++]);
        } else {
            throw new Exception("ArduinoSensor() Not enough parameters Line " +lineNumber + ":" + index);
        }
    }

    String head() {
        return "SType: \tStatus \t   Value \tUnits \tMeasure";
    }

    String show() {
        String str = "";
        str = str.format(" %d \t%d \t%8.2f \t%d \t%d",
                sensorType, sensorStatus, sensorValue, sensorUnits, measurmentType);
        return str;
    }

}
