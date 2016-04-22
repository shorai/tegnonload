/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tegnonload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import static tegnonload.PiLine.logger;

/**
 *
 * @author Chris
 */
public class TegnonLoad {

    // These two vars allow us to test without detroying data
    static final boolean DEBUG = true;
    static final boolean UPDATE_DATA = true;

    static final int LOG_SIZE = 1000000;
    static final int LOG_ROTATION_COUNT = 10;

    static final int NUMBER_OF_FILES_TO_RUN = 4000;   // @TODO: Crashed 15 April 2016 on 14K files of about 20K

    static final Logger logger = Logger.getLogger("TegnonLoad");
    static Handler logHandler = null;

    static final String dirName = "C:\\Tegnon\\tegnonefficiencydatagmail.com\\za.tegnon.consol@gmail.com";
    static final String outName = "C:\\Tegnon\\tegnonefficiencydatagmail.com\\processed";

    static final DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
/*
static final String createTableSQL = "create table LoadStats (
	id int not null IDENTITY(1,1) PRIMARY KEY, 
	newFilePath varchar(255),	
	fileName varchar(255),        
	fileSize int,
        facility  varchar(255),
        DateCreated DateTime,
	DateProcessed DateTime,
	readLines int,
        insufficientParameters int,
        DeviceInserts int,
        SensorInserts int,
        StatEnergy int,
        StatFlow int,
        StatInserted int,
        StatUpdated int,
        PiLines int,
        PiFails int,
        ArdBadValues int,
        ArdReadings int,
        ArdShortLines int,
	ArdFailed int;
)");     
*/
static final String insertStatSQL = "insert into LoadStats (newFilePath,fileName,fileSize,"+
        "facility,DateCreated,DateProcessed,"+
	"readLines,insufficientParameters,"+
        "DeviceInserts,SensorInserts,"+
        "StatEnergy,StatFlow,StatInserted,StatUpdated,"+
        "PiLines,PiFails,"+
        "ArdBadValues,ArdReadings,"+
        "ArdShortLines,ArdFailed";
static PreparedStatement loadStatStatement = null;
    //static String dirName = "D:/Tegnon/logs/WSSVC2";
    static String messageId;
    
static int insufficientParameters = 0;
        static int readLines = 0;
    Vector<PiLine> piLines = new Vector<PiLine>();

    public static Connection conn;

    //java.util.logging.Formatter lf = new SimpleFormatter();
    /**
     * The TEgnon logs have been made available throught he PHP / Laravel
     * programs
     *
     * Access is unrestricted and at <URL>/TegLog  <URL>/TegLog0 .. <URL>/TegLog9
     *
     * These should be brought under the Admin page
     *
     *
     * Normal logging is at the INFO level, use FINE, FINER FINEST for debugging
     *
     */
    static {
        try {
            new File("logs").mkdir();
            // logHandler = new FileHandler("logs/TegnonLoad.log", LOG_SIZE, LOG_ROTATION_COUNT);
            logHandler = new FileHandler("c:/inetpub/wwwroot/app/storage/TegnonLogs/TegnonLoad.log", LOG_SIZE, LOG_ROTATION_COUNT);
            logHandler.setFormatter(new SimpleFormatter());
            logger.setLevel(Level.INFO);
            logger.addHandler(logHandler);
        } catch (Exception exc) {
            System.out.println("Failed to create a log ... Aaargh");
            System.exit(2);
        }

    }

    static public void connectSQL() {
        // Create a variable for the connection string.
        String username = "javaUser1";
        String password = "sHxXWij02AE4ciJre7yX";
        String connectionUrl = "jdbc:sqlserver://localhost:1433;"
                + "databaseName=TegnonEfficiency";

        // Declare the JDBC objects.
        try {
            // Establish the connection.
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            conn = DriverManager.getConnection(connectionUrl, username, password);
            System.out.println("Connection Succeeded");

        } catch (Exception exc) {
            exc.printStackTrace();
            logger.log(Level.SEVERE, exc.getMessage(), exc);
            System.exit(3);
        }

    }
    
    void zeroStats() {
        insufficientParameters = 0;
        readLines = 0;
        Device.zeroStat();
        Sensor.zeroStat();
        Statistic.zeroStat();
        PiLine.zeroStat();
        ArduinoSensor.zeroStat();
    }
    
    void insertStat(String newFilePath, String fileName, long fileSize,
            String facility, java.sql.Time dateCreated ) {
        int i = 0;
        try { 
        loadStatStatement.setString(i++,newFilePath);
	loadStatStatement.setString(i++,fileName);
	loadStatStatement.setLong(i++,fileSize);
        loadStatStatement.setString(i++,facility);
	loadStatStatement.setTime(i++,dateCreated);
	loadStatStatement.setTime(i++,new java.sql.Time(new java.util.Date().getTime()));
	loadStatStatement.setInt(i++,readLines);
	loadStatStatement.setInt(i++,insufficientParameters);
        loadStatStatement.setInt(i++,Device.inserts);
	loadStatStatement.setInt(i++,Sensor.inserts);
        loadStatStatement.setInt(i++,Statistic.numEnergy);
	loadStatStatement.setInt(i++,Statistic.numFlow);
	loadStatStatement.setInt(i++,Statistic.numInserted);
	loadStatStatement.setInt(i++,Statistic.numUpdated);
        loadStatStatement.setInt(i++,PiLine.numLines);
	loadStatStatement.setInt(i++,PiLine.numFails);   
        loadStatStatement.setInt(i++,ArduinoSensor.numBadValues);
	loadStatStatement.setInt(i++,ArduinoSensor.numSensorReadings);
        loadStatStatement.setInt(i++,ArduinoSensor.numShortLines);
	loadStatStatement.setInt(i++,ArduinoSensor.numFailedReads);
        
        loadStatStatement.executeUpdate();
        } catch (SQLException sexc) { 
            logger.log(Level.SEVERE,"Failed to log Statistice "+ sexc.getMessage(),sexc);
        }
    }
       
      

