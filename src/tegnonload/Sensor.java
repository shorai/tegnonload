/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tegnonload;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chris
 */
public class Sensor {

    static final String insertSQL = "insert into sensors( DeviceID, SensorTypeTID, SensorUnitTID,MeasurementTypeTID,SensorNumber) values(?,?,?,?,?)";
    static PreparedStatement insertStatement = null;

    static final String findByIdSQL = "select SensorId from Sensors where DeviceID=? and SensorTypeTID = ? and SensorNumber= ?";
    static PreparedStatement findByIdStatement = null;

    static int inserts = 0;

    static final Logger logger = TegnonLoad.tegnonLogger.getLogger("tegnonload.Sensor");

    static HashMap<String, Sensor> sensors = new HashMap<String, Sensor>();

    int id;
    Device device;
    int typeTID;
    int unitTID;
    int measureTID;
    int lineId;
    int netId;
    int columnNumber;

    Statistic stat;// = new Statistic(this,Calendar.getInstance());

    //SensorID	DeviceID	SensorTypeTID	SensorUnitTID	MeasurementTypeTID	LineID	NetworkID	SensorNumber",
    static {
               logger.setUseParentHandlers(false);
        // logger.setLevel(Level.INFO);
        logger.addHandler(TegnonLoad.logHandler);
        //logger.setLevel(Level.INFO);
        //logger.addHandler(TegnonLoad.logHandler);
    }

    public String toString() {
        return ("Sensor [" + id + "] Device:" + device.toString() + " TypeTID:" + typeTID + " Line:" + lineId + " NetID:" + netId + " ColNumber:" + columnNumber);
    }

    public Sensor(ResultSet rs) throws SQLException {
        int i = 1;
        id = rs.getInt(i++);
        int devId = rs.getInt(i++);
        device = Device.findById(devId);
        typeTID = rs.getInt(i++);
        unitTID = rs.getInt(i++);
        measureTID = rs.getInt(i++);
        lineId = rs.getInt(i++);
        netId = rs.getInt(i++);
        columnNumber = rs.getInt(i++);
        sensors.put(key(), this);
        stat = null; // new Statistic(this,Calendar.getInstance());
    }

    public Sensor(Device dev, ArduinoSensor as, int colNumber) throws SQLException {
        //SensorID, DeviceID, SensorTypeTID, SensorUnitTID,MeasurementTypeTID,SensorNumber
        device = dev;
        typeTID = as.sensorType;
        unitTID = as.sensorUnits;
        measureTID = as.measurmentType;
        columnNumber = colNumber;
        sensors.put(key(), this);
        stat = null; // new Statistic(this,Calendar.getInstance());
        try {
            insert();
        } catch (SQLException exc) {
            logger.severe("Sensor insert failed Device:" + dev.toString() + " Arduino:" + as.toString() + " Column: " + colNumber);
        }

    }

    /*
    public Sensor(String vals) {
        String[] strs = vals.split("[\t ]+");

        //System.out.println("Sensor No:" + strs.length + " " + vals);
        int i = 0;

        id = Integer.parseInt(strs[i++]);
        device = Device.findById(Integer.parseInt(strs[i++]));
        typeTID = Integer.parseInt(strs[i++]);
        unitTID = Integer.parseInt(strs[i++]);
        measureTID = Integer.parseInt(strs[i++]);
        try {
            // NULL is quite normal logger.log(Level.SEVERE,"LineID:" + strs[i-1] + " " + exc.getMessage(), exc);
            if (strs[i].equals("NULL")) {
                lineId = -1;
                i++;
            } else {
                lineId = Integer.parseInt(strs[i++]);
            }
        } catch (Exception exc) {
            lineId = -1;

        }
        try {
            // NULL is quite normal logger.log(Level.SEVERE,"NetID:" + strs[i-1] + " " + exc.getMessage(), exc);
            if (strs[i].equals("NULL")) {
                netId = -1;
                i++;
            } else {
                netId = Integer.parseInt(strs[i++]);
            }
        } catch (Exception exc) {
            netId = -1;

        }
        try {
            columnNumber = Integer.parseInt(strs[i++]);
        } catch (Exception exc) {
            columnNumber = -1;
            logger.log(Level.SEVERE, "Column Count:" + strs[i - 1] + " " + exc.getMessage(), exc);
        }
        //System.out.println("   sensor dev:"+ device.key() + "," + id);
    }
     */
    String key() {
        return "" + device.key() + "," + columnNumber + "," + typeTID;
    }

