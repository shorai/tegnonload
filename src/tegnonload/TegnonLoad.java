/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tegnonload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;

/**
 *
 * @author Chris
 */
public class TegnonLoad {

    static String messageId; 
    Vector<PiLine> piLines = new Vector<PiLine>();

    void runFile(String fileName) {
        try {
            File f = new File(fileName);
            messageId = fileName;
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            String str = br.readLine();

            while (str != null) {
                //if(br.ready()) {
                System.out.println("Read line :" + str);
                PiLine pil =new PiLine(str); 
                piLines.add(pil);
                System.out.println(pil.show());
                System.out.println(pil.sensors[0].head());
                for (int j=0; j < pil.numberOfAttachedSensors; j++) {
                    if (pil.sensors[j] != null) {
                        System.out.println("   " + pil.sensors[j].show());
                    } else {
                        System.out.println("Sensor Null   ");
                    }
                }
                str = br.readLine();
                System.out.println("");
                //for (int i = 0; i < strs.length; i++) {
                //    System.out.println("[" + i + "] " + strs[i]);
            }
            Sensor.writeSQL(messageId);
            
        } catch (Exception exc) {
            System.out.println(exc.toString());
            exc.printStackTrace();
        }
        System.out.println("Pilines scanned = " + piLines.size());
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        System.out.println(args[0]);
        for (int i = 0; i < args.length; i++) {
            System.out.println("" + i + "  " + args[i]);
        }
        TegnonLoad x = new TegnonLoad();
        Device.load();
        Sensor.load();
        
        Device.dump();
        Sensor.dump();
        
        x.runFile(args[0]);

       
        
    }
   
}
