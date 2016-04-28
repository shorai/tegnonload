/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tegnonload;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author chris.rowse
 */
public class Hierarchy {
    
    static final Logger logger = Logger.getLogger("Hierarchy");
    static HashMap<String,Hierarchy> byLocation = new HashMap<String,Hierarchy>();
    
       int countryID;
      String country;
      int clientID;
      String client;
      int siteID;
      String site;
      int locationID;
      String location;
      int networkID;
      String network;
      int lineID;
      String line;
      int deviceID;
      String device;
      int sensorID;
      int sensorTypeTID;
      String sensor;
      
      
      Hierarchy(ResultSet r) {
      try {
    int i = 1;
        countryID = r.getInt(i++);
       country = r.getString(i++);
       clientID = r.getInt(i++);
       client = r.getString(i++);
       siteID = r.getInt(i++);
       site = r.getString(i++);
       locationID = r.getInt(i++);
       location = r.getString(i++);
       networkID = r.getInt(i++);
       network = r.getString(i++);
       lineID = r.getInt(i++);
       line = r.getString(i++);
       deviceID = r.getInt(i++);
       device = r.getString(i++);
       sensorID = r.getInt(i++);
       sensorTypeTID = r.getInt(i++);
       sensor = r.getString(i++);
       
       byLocation.put(key(),this);
      } catch (Exception exc) {
          logger.log(Level.SEVERE,"Create Hierarchy "+ exc.getMessage(),exc);
      }
         
      }
      
      static void load(Connection conn) {
          try {
              ResultSet rs = conn.createStatement().executeQuery("select * from Heirarchy");
              while(rs.next()) {
                  new Hierarchy(rs);
              }
          } catch(Exception exc) {
                logger.log(Level.SEVERE,"Hierarchy.load() "+ exc.getMessage(),exc);
          }
      }
      
      String key() {
          return country + "." + client + "." + site + "." + location;  
      }
      
       static  String key(String country,String client,String site,String location) {
          return country + "." + client + "." + site + "." + location;  
      }
       
       static Hierarchy find(String loc) { 
           return byLocation.get(loc);
       }    
}