    static final Sensor find(Device dev, int columnNumber, int sensorType) {

        return sensors.get("" + dev.key() + "," + columnNumber + "," + sensorType);

    }

    Statistic getStat(Calendar cal) {
        if (cal == null)
            logger.severe("Cal is null");
        if (stat == null) {
            stat = new Statistic(this,cal);
        }
         stat.setStartTime(cal);
        return stat;
    }
    
    static void zeroStat() {
        inserts = 0;
    }

    static void zeroTots() {
        for (Sensor s : sensors.values()) {
            if (s.stat != null)
                s.stat.zero();
        }
    }

    static void dump() {
        System.out.println("****************************************************** Sensors Dump");

        for (Sensor s : sensors.values()) {
            System.out.println(s.id + " " + s.device.key() + " Devid:" + s.device.deviceID + " Type:" + s.typeTID );
        }

    }

    static void writeSQL(Integer messageId) {
        for (Sensor s : sensors.values()) {
            Statistic stat = s.stat;
            if (stat != null) {
                //stat.setTimes();

                //  if ((s.typeTID == 20) || (s.typeTID == 20)) {
                if (s.typeTID == 20) {
                   if (stat.last != stat.first)  {
                       // logger.info("******************************************************** writeEnergySQL for " + stat.toString());
                        stat.writeEnergySQL(messageId);
                   }
                } else {
                    
                   logger.info("======================================================== writeFlowSQL for " + stat.toString());
                    stat.writeFlowSQL(messageId);
                }
              /*  if (stat.count > 0) {
                    SensorDataHour.instance.addHalfHour(stat); // this does not get called??
                }
                */
            }
            //System.out.println(" SensorDataHour updated for " + count + " records");
        }
    }

    static void loadSQL(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select * from sensors");
        while (rs.next()) {
            new Sensor(rs);
        }
    }
//java.sql.SQLException: The INSERT statement conflicted with the FOREIGN KEY constraint "FK_Sensors_SensorType". The conflict occurred in database "TegnonEfficiency", table "dbo.SensorType", column 'SensorTypeID'.

