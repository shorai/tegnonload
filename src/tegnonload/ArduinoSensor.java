/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tegnonload;

import java.util.logging.Level;
import java.util.logging.Logger;
import static tegnonload.PiLine.logger;

/**
 *
 * @author Chris
 */
public class ArduinoSensor {

    static final Logger logger = TegnonLoad.tegnonLogger.getLogger("ArduinoSensor");
    
    static int numBadValues = 0;
    static int numSensorReadings = 0;
    static int numShortLines = 0;
    static int numFailedReads = 0;

    int sensorType;     //Refers to TID in SensorType Table
    int sensorStatus;   //Refers to TID in Sensor Status Table
    Double sensorValue;    //Raw reading
    int sensorUnits;    //Refers to TID in Sensor Units Table
    int measurmentType; //Refers to TID in MeasureType Table

    static {
         //logger.setLevel(Level.WARNING);
        logger.addHandler(TegnonLoad.logHandler);
    }

    
   
    ArduinoSensor( int lineNumber, String[] strs, int index) throws Exception {
        if (strs.length >= index + 4) {
            sensorType = Integer.decode(strs[index++]);
            //if (sensorType==20) {
             //   sensorType = 19;
            //}
            sensorStatus = Integer.decode(strs[index++]);
            if (sensorStatus == 65535) {
                numFailedReads++;
            }
            try {
                sensorValue = Double.parseDouble(strs[index++]);
            } catch (Exception exc) {
                logger.log(Level.SEVERE, "LineNumber "+ (lineNumber+1) + ":" + index, exc);
                if (strs[index].equals("T100")) {
                    sensorValue = -100.0;
                } else {
                    sensorValue = -999.0;
                }
                numBadValues++;
            }
            sensorUnits = Integer.decode(strs[index++]);
            measurmentType =Integer.decode(strs[index++]);
            
            // 8 June 2016 - Koos vdW email 
            // Dew point reading.... comes in as a 16 bit signed integer scaled by 100
            //     REading from PI fails if dewpoint < 0 degrees centigrade (intteger wrap to 327.67
            // here we correct for it
            if ((sensorType==35) && (sensorValue > 163.84)) {
                sensorValue-=327.67;
                logger.info("Corrected Dewpoint to " + sensorValue);
            }
            
            numSensorReadings++;
        } else {
            numShortLines++;
            throw new Exception("ArduinoSensor() Not enough parameters Line " +lineNumber + ":" + index);
            
        }
    }
static void zeroStat() {
    numBadValues = 0;
    numSensorReadings = 0;
    numShortLines = 0;
    numFailedReads = 0;
}
    String head() {
        return "SType: \tStatus \t   Value \tUnits \tMeasure";
    }
 public String toString() {
        return "Arduino:" + sensorType + " Status:"+ sensorStatus + " Value:"+ sensorValue;
    }
    String show() {
        String str = "";
        str = str.format(" %d \t%d \t%8.2f \t%d \t%d",
                sensorType, sensorStatus, sensorValue, sensorUnits, measurmentType);
        return str;
    }

}
