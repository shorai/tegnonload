/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tegnonload;

/**
 *
 * @author Chris
 */
public class ArduinoSensor {
    
        int     sensorType;     //Refers to TID in SensorType Table
        int     sensorStatus;   //Refers to TID in Sensor Status Table
        Double  sensorValue;    //Raw reading
        int     sensorUnits;    //Refers to TID in Sensor Units Table
        int     measurmentType; //Refers to TID in MeasureType Table

        ArduinoSensor(String[] strs, int index) throws Exception {
            if (strs.length >= index + 4) {
                sensorType = Integer.decode(strs[index++]);
                sensorStatus = Integer.decode(strs[index++]);
                sensorValue = Double.parseDouble(strs[index++]);
                sensorUnits = Integer.decode(strs[index++]);
                measurmentType = Integer.decode(strs[index++]);
            } else {
                throw new Exception("Not enough parameters" + index);
            }
        }
        
        String head() {
            return "SType: \tStatus \t   Value \tUnits \tMeasure";
        }
        
        String show() {
            String str = "";
            str = str.format(" %d \t%d \t%8.2f \t%d \t%d", 
                    sensorType,sensorStatus,sensorValue,sensorUnits,measurmentType);
            return str;
                    }

}
