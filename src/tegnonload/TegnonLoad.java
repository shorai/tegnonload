/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tegnonload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
 *
 * @TODO: Attachment IDs a re a problem since Hendrik no longer creates the
 * email and attachment records
 * @TODO: AttchmentIDs - there is insufficient data to create them. Lost in the
 * email download
 */
public class TegnonLoad {

    // These two vars allow us to test without detroying data
    static final boolean DEBUG = false;
    /**
     * if UPDATE_DATA is false, programs goes through the motions without real
     * DB updates AThe files will be left unprocessed in the input directory
     */
    static final boolean UPDATE_DATA = true;

    /**
     * If set, data are moved from wherever to processed/za/client/site/ folder
     * Allows to reprocess processed files in situ in processed directory Useful
     * for big reruns
     */
    static boolean MOVE_DATA = true;

    static final int LOG_SIZE = 1000000;
    static final int LOG_ROTATION_COUNT = 10;

    static final int NUMBER_OF_FILES_TO_RUN = 4000;   // @TODO: Crashed 15 April 2016 on 14K files of about 20K
    // possibly need to garbage collect every 5000 or so files
    static public final Logger tegnonLogger = Logger.getLogger("tegnonload");
    static Handler logHandler = null;

    static final String dirName = "C:\\Tegnon\\tegnonefficiencydatagmail.com\\za.tegnon.consol@gmail.com";
    static final String outName = "C:\\Tegnon\\tegnonefficiencydatagmail.com\\processed";

    static final String insertStatSQL = "insert into LoadStats (newFilePath,fileName,fileSize,"
            + "facility,DateCreated,DateProcessed,"
            + "readLines,insufficientParameters,"
            + "DeviceInserts,SensorInserts,"
            + "StatEnergy,StatFlow,StatInserted,StatUpdated,"
            + "PiLines,PiFails,"
            + "ArdBadValues,ArdReadings,"
            + "ArdShortLines,ArdFailed,milliseconds,"
            + "dataNormalInserts,dataNormalUpdates,dataNormalErrors,"
            + "dataHourEnergy,dataHourFlow,dataHourInserts, dataHourUpdates,dataHourError"
            + ")values(?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?)";
    static PreparedStatement loadStatStatement = null;
    //static String dirName = "D:/Tegnon/logs/WSSVC2";
    static String messageId;

    static int insufficientParameters = 0;
    static int readLines = 0;
    Vector<PiLine> piLines = new Vector<PiLine>();

    public static Connection conn;

    /**
     * The TEgnon logs have been made available through the PHP / Laravel
     * programs
     *
     * Access is unrestricted and at <URL>/TegLog  <URL>/TegLog0 .. <URL>/TegLog9
     *
     * These are available under the Admin tab
     *
     *
     * Normal logging is at the INFO level, use FINE, FINER FINEST for debugging
     *
     */
    static {
        try {
            new File("logs").mkdir();
            logHandler = new FileHandler("c:/inetpub/wwwroot/app/storage/TegnonLogs/TegnonLoad.log", LOG_SIZE, LOG_ROTATION_COUNT);
            logHandler.setFormatter(new SimpleFormatter());
            logger.setLevel(Level.INFO);
            logger.setUseParentHandlers(false);

            Handler h[] = logger.getHandlers();
            logger.info("There were " + h.length + " log handlers");
            logger.addHandler(logHandler);
            h = logger.getHandlers();
            logger.info("There are " + h.length + " log handlers");
        } catch (Exception exc) {
            System.out.println("Failed to create a log ... Aaargh");
            System.exit(2);
        }

    }

