/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tegnonload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chris
 */
public class TegnonLoad {

    static final int LOG_SIZE = 1000000;
    static final int LOG_ROTATION_COUNT = 10;

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
            logger.addHandler(logHandler);
        } catch (Exception exc) {
            System.out.println("Failed to create a log ... Aaargh");
            System.exit(2);
        }

    }

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

    static public void connectSQL() {
        // Create a variable for the connection string.
        String username = "javaUser1";
        String password = "sHxXWij02AE4ciJre7yX";
        String connectionUrl = "jdbc:sqlserver://localhost:1433;"
                + "databaseName=TegnonEfficiency";

//			"databaseName=TegnonEfficiency;integratedSecurity=true;";
        // Declare the JDBC objects.
        try {
            // Establish the connection.
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            conn = DriverManager.getConnection(connectionUrl, username, password);
            System.out.println("Connection Succeeded");

        } catch (Exception exc) {
            exc.printStackTrace();
            logger.log(Level.SEVERE, exc.getMessage(), exc);
        }
        
     

    }

    void runFile(String fileName) {
        try {
            File f = new File(fileName);
            messageId = fileName;
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            String str = br.readLine();

            while (str != null) {
                //if(br.ready()) {
                //System.out.println("Read line :" + str);
                if (str.trim().length() > 0) {
                    PiLine pil = new PiLine(str);
                    piLines.add(pil);
                }
                //System.out.println(pil.show());
                //System.out.println(pil.sensors[0].head());
                /*
                for (int j = 0; j < pil.numberOfAttachedSensors; j++) {
                    if (pil.sensors[j] != null) {
                        System.out.println("   " + pil.sensors[j].show());
                    } else {
                        System.out.println("Sensor Null   ");
                    }
                }
                
                
                
                 */
                str = br.readLine();
                //System.out.println("");
                //for (int i = 0; i < strs.length; i++) {
                //    System.out.println("[" + i + "] " + strs[i]);
            }
            Sensor.writeSQL(messageId);
            Sensor.zeroTots();
            // can throw exception
            try {
                if (f.renameTo(new File(outName + f.getName()))) {
                    System.out.println("File is moved successful! to ");
                    logger.finest("Success " + f.getAbsolutePath() + " \t" + outName);
                } else {
                    System.out.println("File is failed to move!");
                    logger.info("Failed move " + f.getAbsolutePath() + " \t" + outName);
                }
            } catch (Exception exd) {
                logger.severe("Exception move " + f.getAbsolutePath() + " \t" + outName);
                logger.log(Level.SEVERE, exd.getMessage(), exd);
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
                if (count > 3) {
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
        if (args.length > 0) {
            System.out.println(args[0]);
            for (int i = 0; i < args.length; i++) {
                System.out.println("" + i + "  " + args[i]);
            }
        }
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
        /*
        Device.dump();
        Sensor.dump();
         */
        conn = null;
        TegnonLoad x = new TegnonLoad();

        //x.runFile(args[0]);
        x.runDirectory(dirName);
    }

}