    void runFile(String fileName) {
        File f= null;
        String newPath= "";
        try {
            zeroStats();
            f = new File(fileName);
            messageId = fileName;
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            String str = br.readLine();
            logger.info("Opening File " + fileName);
                String facilityInfo;
            
            while (str != null) {
                
        readLines++;
                //if(br.ready()) {
                //System.out.println("Read line :" + str);
                if (str.trim().length() > 0) {
                    String[] strs = str.split("[|]");
                    if(strs.length > 1) {
                               facilityInfo = strs[1];
                 
                    } else {
                        facilityInfo = "Unknown facility";
                    }
                    if (strs.length > 10) {
                        try {
                            PiLine pil = new PiLine(strs);
                            piLines.add(pil);
                        } catch (Exception exc) {

                            logger.log(Level.SEVERE, "new PiLine " + str, exc);
                        }
                    } else {
                       insufficientParameters++;
                    }
                }

                str = br.readLine();

            }
            br.close();
            //conn.executeQuery("Begin trans " + filename);
            Sensor.writeSQL(-1); //messageId);
            //conn.executeQuery("commit trans " + filename);
            conn.commit();
            System.out.println(Statistic.getSqlStat());
            logger.info(Statistic.getSqlStat());

            Sensor.zeroTots();
            // can throw exception
            if (UPDATE_DATA) {
                newPath = PiLine.facility.replaceAll(".","/");
                String newName = outName + File.separatorChar + newPath+ File.separatorChar + f.getName();
                try {
                    // CopyOptions not all implemented or working, ATOMIC excludes others
                    java.nio.file.Files.move(f.toPath(), new File(newName).toPath(),
                            StandardCopyOption.REPLACE_EXISTING); //, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.COPY_ATTRIBUTES);

                    System.out.println("File moved successfully to " + newName);
                    logger.finest("File moved successfully to " + newName);

                } catch (Exception exd) {
                    logger.severe("Exception move " + f.getAbsolutePath() + " \t" + newName);
                    logger.log(Level.SEVERE, exd.getMessage(), exd);
                }
            }
        } catch (Exception exc) {
            System.out.println(exc.toString());
            exc.printStackTrace();
        }
        System.out.println("Pilines scanned = " + piLines.size());
        //logOnSQL(fileName, facilityInfo);
        java.util.Date created = new java.util.Date(PiLine.firstTime);
        insertStat(newPath, fileName,((f==null)?0: f.length()), PiLine.facility, new java.sql.Time(created.getTime()));
    }

    public void runDirectory(String dir) {
        File f = new File(dir);
        int count = 0;

        if (f.isDirectory()) {
            System.out.println("Processing directory: " + dir);
            for (File g : f.listFiles()) {
                if (g.getName().endsWith(".txt")) {
                    runFile(g.getAbsolutePath());
                    System.out.println(" " + count++ + " Loaded:" + g.getAbsolutePath());
                } else {
                    System.out.println(" " + count + " Did not process:" + g.getAbsolutePath());
                }
                if (count >= NUMBER_OF_FILES_TO_RUN) {
                    break;
                }
            }
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        /*
        if (args.length > 0) {
            System.out.println(args[0]);
            for (int i = 0; i < args.length; i++) {
                System.out.println("" + i + "  " + args[i]);
            }
        }
         */
        try {
            File f = new File(outName);
            f.mkdirs();
        } catch (Exception exd) {
            logger.log(Level.SEVERE, outName + " Failed to make dirs:" + exd.getMessage(), exd);
        }

        try {
            connectSQL();
            Statistic.prepare(conn);

            Device.loadSQL(conn);
             if (DEBUG) {
                Device.dump();
            
            }
            Sensor.loadSQL(conn);
            if (DEBUG) {
                Sensor.dump();
            }
            //conn = null;
            TegnonLoad x = new TegnonLoad();
        } catch (SQLException sexc) {
            logger.log(Level.SEVERE, "SQL Exception " + sexc.getMessage(), sexc);
        } catch (Exception exc) {
            logger.log(Level.SEVERE, "SQL Exception " + exc.getMessage(), exc);
        }
        //x.runDirectory(dirName);
    }

    private class MyFormatter extends java.util.logging.Formatter {

        @Override
        public String format(LogRecord lr) {
            //String str;

            return String.format("%s %s %s %s %d %s", lr.getLevel(),
                    df.format(new java.util.Date(lr.getMillis())), lr.getLoggerName(),
                    lr.getSourceClassName(), lr.getThreadID(),
                    lr.getThrown());

            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

}
