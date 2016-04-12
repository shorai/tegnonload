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
public class Statistic {
    
    Sensor sensor;
    String startTime;
    String endTime;
        
    
    Double first;
    Double last;
    Double sum;
    Double sumSquares;
    Double max;
    Double min;
    int    count;
    
    
    public Statistic(Sensor s) {
        sensor = s;
      
        first = 0.0;
        last = 0.0;
        sum = 0.0;
        sumSquares = 0.0;
        max = 0.0;
        min =  0.0;
        count = 0;
    }
    
    public void add(String time, Double value){
        
        if (count ==0) {
            first = value;
            min = value;
            startTime = time;
            endTime = time;
        } else { 
            last = value;
            if (time.compareTo(startTime) < 0)
                startTime = time;
            if (time.compareTo(endTime) > 0)
                endTime = time;       
        }
        sum+=value;
        sumSquares += (value*value);
        if (max < value) 
            max = value;
        if (min > value)
            min = value;
        count++;
    }
    
    String dump() {
        return sensor.key() + "\t" + startTime + "\t" + endTime + "\t"
                + first + "\t" + last + "\t" + sum + "\t" 
                + max + "\t" + min + "\t" + count;
    }
    
    String writeFlowSQL(String messageId) {
        if (sum > 0.00) {
        String str =  "Flow:" + messageId+ ","+ sensor.id+ ","+ startTime+ ","+ sensor.typeTID+ ","+ count+ ","+ sum+ ","+max+ ","+ min+ ","+ sumSquares;
        str = String.format( "values('%s',%d,'%s',%d,%d,%8.4f,%8.4f,%8.4f,%12.4f)",
                messageId, sensor.id, startTime, sensor.typeTID, count, sum,max, min, sumSquares);
        return "insert into SensorDataHalfHour(attachmentID, sensorId, startTime, sensorType, recordCount, sensorValue,maximum, minimum, sumOfSquares)"
                   +str;
        } else {
            return null;
        }
    }
    
     String writeEnergySQL(String messageId) {
       
        Double val = last-first;
        if (val < 0) val +=32767;
        if (val > 0.00) {
        String str =  "Energy:" + messageId+ ","+ sensor.id+ ","+ startTime+ ","+ sensor.typeTID+ ","+ count+ ","+ sum+ ","+max+ ","+ min+ ","+ sumSquares;
        
        str = String.format("values('%s',%d,'%s',%d,%d,%8.4f,%8.4f,%8.4f,%12.4f)",
                messageId, sensor.id, startTime, 19, 1, val, last, first, val*val);
        return "insert into SensorDataHalfHour(attachmentID, sensorId, startTime, sensorType, recordCount, sensorValue,maximum, minimum, sumOfSquares)"
                   + str;
        } else {
            return null;
        }
    }
     
      
}
    
    