    void insert() throws SQLException {
        if (insertStatement == null) {
            insertStatement = TegnonLoad.conn.prepareStatement(insertSQL);
            findByIdStatement = TegnonLoad.conn.prepareStatement(findByIdSQL);
        }
        int i = 1;
        //SensorID, DeviceID, SensorTypeTID, SensorUnitTID,MeasurementTypeTID,SensorNumber   
        // insertStatement.setInt(i++, id);
        insertStatement.setInt(i++, device.deviceID);
        insertStatement.setInt(i++, typeTID);
        insertStatement.setInt(i++, unitTID);
        insertStatement.setInt(i++, measureTID);
        insertStatement.setInt(i++, columnNumber);

        insertStatement.executeUpdate();

        findByIdStatement.setInt(1, device.deviceID);
        findByIdStatement.setInt(2, typeTID);
        findByIdStatement.setInt(3, columnNumber);
        ResultSet rs = findByIdStatement.executeQuery();
        rs.next();
        id = rs.getInt(1);
        logger.info("Sensor.insert " + id + " " + device.key() + " Devid:" + device.deviceID + " Type:" + typeTID + " Column:" + columnNumber);
        inserts++;
    }
    /*
    //SensorID	DeviceID	SensorTypeTID	SensorUnitTID	MeasurementTypeTID	LineID	NetworkID	SensorNumber",
    static final String[] sensorData = {
        "657	157	1	4	1	NULL	1	1",
        "658	157	4	12	1	NULL	1	2",
        "659	157	3	1	1	NULL	1	3",
        "660	157	2	6	1	NULL	1	4",
        "661	158	1	4	1	NULL	1	1",
        "662	158	4	12	1	NULL	1	2",
        "663	158	3	1	1	NULL	1	3",
        "664	158	2	6	1	NULL	1	4",
        "665	159	1	4	1	NULL	1	1",
        "666	159	4	12	1	NULL	1	2",
        "667	159	3	1	1	NULL	1	3",
        "668	159	2	6	1	NULL	1	4",
        "669	160	1	4	1	NULL	1	1",
        "670	160	4	12	1	NULL	1	2",
        "671	160	3	1	1	NULL	1	3",
        "672	160	2	6	1	NULL	1	4",
        "673	161	1	4	1	NULL	2	1",
        "674	161	4	12	1	NULL	2	2",
        "675	161	3	1	1	NULL	2	3",
        "676	161	2	6	1	NULL	2	4",
        "677	162	1	4	1	NULL	2	1",
        "678	162	4	12	1	NULL	2	2",
        "679	162	3	1	1	NULL	2	3",
        "680	162	2	6	1	NULL	2	4",
        "681	163	1	4	1	NULL	1	1",
        "682	163	4	12	1	NULL	1	2",
        "683	163	3	1	1	NULL	1	3",
        "684	163	2	6	1	NULL	1	4",
        "685	164	1	4	1	NULL	1	1",
        "686	164	4	12	1	NULL	1	2",
        "687	164	3	1	1	NULL	1	3",
        "688	164	2	6	1	NULL	1	4",
        "689	165	1	4	1	NULL	1	1",
        "690	165	4	12	1	NULL	1	2",
        "691	165	3	1	1	NULL	1	3",
        "692	165	2	6	1	NULL	1	4",
        "693	166	1	4	1	NULL	2	1",
        "694	166	4	12	1	NULL	2	2",
        "695	166	3	1	1	NULL	2	3",
        "696	166	2	6	1	NULL	2	4",
        "697	167	1	4	1	NULL	1	1",
        "698	167	4	12	1	NULL	1	2",
        "699	167	3	1	1	NULL	1	3",
        "700	167	2	6	1	NULL	1	4",
        "701	168	1	4	1	NULL	1	1",
        "702	168	4	12	1	NULL	1	2",
        "703	168	3	1	1	NULL	1	3",
        "704	168	2	6	1	NULL	1	4",
        "705	169	1	4	1	NULL	2	1",
        "706	169	4	12	1	NULL	2	2",
        "707	169	3	1	1	NULL	2	3",
        "708	169	2	6	1	NULL	2	4",
        "709	170	1	4	1	NULL	2	1",
        "710	170	4	12	1	NULL	2	2",
        "711	170	3	1	1	NULL	2	3",
        "712	170	2	6	1	NULL	2	4",
        "713	171	1	4	1	NULL	2	1",
        "714	171	4	12	1	NULL	2	2",
        "715	171	3	1	1	NULL	2	3",
        "716	171	2	6	1	NULL	2	4",
        "717	172	1	4	1	NULL	1	1",
        "718	172	4	12	1	NULL	1	2",
        "719	172	3	1	1	NULL	1	3",
        "720	172	2	6	1	NULL	1	4",
        "721	173	1	4	1	NULL	1	1",
        "722	173	4	12	1	NULL	1	2",
        "723	173	3	1	1	NULL	1	3",
        "724	173	2	6	1	NULL	1	4",
        "725	174	1	4	1	NULL	1	1",
        "726	174	4	12	1	NULL	1	2",
        "727	174	3	1	1	NULL	1	3",
        "728	174	2	6	1	NULL	1	4",
        "729	175	1	4	1	NULL	2	1",
        "730	175	4	12	1	NULL	2	2",
        "731	175	3	1	1	NULL	2	3",
        "732	175	2	6	1	NULL	2	4",
        "733	176	1	4	1	NULL	2	1",
        "734	176	4	12	1	NULL	2	2",
        "735	176	3	1	1	NULL	2	3",
        "736	176	2	6	1	NULL	NULL	4",
        "737	177	1	4	1	NULL	2	1",
        "738	177	4	12	1	NULL	2	2",
        "739	177	3	1	1	NULL	2	3",
        "740	177	2	6	1	NULL	2	4",
        "741	178	1	4	1	NULL	2	1",
        "742	178	4	12	1	NULL	2	2",
        "743	178	3	1	1	NULL	2	3",
        "744	178	2	6	1	NULL	2	4",
        "745	179	1	4	1	NULL	2	1",
        "746	179	4	12	1	NULL	2	2",
        "747	179	3	1	1	NULL	2	3",
        "748	179	2	6	1	NULL	2	4",
        "749	180	1	4	1	NULL	1	1",
        "750	180	4	12	1	NULL	1	2",
        "751	180	3	1	1	NULL	1	3",
        "752	180	2	6	1	NULL	1	4",
        "753	181	1	4	1	NULL	1	1",
        "754	181	4	12	1	NULL	1	2",
        "755	181	3	1	1	NULL	1	3",
        "756	181	2	6	1	NULL	1	4",
        "757	182	1	4	1	NULL	1	1",
        "758	182	4	12	1	NULL	1	2",
        "759	182	3	1	1	NULL	1	3",
        "760	182	2	6	1	NULL	NULL	4",
        "761	183	1	4	1	NULL	1	1",
        "762	183	4	12	1	NULL	1	2",
        "763	183	3	1	1	NULL	1	3",
        "764	183	2	6	1	NULL	NULL	4",
        "765	184	1	4	1	NULL	2	1",
        "766	184	4	12	1	NULL	2	2",
        "767	184	3	1	1	NULL	2	3",
        "768	184	2	6	1	NULL	2	4",
        "769	185	1	4	1	NULL	1	1",
        "770	185	4	12	1	NULL	1	2",
        "771	185	3	1	1	NULL	1	3",
        "772	185	2	6	1	NULL	1	4",
        "773	186	20	14	1	NULL	1	1",
        "774	186	20	14	1	NULL	1	2",
        "775	186	20	14	1	NULL	1	3",
        "776	186	20	14	1	NULL	1	4",
        "777	186	20	14	1	NULL	1	5",
        "778	186	20	14	1	NULL	1	6",
        "779	186	20	14	1	NULL	1	7",
        "780	186	20	14	1	NULL	1	8",
        "781	187	20	14	1	NULL	2	1",
        "782	187	20	14	1	NULL	2	2",
        "783	187	20	14	1	NULL	2	3",
        "784	187	20	14	1	NULL	2	4",
        "785	187	20	14	1	NULL	2	5",
        "786	187	20	14	1	NULL	2	6",
        "787	187	20	14	1	NULL	2	7",
        "788	187	20	14	1	NULL	2	8",
        "789	188	1	4	1	NULL	2	1",
        "790	188	4	12	1	NULL	2	2",
        "791	188	3	1	1	NULL	2	3",
        "792	188	2	6	1	NULL	2	4",
        "793	189	1	4	1	NULL	2	1",
        "794	189	4	12	1	NULL	2	2",
        "795	189	3	1	1	NULL	2	3",
        "796	189	2	6	1	NULL	2	4",
        "797	190	1	4	1	NULL	1	1",
        "798	190	4	12	1	NULL	1	2",
        "799	190	3	1	1	NULL	1	3",
        "800	190	2	6	1	NULL	1	4",
        "801	191	1	4	1	NULL	1	1",
        "802	191	4	12	1	NULL	1	2",
        "803	191	3	1	1	NULL	1	3",
        "804	191	2	6	1	NULL	1	4",
        "805	193	1	4	1	NULL	2	1",
        "806	193	4	12	1	NULL	2	2",
        "807	193	3	1	1	NULL	2	3",
        "808	193	2	6	1	NULL	2	4",
        "809	194	1	4	1	NULL	2	1",
        "810	194	4	12	1	NULL	2	2",
        "811	194	3	1	1	NULL	2	3",
        "812	194	2	6	1	NULL	2	4",
        "813	195	20	14	1	NULL	1	1",
        "814	195	20	14	1	NULL	2	2",
        "815	195	20	14	1	NULL	2	3",
        "816	195	20	14	1	NULL	1	4",
        "817	195	20	14	1	NULL	2	5",
        "818	195	20	14	1	NULL	2	6",
        "819	195	20	14	1	NULL	1	7",
        "820	195	20	14	1	NULL	1	8",
        "821	196	1	4	1	NULL	3	1",
        "822	196	4	12	1	NULL	3	2",
        "823	196	3	1	1	NULL	3	3",
        "824	196	2	6	1	NULL	3	4",
        "825	196	32	17	1	NULL	3	5",
        "826	197	20	14	1	NULL	3	1",
        "827	197	20	14	1	NULL	3	2",
        "828	197	20	14	1	NULL	3	3",
        "829	197	20	14	1	NULL	3	4",
        "830	197	20	14	1	NULL	3	5",
        "831	197	20	14	1	NULL	3	6",
        "832	197	20	14	1	NULL	3	7",
        "833	197	20	14	1	NULL	3	8",
        "834	199	1	4	1	NULL	1	1",
        "835	199	4	12	1	NULL	1	2",
        "836	199	3	1	1	NULL	1	3",
        "837	199	2	6	1	NULL	1	4",
        "838	200	1	4	1	NULL	1	1",
        "839	200	4	12	1	NULL	1	2",
        "840	200	3	1	1	NULL	1	3",
        "841	200	2	6	1	NULL	1	4",
        "842	207	1	4	1	NULL	2	1",
        "843	207	4	12	1	NULL	2	2",
        "844	207	3	1	1	NULL	2	3",
        "845	207	2	6	1	NULL	2	4",
        "846	209	1	4	1	NULL	1	1",
        "847	209	4	12	1	NULL	1	2",
        "848	209	3	1	1	NULL	1	3",
        "849	209	2	6	1	NULL	1	4",
        "850	211	1	4	1	NULL	NULL	1",
        "851	211	4	12	1	NULL	NULL	2",
        "852	211	3	1	1	NULL	NULL	3",
        "853	211	2	6	1	NULL	NULL	4",
        "854	186	19	14	1	NULL	1	1",
        "855	186	19	14	1	NULL	1	2",
        "856	186	19	14	1	NULL	1	3",
        "857	186	19	14	1	NULL	1	4",
        "858	186	19	14	1	NULL	1	5",
        "859	186	19	14	1	NULL	1	6",
        "860	186	19	14	1	NULL	1	7",
        "861	186	19	14	1	NULL	1	8",
        "862	187	19	14	1	NULL	2	1",
        "863	187	19	14	1	NULL	2	2",
        "864	187	19	14	1	NULL	2	3",
        "865	187	19	14	1	NULL	2	4",
        "866	187	19	14	1	NULL	2	5",
        "867	187	19	14	1	NULL	2	6",
        "868	187	19	14	1	NULL	2	7",
        "869	187	19	14	1	NULL	2	8",
        "870	195	19	14	1	NULL	1	1",
        "871	195	19	14	1	NULL	2	2",
        "872	195	19	14	1	NULL	2	3",
        "873	195	19	14	1	NULL	1	4",
        "874	195	19	14	1	NULL	2	5",
        "875	195	19	14	1	NULL	2	6",
        "876	195	19	14	1	NULL	1	7",
        "877	195	19	14	1	NULL	1	8",
        "878	197	19	14	1	NULL	3	1",
        "879	197	19	14	1	NULL	3	2",
        "880	197	19	14	1	NULL	3	3",
        "881	197	19	14	1	NULL	3	4",
        "882	197	19	14	1	NULL	3	5",
        "883	197	19	14	1	NULL	3	6",
        "884	197	19	14	1	NULL	3	7",
        "885	197	19	14	1	NULL	3	8",
        "886	214	1	4	1	NULL	4	1",
        "887	214	4	12	1	NULL	4	2",
        "888	214	3	1	1	NULL	4	3",
        "889	214	2	6	1	NULL	4	4",
        "890	214	33	10	1	NULL	4	5",
        "891	214	35	10	1	NULL	4	6",
        "892	214	32	17	1	NULL	4	7",
        "893	215	20	14	1	NULL	NULL	1",
        "894	215	20	14	1	NULL	NULL	2",
        "895	215	20	14	1	NULL	NULL	3",
        "896	215	20	14	1	NULL	NULL	4",
        "897	215	20	14	1	NULL	NULL	5",
        "898	215	20	14	1	NULL	NULL	6",
        "899	215	20	14	1	NULL	NULL	7",
        "900	215	20	14	1	NULL	NULL	8",
        "901	216	1	4	1	NULL	NULL	1",
        "902	216	4	12	1	NULL	NULL	2",
        "903	216	3	1	1	NULL	NULL	3",
        "904	216	2	6	1	NULL	NULL	4",
        "905	216	33	10	1	NULL	NULL	5",
        "906	216	35	10	1	NULL	NULL	6",
        "907	216	32	17	1	NULL	NULL	7",
        "908	217	1	4	1	NULL	NULL	1",
        "909	217	4	12	1	NULL	NULL	2",
        "910	217	3	1	1	NULL	NULL	3",
        "911	217	2	6	1	NULL	NULL	4",
        "912	225	20	14	1	NULL	NULL	1",
        "913	225	20	14	1	NULL	NULL	2",
        "914	225	20	14	1	NULL	NULL	3",
        "915	225	20	14	1	NULL	NULL	4",
        "916	225	20	14	1	NULL	NULL	5",
        "917	225	20	14	1	NULL	NULL	6",
        "918	225	20	14	1	NULL	NULL	7",
        "919	225	20	14	1	NULL	NULL	8",
        "920	226	1	4	1	NULL	5	1",
        "921	226	4	12	1	NULL	5	2",
        "922	226	3	1	1	NULL	5	3",
        "923	226	2	6	1	NULL	5	4",
        "924	226	33	10	1	NULL	5	5",
        "925	226	35	10	1	NULL	5	6",
        "926	226	32	17	1	NULL	5	7",
        "927	227	1	4	1	NULL	5	1",
        "928	227	4	12	1	NULL	5	2",
        "929	227	3	1	1	NULL	5	3",
        "930	227	2	6	1	NULL	5	4",
        "931	228	20	14	1	NULL	5	1",
        "932	228	20	14	1	NULL	5	2",
        "933	228	20	14	1	NULL	NULL	3",
        "934	228	20	14	1	NULL	NULL	4",
        "935	228	20	14	1	NULL	NULL	5",
        "936	228	20	14	1	NULL	NULL	6",
        "937	228	20	14	1	NULL	NULL	7",
        "938	228	20	14	1	NULL	NULL	8",
        "939	229	1	4	1	NULL	NULL	1",
        "940	229	4	12	1	NULL	NULL	2",
        "941	229	3	1	1	NULL	NULL	3",
        "942	229	2	6	1	NULL	NULL	4",
        "943	230	1	4	1	NULL	NULL	1",
        "944	230	4	12	1	NULL	NULL	2",
        "945	230	3	1	1	NULL	NULL	3",
        "946	230	2	6	1	NULL	NULL	4",
        "947	231	1	4	1	NULL	NULL	1",
        "948	231	4	12	1	NULL	NULL	2",
        "949	231	3	1	1	NULL	NULL	3",
        "950	231	2	6	1	NULL	NULL	4",
        "951	232	1	4	1	NULL	NULL	1",
        "952	232	4	12	1	NULL	NULL	2",
        "953	232	3	1	1	NULL	NULL	3",
        "954	232	2	6	1	NULL	NULL	4",
        "955	233	1	4	1	NULL	NULL	1",
        "956	233	4	12	1	NULL	NULL	2",
        "957	233	3	1	1	NULL	NULL	3",
        "958	233	2	6	1	NULL	NULL	4",
        "959	234	1	4	1	NULL	NULL	1",
        "960	234	4	12	1	NULL	NULL	2",
        "961	234	3	1	1	NULL	NULL	3",
        "962	234	2	6	1	NULL	NULL	4",
        "963	235	1	4	1	NULL	NULL	1",
        "964	235	4	12	1	NULL	NULL	2",
        "965	235	3	1	1	NULL	NULL	3",
        "966	235	2	6	1	NULL	NULL	4",
        "967	236	1	4	1	NULL	NULL	1",
        "968	236	4	12	1	NULL	NULL	2",
        "969	236	3	1	1	NULL	NULL	3",
        "970	236	2	6	1	NULL	NULL	4",
        "971	237	1	4	1	NULL	NULL	1",
        "972	237	4	12	1	NULL	NULL	2",
        "973	237	3	1	1	NULL	NULL	3",
        "974	237	2	6	1	NULL	NULL	4",
        "975	238	1	4	1	NULL	NULL	1",
        "976	238	4	12	1	NULL	NULL	2",
        "977	238	3	1	1	NULL	NULL	3",
        "978	238	2	6	1	NULL	NULL	4",
        "979	239	1	4	1	NULL	NULL	1",
        "980	239	4	12	1	NULL	NULL	2",
        "981	239	3	1	1	NULL	NULL	3",
        "982	239	2	6	1	NULL	NULL	4",
        "983	240	1	4	1	NULL	NULL	1",
        "984	240	4	12	1	NULL	NULL	2",
        "985	240	3	1	1	NULL	NULL	3",
        "986	240	2	6	1	NULL	NULL	4",
        "987	241	1	4	1	NULL	1	1",
        "988	241	4	12	1	NULL	1	2",
        "989	241	3	1	1	NULL	1	3",
        "990	241	2	6	1	NULL	1	4",
        "991	227	33	10	1	NULL	NULL	5",
        "992	227	35	10	1	NULL	NULL	6",
        "993	227	32	17	1	NULL	NULL	7",
        "994	243	1	4	1	NULL	1	1",
        "995	243	4	12	1	NULL	1	2",
        "996	243	3	1	1	NULL	1	3",
        "997	243	2	6	1	NULL	1	4",
        "998	243	33	10	1	NULL	1	5",
        "999	243	35	10	1	NULL	1	6",
        "1000	244	1	4	1	NULL	1	1",
        "1001	244	4	12	1	NULL	1	2",
        "1002	244	3	1	1	NULL	1	3",
        "1003	244	2	6	1	NULL	1	4",
        "1004	244	33	10	1	NULL	1	5",
        "1005	244	35	10	1	NULL	1	6",
        "1006	248	1	4	1	NULL	NULL	1",
        "1007	248	4	12	1	NULL	NULL	2",
        "1008	248	3	1	1	NULL	NULL	3",
        "1009	248	2	6	1	NULL	NULL	4",
        "1010	249	1	4	1	NULL	NULL	1",
        "1011	249	4	12	1	NULL	NULL	2",
        "1012	249	3	1	1	NULL	NULL	3",
        "1013	249	2	6	1	NULL	NULL	4",
        "1014	250	1	4	1	NULL	NULL	1",
        "1015	250	4	12	1	NULL	NULL	2",
        "1016	250	3	1	1	NULL	NULL	3",
        "1017	250	2	6	1	NULL	NULL	4",
        "1018	251	1	4	1	NULL	NULL	1",
        "1019	251	4	12	1	NULL	NULL	2",
        "1020	251	3	1	1	NULL	NULL	3",
        "1021	251	2	6	1	NULL	NULL	4",
        "1022	252	1	4	1	NULL	NULL	1",
        "1023	252	4	12	1	NULL	NULL	2",
        "1024	252	3	1	1	NULL	NULL	3",
        "1025	252	2	6	1	NULL	NULL	4",
        "1026	253	1	4	1	NULL	NULL	1",
        "1027	253	4	12	1	NULL	NULL	2",
        "1028	253	3	1	1	NULL	NULL	3",
        "1029	253	2	6	1	NULL	NULL	4",
        "1030	254	1	4	1	NULL	NULL	1",
        "1031	254	4	12	1	NULL	NULL	2",
        "1032	254	3	1	1	NULL	NULL	3",
        "1033	254	2	6	1	NULL	NULL	4",
        "1034	255	20	14	1	NULL	NULL	1",
        "1035	255	20	14	1	NULL	NULL	2",
        "1036	255	20	14	1	NULL	NULL	3",
        "1037	255	20	14	1	NULL	NULL	4",
        "1038	255	20	14	1	NULL	NULL	5",
        "1039	255	20	14	1	NULL	NULL	6",
        "1040	255	20	14	1	NULL	NULL	7",
        "1041	255	20	14	1	NULL	NULL	8",
        "1042	257	1	4	1	NULL	1	1",
        "1043	257	4	12	1	NULL	1	2",
        "1044	257	3	1	1	NULL	1	3",
        "1045	257	2	6	1	NULL	1	4",
        "1046	258	1	4	1	NULL	1	1",
        "1047	258	4	12	1	NULL	1	2",
        "1048	258	3	1	1	NULL	1	3",
        "1049	258	2	6	1	NULL	1	4",
        "1050	259	1	4	1	NULL	2	1",
        "1051	259	4	12	1	NULL	2	2",
        "1052	259	3	1	1	NULL	2	3",
        "1053	259	2	6	1	NULL	2	4",
        "1054	262	1	4	1	NULL	NULL	1",
        "1055	262	4	12	1	NULL	NULL	2",
        "1056	262	3	1	1	NULL	NULL	3",
        "1057	262	2	6	1	NULL	NULL	4",
        "1058	262	33	10	1	NULL	NULL	5",
        "1059	262	35	10	1	NULL	NULL	6",
        "1060	262	32	17	1	NULL	NULL	7",
        "1061	265	0	4	1	NULL	NULL	1",
        "1062	265	0	1	4	NULL	NULL	2",
        "1063	265	0	1	3	NULL	NULL	3",
        "1064	265	0	1	2	NULL	NULL	4",
        "1065	265	0	1	0	NULL	NULL	5",
        "1066	265	0	0	0	NULL	NULL	6",
        "1067	265	0	0	0	NULL	NULL	7",
        "1068	265	0	0	0	NULL	NULL	8",
        "1069	265	0	0	0	NULL	NULL	9",
        "1070	265	0	0	0	NULL	NULL	10",
        "1071	268	1	4	1	NULL	6	1",
        "1072	268	4	12	1	NULL	6	2",
        "1073	268	3	1	1	NULL	6	3",
        "1074	268	2	6	1	NULL	6	4",
        "1075	269	1	4	1	NULL	6	1",
        "1076	269	4	12	1	NULL	6	2",
        "1077	269	3	1	1	NULL	6	3",
        "1078	269	2	6	1	NULL	6	4",
        "1079	270	1	4	1	NULL	NULL	1",
        "1080	270	4	12	1	NULL	NULL	2",
        "1081	270	3	1	1	NULL	NULL	3",
        "1082	270	2	6	1	NULL	NULL	4",
        "1083	270	33	10	1	NULL	NULL	5",
        "1084	270	35	10	1	NULL	NULL	6",
        "1085	271	1	4	1	NULL	NULL	1",
        "1086	271	4	12	1	NULL	NULL	2",
        "1087	271	3	1	1	NULL	NULL	3",
        "1088	271	2	6	1	NULL	NULL	4"};

    static void load() {
        for (String str : sensorData) {
            Sensor s = new Sensor(str);
            String key = s.key();
            sensors.put(key, s);
        }

    }
     */
}
