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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chris
 */
public class Device {
    
    static String insertSQL = "insert into Device(facilityInfo,DeviceCommonName,modbusAddr,DeviceSerialNumber,reporting,NumberOfAttachedSensors) values(?,?,?,?,?,?)";
    PreparedStatement insertStatement = null;
    
    static String findByIdSQL = "select DEviceId from DEvice where facilityInfo=? and modbusAddr=? and DeviceSerialNumber=?";
    PreparedStatement findByIdStatement = null;
    
    
    static int inserts = 0;
    
    static final Logger logger = Logger.getLogger("Device");

    static HashMap<String, Device> devices = new HashMap<String, Device>();
    static HashMap<Integer, Device> devicesById = new HashMap<Integer, Device>();

    int deviceID;
    String facilityInfo;
    String name;
    int networkId;
    String sensorFacilityInfo;
    int modbusAddr;
    int serialNumber;
    String type;
    String version;
    boolean reporting;
    int locationId;
    int numSensors;
    ////int countryId;
    int clientId;
    //int siteId;
    //int locationId;

    static {
         logger.setLevel(Level.WARNING);
        logger.addHandler(TegnonLoad.logHandler);
    }

    public Device (ResultSet rs) throws SQLException {
        int i = 1;
        deviceID = rs.getInt(i++);
        facilityInfo = rs.getString(i++);
        name = rs.getString(i++);
         networkId = rs.getInt(i++);
        sensorFacilityInfo = rs.getString(i++);
        modbusAddr = rs.getInt(i++);
        serialNumber = rs.getInt(i++);
        type = rs.getString(i++);
        version = rs.getString(i++);
        reporting = rs.getBoolean(i++);
        locationId = rs.getInt(i++);
        numSensors = rs.getInt(i++);
    
       
        devices.put(this.key(), this);
        devicesById.put(deviceID,this);
        
    }
    
    public Device(PiLine pi) throws SQLException {
        facilityInfo = pi.facilityInfo;
        name = pi.deviceCommonName;
        //networkId = pi.
        // sensorFacilityInfo
        modbusAddr = pi.modbusAddr;
        serialNumber = pi.deviceSerialNumber;
        //type = pi.
        //version = pi.
        reporting = true;
        //locationId = pi.
        numSensors = pi.numberOfAttachedSensors;
        
        insert();
        
         devices.put(this.key(), this);
        devicesById.put(deviceID,this);
        
    }
    
    public Device(String params) {

        String[] strs = params.split("[\t ]+");

        //System.out.println("Dev NoParams:" + strs.length + " " + params);
//DeviceID	FacilityInfo	DeviceCommonName	NetworkID	SensorFacilityInfo	ModbusAddr	
//DeviceSerialNumber	DeviceType	FirmwareVersion	Reporting	LocationID	NumberOfAttachedSensors
        int i = 0;
        deviceID = Integer.parseInt(strs[i++]);
        facilityInfo = strs[i++];
        name = strs[i++];
        try {
            networkId = Integer.parseInt(strs[i++]);
        } catch (Exception exc) {
            networkId = -1;
        }
        i++; // sensFacInfo
        modbusAddr = Integer.parseInt(strs[i++]);
        serialNumber = Integer.parseInt(strs[i++]);
        type = strs[i++];
        version = strs[i++];
        reporting = Boolean.getBoolean(strs[i++]);
        locationId = Integer.parseInt(strs[i++]);
        try {
            numSensors = Integer.parseInt(strs[i++]);
        } catch (Exception exc) {
            numSensors = 0;
        }
    }
    
    static public void zeroStat() {
        inserts=0;
    }

    String key() {
        return facilityInfo + "|" + modbusAddr + "|" + serialNumber;
    }

    static public Device find(String facilityInfo, int modbusAddr, int serialNumber) {

        return devices.get(facilityInfo + "|" + modbusAddr + "|" + serialNumber);
    }

    static public Device findById(Integer id) {

        return devicesById.get(id);
    }