    static public void connectSQL() {
        // Create a variable for the connection string.
        String username = "javaUser1";
        String password = "sHxXWij02AE4ciJre7yX";

        String connectionUrl = "jdbc:jtds:sqlserver://localhost/TegnonEfficiency";

        try {

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
        SensorDataHour.zeroStat();
        SensorDataNormal.zeroStat();
    }

    void insertStat(String newFilePath, String fileName, long fileSize,
            String facility, LocalDateTime created, long milliseconds) {
        int i = 1;
        String fname = fileName.substring(fileName.lastIndexOf('\\') + 1);
        try {
            if (loadStatStatement == null) {
                loadStatStatement = conn.prepareStatement(insertStatSQL);
            }

            loadStatStatement.setString(i++, newFilePath);
            loadStatStatement.setString(i++, fileName);
            loadStatStatement.setLong(i++, fileSize);
            loadStatStatement.setString(i++, facility);
            loadStatStatement.setTimestamp(i++, java.sql.Timestamp.valueOf(created));

            loadStatStatement.setTimestamp(i++, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            loadStatStatement.setInt(i++, readLines);
            loadStatStatement.setInt(i++, insufficientParameters);
            loadStatStatement.setInt(i++, Device.inserts);
            loadStatStatement.setInt(i++, Sensor.inserts);

            loadStatStatement.setInt(i++, Statistic.numEnergy);
            loadStatStatement.setInt(i++, Statistic.numFlow);
            loadStatStatement.setInt(i++, Statistic.numInserted);
            loadStatStatement.setInt(i++, Statistic.numUpdated);
            loadStatStatement.setInt(i++, PiLine.numLines);

            loadStatStatement.setInt(i++, PiLine.numFails);
            loadStatStatement.setInt(i++, ArduinoSensor.numBadValues);
            loadStatStatement.setInt(i++, ArduinoSensor.numSensorReadings);
            loadStatStatement.setInt(i++, ArduinoSensor.numShortLines);
            loadStatStatement.setInt(i++, ArduinoSensor.numFailedReads);

            loadStatStatement.setLong(i++, milliseconds);
            loadStatStatement.setLong(i++, SensorDataNormal.numInserts);
            loadStatStatement.setLong(i++, SensorDataNormal.numUpdates);
            loadStatStatement.setLong(i++, SensorDataNormal.numErrors);

            //dataHourEnergy,dataHourFlow,dataHourInserted,dataHourUpdated,dataHourError
            loadStatStatement.setInt(i++, SensorDataHour.numEnergy);
            loadStatStatement.setInt(i++, SensorDataHour.numFlow);
            loadStatStatement.setInt(i++, SensorDataHour.numInserted);
            loadStatStatement.setInt(i++, SensorDataHour.numUpdated);
            loadStatStatement.setInt(i++, SensorDataHour.numErrors);

            loadStatStatement.executeUpdate();
        } catch (SQLException sexc) {
            logger.log(Level.SEVERE, "Failed to log Statistice " + sexc.getMessage(), sexc);
        }
    }

    void runFile(String fileName, boolean moveData) {
        LocalDateTime milliseconds = LocalDateTime.now();
        long filesize = 0;
        File f = null;
        String newPath = "";
        try {
            zeroStats();
            f = new File(fileName);
            filesize = f.length();
            messageId = fileName;
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            String str = br.readLine();
            logger.info("Opening File " + fileName);
            String facilityInfo;

            while (str != null) {

                readLines++;
                if (str.trim().length() > 0) {
                    String[] strs = str.split("[|]");
                    if (strs.length > 1) {
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

            Sensor.writeSQL(-1); //messageId);

            System.out.println(Statistic.getSqlStat());
            logger.info(Statistic.getSqlStat());
            LocalDateTime created = PiLine.firstTime;

            newPath = PiLine.facility.replace(".", "/");

            insertStat(newPath, f.getName(), filesize, PiLine.facility, created, LocalDateTime.now().compareTo(milliseconds));

            Sensor.zeroTots();

            if ((UPDATE_DATA) && (moveData)) {  // move files from load directory to processed
                String newName = outName + File.separatorChar + newPath + File.separatorChar + f.getName();
                try {
                    File np = new File(newName);
                    // CopyOptions not all implemented or working, ATOMIC excludes others
                    np.mkdirs();
                    java.nio.file.Files.move(f.toPath(), np.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);

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

    }

    public void runDirectory(String dir, boolean moveData) {
        File f = new File(dir);
        int count = 0;

        if (f.isDirectory()) {
            System.out.println("Processing directory: " + dir);
            for (File g : f.listFiles()) {
                if (g.getName().endsWith(".txt")) {
                    runFile(g.getAbsolutePath(), moveData);
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

    public void preload() {

        try {
            File f = new File(outName);
            f.mkdirs();
        } catch (Exception exd) {
            logger.log(Level.SEVERE, outName + " Failed to make dirs:" + exd.getMessage(), exd);
        }

        try {
            connectSQL();
            Statistic.prepare(conn);

            Hierarchy.load(conn);
            Device.loadSQL(conn);
            if (DEBUG) {
                Device.dump();

            }
            SensorDataNormal.init(conn);
            Sensor.loadSQL(conn);
            if (DEBUG) {
                Sensor.dump();
            }
        } catch (SQLException sexc) {
            logger.log(Level.SEVERE, "SQL Exception " + sexc.getMessage(), sexc);
        } catch (Exception exc) {
            logger.log(Level.SEVERE, "SQL Exception " + exc.getMessage(), exc);
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        TegnonLoad x = new TegnonLoad();
        x.preload();
        x.runDirectory(dirName, MOVE_DATA);

    }

    private class MyFormatter extends java.util.logging.Formatter {

        @Override
        public String format(LogRecord lr) {

            return String.format("%s %s %s %s %d %s", lr.getLevel(),
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(lr.getMillis()), ZoneId.systemDefault()),
                    lr.getLoggerName(),
                    lr.getSourceClassName(), lr.getThreadID(),
                    lr.getThrown());

        }

    }

}
