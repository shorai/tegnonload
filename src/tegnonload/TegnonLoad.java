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
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author Chris
 */
public class TegnonLoad {

    // These two vars allow us to test without detroying data
    static final boolean DEBUG = false;
    static final boolean UPDATE_DATA = true;

    static final int LOG_SIZE = 1000000;
    static final int LOG_ROTATION_COUNT = 10;

    static final int NUMBER_OF_FILES_TO_RUN =10000;   // @TODO: Crashed 15 April 2016 on 14K files of about 20K

    static final Logger logger = Logger.getLogger("TegnonLoad");
    static Handler logHandler = null;

    static final String dirName = "C:\\Tegnon\\tegnonefficiencydatagmail.com\\za.tegnon.consol@gmail.com";
    static final String outName = "C:\\Tegnon\\tegnonefficiencydatagmail.com\\processed";

    //static String dirName = "D:/Tegnon/logs/WSSVC2";
    static String messageId;
    Vector<PiLine> piLines = new Vector<PiLine>();

    public static Connection conn;

    static {
        try {
            logHandler = new FileHandler("TegnonLoad.log", LOG_SIZE, LOG_ROTATION_COUNT);
            logHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(logHandler);
        } catch (Exception exc) {
            System.out.println("Failed to create a log ... Aaargh");
            System.exit(2);
        }

    }

    /*
    static void connect() {
        try {
            String driver = "sun.jdbc.odbc.JdbcOdbcDriver";
            String url = "jdbc:odbc:javaUser";
            String username = "javaUser1";
            String password = "sHxXWij02AE4ciJre7yX";
            Class.forName(driver);
            conn = DriverManager.getConnection(url, username, password);
        } catch (Exception exc) {
            System.out.println(exc);
        }

    }
     */
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

    void runFile(String fileName) {
        try {
            File f = new File(fileName);
            messageId = fileName;
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            String str = br.readLine();
            logger.info("Opening File "+fileName);
            
            while (str != null) {
                //if(br.ready()) {
                //System.out.println("Read line :" + str);
                if (str.trim().length() > 0) {
                    String[] strs = str.split("[|]");
                    if (strs.length > 10) {
                        try {
                            PiLine pil = new PiLine(strs);
                            piLines.add(pil);
                        } catch (Exception exc) {
                            System.out.println("Exception creating PiLine from :" + str);
                            logger.log(Level.SEVERE, "new PiLine", exc);
                        }
                    }
                }

                str = br.readLine();

            }
            br.close();
            Sensor.writeSQL(-1); //messageId);
            System.out.println(Statistic.getSqlStat());
            logger.info(Statistic.getSqlStat());

            Sensor.zeroTots();
            // can throw exception
            if (UPDATE_DATA) {
                String newName = outName + File.separator + f.getName();
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
        connectSQL();
        Statistic.prepare(conn);

        Device.load();
        Sensor.load();
        if (DEBUG) {
            Device.dump();
            Sensor.dump();
        }
        //conn = null;
        TegnonLoad x = new TegnonLoad();

        //x.runFile(args[0]);
        x.runDirectory(dirName);
    }

}