    static void dump() {
        System.out.println("************************************************************ Devices Dump");
        for (Device d : devices.values()) {
            System.out.println(" " + d.deviceID + " " + d.facilityInfo + "\t" + d.name + "\t" + d.type);
        }
    }

    static void loadSQL(Connection conn) throws SQLException{
        Statement stmt  = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select * from DEvice");
        while(rs.next()) {
            new Device (rs);
        }
        
    }
    
    void insert() throws SQLException {
        int i = 1;
        if (findByIdStatement==null) {
            findByIdStatement = TegnonLoad.conn.prepareStatement(findByIdSQL);
            insertStatement =  TegnonLoad.conn.prepareStatement(insertSQL);
        }
        //facilityInfo,DeviceCommonName,modbusAddr,DeviceSerialNumber,reporting,NumberOfAttachedSensors
        insertStatement.setString(i++,facilityInfo);
        insertStatement.setString(i++,name);
        insertStatement.setInt(i++,modbusAddr);
        insertStatement.setInt(i++,serialNumber);
        insertStatement.setBoolean(i++,reporting);
        insertStatement.setInt(i++,numSensors);
        insertStatement.executeUpdate();

        findByIdStatement.setString(1,facilityInfo);
        findByIdStatement.setInt(2,modbusAddr);
        findByIdStatement.setInt(3,serialNumber);
        ResultSet rs = findByIdStatement.executeQuery();
        rs.next();
        this.deviceID = rs.getInt(1);
        logger.fine("Device.Insert " + deviceID + " " + facilityInfo + "\t" + name + "\t" + type);
        inserts++;
    }
//DeviceID	FacilityInfo	DeviceCommonName	NetworkID	SensorFacilityInfo	ModbusAddr	DeviceSerialNumber	DeviceType	FirmwareVersion	Reporting	LocationID	NumberOfAttachedSensors
    static final String[] deviceData = {
        "157	za.Consol.Wadeville.CartonFloor	W3_1_Pilot_Air_HP	NULL	NULL	5	31	NULL	NULL	NULL	9	NULL",
        "158	za.Consol.Wadeville.CartonFloor	W3_1_OP_Air_HP	NULL	NULL	10	53	NULL	NULL	NULL	9	NULL",
        "159	za.Consol.Wadeville.CartonFloor	W3_2_Pilot_Air_HP	NULL	NULL	15	56	NULL	NULL	NULL	9	NULL",
        "160	za.Consol.Wadeville.CartonFloor	Cold_End_01_HP	NULL	NULL	20	59	NULL	NULL	NULL	9	NULL",
        "161	za.Consol.Wadeville.CartonFloor	W3_2_LP	NULL	NULL	25	58	NULL	NULL	NULL	9	NULL",
        "162	za.Consol.Wadeville.CartonFloor	W3_1_LP	NULL	NULL	30	57	NULL	NULL	NULL	9	NULL",
        "163	za.Consol.Wadeville.CartonFloor	LehrSpray_HP	NULL	NULL	35	50	NULL	NULL	NULL	9	NULL",
        "164	za.Consol.Wadeville.CartonFloor	W4_0_Pilot_Air_HP	NULL	NULL	40	76	NULL	NULL	NULL	9	NULL",
        "165	za.Consol.Wadeville.CartonFloor	Cold_End_02_HP	NULL	NULL	45	54	NULL	NULL	NULL	9	NULL",
        "166	za.Consol.Wadeville.CartonFloor	W4_0_LP	NULL	NULL	50	33	NULL	NULL	NULL	9	NULL",
        "167	za.Consol.Wadeville.CartonFloor	W4_1_HP	NULL	NULL	55	61	NULL	NULL	NULL	9	NULL",
        "168	za.Consol.Wadeville.CartonFloor	W4_2_HP	NULL	NULL	60	44	NULL	NULL	NULL	9	NULL",
        "169	za.Consol.Wadeville.CartonFloor	W4_1_LP	NULL	NULL	65	60	NULL	NULL	NULL	9	NULL",
        "170	za.Consol.Wadeville.CartonFloor	W4_2_LP	NULL	NULL	70	63	NULL	NULL	NULL	9	NULL",
        "171	za.Consol.Wadeville.CartonFloor	W4_3_LP	NULL	NULL	75	74	NULL	NULL	NULL	9	NULL",
        "172	za.Consol.Wadeville.CartonFloor	W4_3_Pilot_Air_HP	NULL	NULL	80	49	NULL	NULL	NULL	9	NULL",
        "173	za.Consol.Wadeville.CartonFloor	Guide_Air_HP	NULL	NULL	85	55	NULL	NULL	NULL	9	NULL",
        "174	za.Consol.Wadeville.CartonFloor	W4_4_Pilot_Air_HP	NULL	NULL	90	32	NULL	NULL	NULL	9	NULL",
        "175	za.Consol.Wadeville.CartonFloor	W4_4_LP	NULL	NULL	95	62	NULL	NULL	NULL	9	NULL",
        "176	za.Consol.Bellville.CompressorRoom	LP6	NULL	NULL	5	48	NULL	NULL	NULL	10	NULL",
        "177	za.Consol.Bellville.CompressorRoom	LP1	NULL	NULL	10	67	NULL	NULL	NULL	10	NULL",
        "178	za.Consol.Bellville.CompressorRoom	LP3	NULL	NULL	15	72	NULL	NULL	NULL	10	NULL",
        "179	za.Consol.Bellville.CompressorRoom	LP5	NULL	NULL	20	64	NULL	NULL	NULL	10	NULL",
        "180	za.Consol.Bellville.CompressorRoom	HP1	NULL	NULL	25	66	NULL	NULL	NULL	10	NULL",
        "181	za.Consol.Bellville.CompressorRoom	HP4	NULL	NULL	30	65	NULL	NULL	NULL	10	NULL",
        "182	za.Consol.Bellville.CompressorRoom	HP3	NULL	NULL	35	69	NULL	NULL	NULL	10	NULL",
        "183	za.Consol.Bellville.CompressorRoom	HP5	NULL	NULL	40	51	NULL	NULL	NULL	10	NULL",
        "184	za.Consol.Bellville.CompressorRoom	LP2	NULL	NULL	50	71	NULL	NULL	NULL	10	NULL",
        "185	za.Consol.Bellville.CompressorRoom	HP2	NULL	NULL	55	73	NULL	NULL	NULL	10	NULL",
        "186	za.Consol.Bellville.CompressorRoom	HPEnergy	NULL	NULL	60	100	NULL	NULL	NULL	10	NULL",
        "187	za.Consol.Bellville.CompressorRoom	LPEnergy	NULL	NULL	65	150	NULL	NULL	NULL	10	NULL",
        "188	za.Consol.Wadeville.CompressorRoom	Centac02	NULL	NULL	5	30	NULL	NULL	NULL	11	NULL",
        "189	za.Consol.Wadeville.CompressorRoom	Centac01	NULL	NULL	10	47	NULL	NULL	NULL	11	NULL",
        "190	za.Consol.Wadeville.CompressorRoom	Centac04	NULL	NULL	15	45	NULL	NULL	NULL	11	NULL",
        "191	za.Consol.Wadeville.CompressorRoom	Centac06	NULL	NULL	20	36	NULL	NULL	NULL	11	NULL",
        "192	za.Consol.Wadeville.CompressorRoom	Centac05	NULL	NULL	25	0	NULL	NULL	NULL	11	NULL",
        "193	za.Consol.Wadeville.CompressorRoom	Centac03	NULL	NULL	30	35	NULL	NULL	NULL	11	NULL",
        "194	za.Consol.Wadeville.CompressorRoom	Joy	NULL	NULL	35	75	NULL	NULL	NULL	11	NULL",
        "195	za.Consol.Wadeville.CompressorRoom	Energy	NULL	NULL	40	100	NULL	NULL	NULL	11	NULL",
        "196	za.Netcare.Alberlito.CompressorRoom	LP6	NULL	NULL	5	94	NULL	NULL	NULL	12	NULL",
        "197	za.Netcare.Alberlito.CompressorRoom	Energy	NULL	NULL	10	101	NULL	NULL	NULL	12	NULL",
        "198	za.Consol.Bellville.CompressorRoom	HP4	NULL	NULL	30	0	NULL	NULL	NULL	10	NULL",
        "199	za.Consol.Bellville.CompressorRoom	HP5_1	NULL	NULL	40	70	NULL	NULL	NULL	10	NULL",
        "200	za.Consol.Bellville.CompressorRoom	HP5_2	NULL	NULL	45	68	NULL	NULL	NULL	10	NULL",
        "201	za.Netcare.Alberlito.CompressorRoom	Energy	NULL	NULL	10	0	NULL	NULL	NULL	12	NULL",
        "202	za.Consol.Wadeville.CompressorRoom	Centac01	NULL	NULL	10	0	NULL	NULL	NULL	11	NULL",
        "203	za.Consol.Wadeville.CompressorRoom	Centac04	NULL	NULL	15	0	NULL	NULL	NULL	11	NULL",
        "204	za.Consol.Wadeville.CompressorRoom	Centac06	NULL	NULL	20	0	NULL	NULL	NULL	11	NULL",
        "205	za.Consol.Wadeville.CompressorRoom	Centac03	NULL	NULL	30	0	NULL	NULL	NULL	11	NULL",
        "206	za.Consol.Wadeville.CompressorRoom	Joy	NULL	NULL	35	0	NULL	NULL	NULL	11	NULL",
        "207	za.Consol.Wadeville.CompressorRoom	Joy	NULL	NULL	35	46	NULL	NULL	NULL	11	NULL",
        "208	za.Consol.Wadeville.CompressorRoom	Centac02	NULL	NULL	5	0	NULL	NULL	NULL	11	NULL",
        "209	za.Consol.Bellville.CompressorRoom	HP5_1	NULL	NULL	40	51	NULL	NULL	NULL	10	NULL",
        "210	za.tegnontest.TestSite.CompressorRoom	PowerMeterBank1	NULL	NULL	3	0	NULL	NULL	NULL	10	NULL",
        "211	za.tegnontest.TestSite.LPCompressorRoom	CompressorA	NULL	NULL	20	64	NULL	NULL	NULL	13	NULL",
        "212	za.tegnontest.TestSite.LPCompressorRoom	Centac2	NULL	NULL	30	0	NULL	NULL	NULL	13	NULL",
        "213	za.Consol.Bellville.CompressorRoom	HP2	NULL	NULL	55	0	NULL	NULL	NULL	10	NULL",
        "214	za.TongaatHuletts.Kliprivier.TegnonDryer	MainSupply	NULL	NULL	5	85	NULL	NULL	NULL	14	NULL",
        "215	za.Consol.Bellville.CompressorRoom	LP6	NULL	NULL	5	101	NULL	NULL	NULL	10	NULL",
        "216	za.Consol.Bellville.CompressorRoom	LP1	NULL	NULL	10	84	NULL	NULL	NULL	10	NULL",
        "217	za.Consol.Bellville.CompressorRoom	LP3	NULL	NULL	15	90	NULL	NULL	NULL	10	NULL",
        "218	za.Consol.Bellville.CompressorRoom	LP5	NULL	NULL	20	0	NULL	NULL	NULL	10	NULL",
        "219	za.Consol.Bellville.CompressorRoom	HP1	NULL	NULL	25	0	NULL	NULL	NULL	10	NULL",
        "220	za.Consol.Bellville.CompressorRoom	HP3	NULL	NULL	35	0	NULL	NULL	NULL	10	NULL",
        "221	za.Consol.Bellville.CompressorRoom	HP5	NULL	NULL	40	0	NULL	NULL	NULL	10	NULL",
        "222	za.Consol.Bellville.CompressorRoom	LP2	NULL	NULL	50	0	NULL	NULL	NULL	10	NULL",
        "223	za.Consol.Bellville.CompressorRoom	HPEnergy	NULL	NULL	60	0	NULL	NULL	NULL	10	NULL",
        "224	za.Consol.Bellville.CompressorRoom	LPEnergy	NULL	NULL	65	0	NULL	NULL	NULL	10	NULL",
        "225	za.Pioneer.Wadeville.CompressorRoom	Compressors	NULL	NULL	5	101	NULL	NULL	NULL	17	NULL",
        "226	za.Pioneer.Wadeville.DryingRoom	Pronutro	NULL	NULL	10	84	NULL	NULL	NULL	15	NULL",
        "227	za.Pioneer.Wadeville.DryingRoom	HotPorridges	NULL	NULL	15	90	NULL	NULL	NULL	15	NULL",
        "228	za.Pioneer.Wadeville.CompressorRoom	Compressors	NULL	NULL	5	102	NULL	NULL	NULL	17	NULL",
        "229	za.Consol.Bellville.CompressorRoom	LP6	NULL	NULL	5	31	NULL	NULL	NULL	10	NULL",
        "230	za.Consol.Bellville.CompressorRoom	LP1	NULL	NULL	10	53	NULL	NULL	NULL	10	NULL",
        "231	za.Consol.Bellville.CompressorRoom	LP3	NULL	NULL	15	56	NULL	NULL	NULL	10	NULL",
        "232	za.Consol.Bellville.CompressorRoom	LP5	NULL	NULL	20	59	NULL	NULL	NULL	10	NULL",
        "233	za.Consol.Bellville.CompressorRoom	HP1	NULL	NULL	25	58	NULL	NULL	NULL	10	NULL",
        "234	za.Consol.Bellville.CompressorRoom	HP4	NULL	NULL	30	57	NULL	NULL	NULL	10	NULL",
        "235	za.Consol.Bellville.CompressorRoom	HP3	NULL	NULL	35	50	NULL	NULL	NULL	10	NULL",
        "236	za.Consol.Bellville.CompressorRoom	HP5	NULL	NULL	40	76	NULL	NULL	NULL	10	NULL",
        "237	za.Consol.Bellville.CompressorRoom	LP2	NULL	NULL	50	33	NULL	NULL	NULL	10	NULL",
        "238	za.Consol.Bellville.CompressorRoom	HP2	NULL	NULL	55	61	NULL	NULL	NULL	10	NULL",
        "239	za.Consol.Bellville.CompressorRoom	HPEnergy	NULL	NULL	60	44	NULL	NULL	NULL	10	NULL",
        "240	za.Consol.Bellville.CompressorRoom	LPEnergy	NULL	NULL	65	60	NULL	NULL	NULL	10	NULL",
        "241	za.Consol.Wadeville.CompressorRoom	Centac05	NULL	NULL	25	37	NULL	NULL	NULL	11	NULL",
        "242	za.Pioneer.Wadeville.DryingRoom	HotPorridges	NULL	NULL	15	0	NULL	NULL	NULL	15	NULL",
        "243	za.Consol.Wadeville.CompressorRoom	Dryer01	NULL	NULL	45	86	NULL	NULL	NULL	11	NULL",
        "244	za.Consol.Wadeville.CompressorRoom	Dryer02	NULL	NULL	50	87	NULL	NULL	NULL	11	NULL",
        "245	za.Consol.Wadeville.CompressorRoom	Dryer01	NULL	NULL	45	0	NULL	NULL	NULL	10	NULL",
        "246	za.Consol.Wadeville.CompressorRoom	Dryer02	NULL	NULL	50	0	NULL	NULL	NULL	10	NULL",
        "247	za.Consol.Bellville.CompressorRoom	LP6	NULL	NULL	5	0	NULL	NULL	NULL	10	NULL",
        "248	za.Consol.Bellville.CompressorRoom	LP6	NULL	NULL	5	30	NULL	NULL	NULL	10	NULL",
        "249	za.Consol.Bellville.CompressorRoom	LP1	NULL	NULL	10	47	NULL	NULL	NULL	10	NULL",
        "250	za.Consol.Bellville.CompressorRoom	LP3	NULL	NULL	15	45	NULL	NULL	NULL	10	NULL",
        "251	za.Consol.Bellville.CompressorRoom	LP5	NULL	NULL	20	36	NULL	NULL	NULL	10	NULL",
        "252	za.Consol.Bellville.CompressorRoom	HP1	NULL	NULL	25	37	NULL	NULL	NULL	10	NULL",
        "253	za.Consol.Bellville.CompressorRoom	HP4	NULL	NULL	30	35	NULL	NULL	NULL	10	NULL",
        "254	za.Consol.Bellville.CompressorRoom	HP3	NULL	NULL	35	75	NULL	NULL	NULL	10	NULL",
        "255	za.Consol.Bellville.CompressorRoom	HP5	NULL	NULL	40	100	NULL	NULL	NULL	10	NULL",
        "256	za.Consol.Wadeville.CartonFloor	W4_2_HP	NULL	NULL	60	0	NULL	NULL	NULL	9	NULL",
        "257	za.Consol.Bellville.CompressorRoom	HP5	NULL	NULL	40	92	NULL	NULL	NULL	10	NULL",
        "258	za.Consol.Bellville.CompressorRoom	HP3	NULL	NULL	35	82	NULL	NULL	NULL	10	NULL",
        "259	za.Consol.Bellville.CompressorRoom	LP6	NULL	NULL	5	83	NULL	NULL	NULL	10	NULL",
        "260	za.Consol.Wadeville.CartonFloor	W4_0_Pilot_Air_HP	NULL	NULL	40	0	NULL	NULL	NULL	9	NULL",
        "261	za.Pioneer.Wadeville.DryingRoom	Pronutro	NULL	NULL	10	0	NULL	NULL	NULL	15	NULL",
        "262	za.Pioneer.Wadeville.DryingRoom	HotPorridges	NULL	NULL	15	93	NULL	NULL	NULL	15	NULL",
        "263	za.Consol.Wadeville.CartonFloor	W4_4_Pilot_Air_HP	NULL	NULL	90	0	NULL	NULL	NULL	9	NULL",
        "264	za.Consol.Wadeville.CartonFloor	W4_3_LP	NULL	NULL	75	0	NULL	NULL	NULL	9	NULL",
        "265	za.Consol.Wadeville.CartonFloor	W4_3_Pilot_Air_HP	NULL	NULL	80	0	NULL	NULL	NULL	9	NULL",
        "266	za.Consol.Wadeville.CartonFloor	Cold_End_02_HP	NULL	NULL	45	0	NULL	NULL	NULL	9	NULL",
        "267	za.Consol.Wadeville.CartonFloor	W4_1_HP	NULL	NULL	55	0	NULL	NULL	NULL	9	NULL",
        "268	za.Consol.Bellville.CompressorRoom	HPRing01	NULL	NULL	70	98	NULL	NULL	NULL	10	NULL",
        "269	za.Consol.Bellville.CompressorRoom	HPRing02	NULL	NULL	75	99	NULL	NULL	NULL	10	NULL",
        "270	za.Consol.Bellville.CompressorRoom	LP2	NULL	NULL	50	87	NULL	NULL	NULL	10	NULL",
        "271	za.Consol.Wadeville.CompressorRoom	Centac03	NULL	NULL	30	86	NULL	NULL	NULL	10	NULL",
        "272	za.Netcare.Alberlito.CompressorRoom	LP6	NULL	NULL	5	0	NULL	NULL	NULL	10	NULL"
    };

    static void load() {
        for (String str : deviceData) {
            Device d = new Device(str);
            devices.put(d.key(), d);
            devicesById.put(d.deviceID, d);
        }

    }
}
