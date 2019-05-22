package scl.oms.outagemap;

import com.esri.core.geometry.Envelope;
import java.io.FileReader;
import java.io.IOException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 * Reads external JSON configuration file and sets the configuration values
 * in this singleton class instance.
 * 
 * @author jstewart
 */
public enum Config {

    INSTANCE;

    private boolean debugMode;
    private String logDirectory;
    private String environmentLabel;
    private boolean bufferOn;
    private boolean mergeOn;
    private boolean convexHullEvents;
    private double maxSupplyNodePointDist;
    private double densifyDistDegrees;
    private int densifyMaxVertices;
    private double bufferDistDegrees;
    private double pointExpandDegrees;
    private Envelope serviceEnvelope;
    private int serviceEnvelopeWkid;
    private int supplyNodeMapCapacity;
    private float supplyNodeMapLoading;
    private int customerMapCapacity;
    private float customerMapLoading;
    private String sourceDbConn; // "SYNERGEN/synergen1@//localhost:1521"
    private String sourceDbSQL;
    private int inputProjWKID;
    private int outputProjWKID;
    private String fileOutputName;
    private long fileMaxSizeBytes;
    private boolean outputToKml; // true
    private String kmlDirectory; // "./kml"
    private boolean outputToGeoDb;
    private String geoDbConn;
    private String geoDbFeatureClassTable;
    private int geoDbBatchSize;
    private boolean emailAlerts; //true,
    private String emailUser; //webteam.scl",
    private String smtpMtaHost; //mailhost.light.ci.seattle.wa.us",
    private int smtpPort; //25,
    private String emailOriginator; //webteam.scl@seattle.gov",
    private String emailRecipient; //Jeffrey.Stewart@seattle.gov"

    private Config() {
        this.resetConfig();
    }

    /*
    * Resets the configuration data.
    */
    public void resetConfig() {
        this.setDebugMode(true);
        this.setLogDirectory("");
        this.setEnvironmentLabel("unknown");
        this.setBufferOn(true);
        this.setMergeOn(true);
        this.setConvexHullEvents(true);
        this.setMaxSupplyNodePointDist(0.001799646);
        this.setDensifyDistDegrees(12.9590696961);
        this.setDensifyMaxVertices(16);
        this.setBufferDistDegrees(0.0002964440);
        this.setPointExpandDegrees(0.000035);
        this.setServiceEnvelope(new Envelope());
        this.setServiceEnvelopeWkid(ProjectTool.WGS84_WKID);
        this.getServiceEnvelope().setXMin(-122.50);
        this.getServiceEnvelope().setYMin(47.40);
        this.getServiceEnvelope().setXMax(-122.20);
        this.getServiceEnvelope().setYMax(47.80);
        this.setSupplyNodeMapCapacity(237);
        this.setSupplyNodeMapLoading(0.68f);
        this.setCustomerMapCapacity(22);
        this.setCustomerMapLoading(0.68f);
        this.setSourceDbConn("unknown");
        this.setSourceDbSQL("SELECT * FROM DUAL");
        this.setInputProjWKID(102113);
        this.setOutputProjWKID(102113);
        this.setFileOutputName("outage_map");
        this.setFileMaxSizeBytes(5242880); // 5MB
        this.setOutputToKml(false);
        this.setKmlDirectory("unknown");
        this.setOutputToGeoDb(true);
        this.setGeoDbConn("unknown");
        this.setGeoDbFeatureClassTable("DUAL");
        this.setGeoDbBatchSize(25);
        this.setEmailAlerts(true);
        this.setEmailUser("unknown");
        this.setSmtpMtaHost("unknown");
        this.setSmtpPort(25);
        this.setEmailOriginator("unknown");
        this.setEmailRecipient("unknown");
    }

    /*
    * Loads the configuration data from the external configuration file.
    */
    public void loadConfig(String applicationPath) throws IOException {

        try (FileReader reader = new FileReader(applicationPath + "config/outagemap_config_json.txt");
                JsonReader jsonReader = Json.createReader(reader)) {

            JsonObject jsonObject = (JsonObject) jsonReader.readObject();

            this.setDebugMode(jsonObject.getBoolean("debugMode"));
            this.setLogDirectory(jsonObject.getString("logDirectory"));
            this.setEnvironmentLabel(jsonObject.getString("environmentLabel"));
            this.setBufferOn(jsonObject.getBoolean("bufferOn"));
            this.setMergeOn(jsonObject.getBoolean("mergeOn"));
            this.setConvexHullEvents(jsonObject.getBoolean("convexHullEvents"));
            this.setMaxSupplyNodePointDist(jsonObject.getJsonNumber("maxSupplyNodePointDist").doubleValue());
            this.setDensifyDistDegrees(jsonObject.getJsonNumber("densifyDistDegrees").doubleValue());
            this.setDensifyDistDegrees(jsonObject.getJsonNumber("densifyMaxVertices").intValue());
            this.setBufferDistDegrees(jsonObject.getJsonNumber("bufferDistDegrees").doubleValue());
            this.setPointExpandDegrees(jsonObject.getJsonNumber("pointExpandDegrees").doubleValue());
            JsonObject jsonEvelopeObj;
            jsonEvelopeObj = jsonObject.getJsonObject("envelope");
            this.getServiceEnvelope().setXMin(jsonEvelopeObj.getJsonNumber("xmin").doubleValue());
            this.getServiceEnvelope().setYMin(jsonEvelopeObj.getJsonNumber("ymin").doubleValue());
            this.getServiceEnvelope().setXMax(jsonEvelopeObj.getJsonNumber("xmax").doubleValue());
            this.getServiceEnvelope().setYMax(jsonEvelopeObj.getJsonNumber("ymax").doubleValue());
            JsonObject jsonEvelopeWkidObj;
            jsonEvelopeWkidObj = jsonEvelopeObj.getJsonObject("spatialReference");
            this.setServiceEnvelopeWkid(jsonEvelopeWkidObj.getJsonNumber("wkid").intValue());
            this.setSupplyNodeMapCapacity(jsonObject.getJsonNumber("supplyNodeMapCapacity").intValue());
            this.setSupplyNodeMapLoading((float) jsonObject.getJsonNumber("supplyNodeMapLoading").doubleValue());
            this.setCustomerMapCapacity(jsonObject.getJsonNumber("customerMapCapacity").intValue());
            this.setCustomerMapLoading((float) jsonObject.getJsonNumber("customerMapLoading").doubleValue());
            this.setSourceDbConn(jsonObject.getString("sourceDbConn"));
            this.setSourceDbSQL(jsonObject.getString("sourceDbSQL"));
            this.setInputProjWKID(jsonObject.getJsonNumber("inputProjWKID").intValue());
            this.setOutputProjWKID(jsonObject.getJsonNumber("outputProjWKID").intValue());
            this.setFileOutputName(jsonObject.getString("fileOutputName"));
            this.setFileMaxSizeBytes(jsonObject.getJsonNumber("fileMaxSizeBytes").longValue());
            this.setOutputToKml(jsonObject.getBoolean("outputToKml"));
            this.setKmlDirectory(jsonObject.getString("kmlDirectory"));
            this.setOutputToGeoDb(jsonObject.getBoolean("outputToGeoDb"));
            this.setGeoDbConn(jsonObject.getString("geoDbConn"));
            this.setGeoDbFeatureClassTable(jsonObject.getString("geoDbFeatureClassTable"));
            this.setGeoDbBatchSize(jsonObject.getJsonNumber("geoDbBatchSize").intValue());
            this.setEmailAlerts(jsonObject.getBoolean("emailAlerts"));
            this.setEmailUser(jsonObject.getString("emailUser"));
            this.setSmtpMtaHost(jsonObject.getString("smtpMtaHost"));
            this.setSmtpPort(jsonObject.getJsonNumber("smtpPort").intValue());
            this.setEmailOriginator(jsonObject.getString("emailOriginator"));
            this.setEmailRecipient(jsonObject.getString("emailRecipient"));
        } catch (IOException | NullPointerException ex) {
            throw ex;
        }
    }

    /**
     * @return the debugMode
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * @param debugMode the debugMode to set
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * @return the environmentLabel
     */
    public String getEnvironmentLabel() {
        return environmentLabel;
    }

    /**
     * @param environmentLabel the environmentLabel to set
     */
    public void setEnvironmentLabel(String environmentLabel) {
        this.environmentLabel = environmentLabel;
    }

    /**
     * @return the bufferDistDegrees
     */
    public double getBufferDistDegrees() {
        return bufferDistDegrees;
    }

    /**
     * @param bufferDistDegrees the bufferDistDegrees to set
     */
    public void setBufferDistDegrees(double bufferDistDegrees) {
        this.bufferDistDegrees = bufferDistDegrees;
    }

    /**
     * @return the pointExpandDegrees
     */
    public double getPointExpandDegrees() {
        return pointExpandDegrees;
    }

    /**
     * @param pointExpandDegrees the pointExpandDegrees to set
     */
    public void setPointExpandDegrees(double pointExpandDegrees) {
        this.pointExpandDegrees = pointExpandDegrees;
    }

    /**
     * @return the supplyNodeMapCapacity
     */
    public int getSupplyNodeMapCapacity() {
        return supplyNodeMapCapacity;
    }

    /**
     * @param supplyNodeMapCapacity the supplyNodeMapCapacity to set
     */
    public void setSupplyNodeMapCapacity(int supplyNodeMapCapacity) {
        this.supplyNodeMapCapacity = supplyNodeMapCapacity;
    }

    /**
     * @return the supplyNodeMapLoading
     */
    public float getSupplyNodeMapLoading() {
        return supplyNodeMapLoading;
    }

    /**
     * @param supplyNodeMapLoading the supplyNodeMapLoading to set
     */
    public void setSupplyNodeMapLoading(float supplyNodeMapLoading) {
        this.supplyNodeMapLoading = supplyNodeMapLoading;
    }

    /**
     * @return the customerMapCapacity
     */
    public int getCustomerMapCapacity() {
        return customerMapCapacity;
    }

    /**
     * @param customerMapCapacity the customerMapCapacity to set
     */
    public void setCustomerMapCapacity(int customerMapCapacity) {
        this.customerMapCapacity = customerMapCapacity;
    }

    /**
     * @return the customerMapLoading
     */
    public float getCustomerMapLoading() {
        return customerMapLoading;
    }

    /**
     * @param customerMapLoading the customerMapLoading to set
     */
    public void setCustomerMapLoading(float customerMapLoading) {
        this.customerMapLoading = customerMapLoading;
    }

    /**
     * @return the sourceDbConn
     */
    public String getSourceDbConn() {
        return sourceDbConn;
    }

    /**
     * @param sourceDbConn the sourceDbConn to set
     */
    public void setSourceDbConn(String sourceDbConn) {
        this.sourceDbConn = sourceDbConn;
    }

    /**
     * @return the inputProjWKID
     */
    public int getInputProjWKID() {
        return inputProjWKID;
    }

    /**
     * @param inputProjWKID the inputProjWKID to set
     */
    public void setInputProjWKID(int inputProjWKID) {
        this.inputProjWKID = inputProjWKID;
    }

    /**
     * @return the outputProjWKID
     */
    public int getOutputProjWKID() {
        return outputProjWKID;
    }

    /**
     * @param outputProjWKID the outputProjWKID to set
     */
    public void setOutputProjWKID(int outputProjWKID) {
        this.outputProjWKID = outputProjWKID;
    }

    /**
     * @return the fileMaxSizeBytes
     */
    public long getFileMaxSizeBytes() {
        return fileMaxSizeBytes;
    }

    /**
     * @param fileMaxSizeBytes the fileMaxSizeBytes to set
     */
    public void setFileMaxSizeBytes(long fileMaxSizeBytes) {
        this.fileMaxSizeBytes = fileMaxSizeBytes;
    }

    /**
     * @return the sourceDbSQL
     */
    public String getSourceDbSQL() {
        return sourceDbSQL;
    }

    /**
     * @param sourceDbSQL the sourceDbSQL to set
     */
    public void setSourceDbSQL(String sourceDbSQL) {
        this.sourceDbSQL = sourceDbSQL;
    }

    /**
     * @return the outputToKml
     */
    public boolean isOutputToKml() {
        return outputToKml;
    }

    /**
     * @param outputToKml the outputToKml to set
     */
    public void setOutputToKml(boolean outputToKml) {
        this.outputToKml = outputToKml;
    }

    /**
     * @return the kmlDirectory
     */
    public String getKmlDirectory() {
        return kmlDirectory;
    }

    /**
     * @param kmlDirectory the kmlDirectory to set
     */
    public void setKmlDirectory(String kmlDirectory) {
        this.kmlDirectory = kmlDirectory;
    }

    /**
     * @return the bufferOn
     */
    public boolean isBufferOn() {
        return bufferOn;
    }

    /**
     * @param bufferOn the bufferOn to set
     */
    public void setBufferOn(boolean bufferOn) {
        this.bufferOn = bufferOn;
    }

    /**
     * @return the densifyDistDegrees
     */
    public double getDensifyDistDegrees() {
        return densifyDistDegrees;
    }

    /**
     * @param densifyDistDegrees the densifyDistDegrees to set
     */
    public void setDensifyDistDegrees(double densifyDistDegrees) {
        this.densifyDistDegrees = densifyDistDegrees;
    }

    /**
     * @return the densifyDistDegrees
     */
    public int getDensifyMaxVertices() {
        return this.densifyMaxVertices;
    }

    /**
     * @param densifyMaxVertices the densifyDistDegrees to set
     */
    public void setDensifyMaxVertices(int densifyMaxVertices) {
        this.densifyMaxVertices = densifyMaxVertices;
    }

    /**
     * @return the serviceEnvelope
     */
    public Envelope getServiceEnvelope() {
        return serviceEnvelope;
    }

    /**
     * @param serviceEnvelope the serviceEnvelope to set
     */
    public void setServiceEnvelope(Envelope serviceEnvelope) {
        this.serviceEnvelope = serviceEnvelope;
    }

    /**
     * @return the mergeOn
     */
    public boolean isMergeOn() {
        return mergeOn;
    }

    /**
     * @param mergeOn the mergeOn to set
     */
    public void setMergeOn(boolean mergeOn) {
        this.mergeOn = mergeOn;
    }

    /**
     * @return the convexHullEvents
     */
    public boolean isConvexHullEvents() {
        return convexHullEvents;
    }

    /**
     * @param convexHullEvents the convexHullEvents to set
     */
    public void setConvexHullEvents(boolean convexHullEvents) {
        this.convexHullEvents = convexHullEvents;
    }

    /**
     * @return the serviceEnvelopeWkid
     */
    public int getServiceEnvelopeWkid() {
        return serviceEnvelopeWkid;
    }

    /**
     * @param serviceEnvelopeWkid the serviceEnvelopeWkid to set
     */
    public void setServiceEnvelopeWkid(int serviceEnvelopeWkid) {
        this.serviceEnvelopeWkid = serviceEnvelopeWkid;
    }

    /**
     * @return the fileOutputName
     */
    public String getFileOutputName() {
        return fileOutputName;
    }

    /**
     * @param fileOutputName the fileOutputName to set
     */
    public void setFileOutputName(String fileOutputName) {
        this.fileOutputName = fileOutputName;
    }

    /**
     * @return the logDirectory
     */
    public String getLogDirectory() {
        return logDirectory;
    }

    /**
     * @param logDirectory the logDirectory to set
     */
    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
    }

    /**
     * @return the outputToGeoDb
     */
    public boolean isOutputToGeoDb() {
        return outputToGeoDb;
    }

    /**
     * @param outputToGeoDb the outputToGeoDb to set
     */
    public void setOutputToGeoDb(boolean outputToGeoDb) {
        this.outputToGeoDb = outputToGeoDb;
    }

    /**
     * @return the geoDbConn
     */
    public String getGeoDbConn() {
        return geoDbConn;
    }

    /**
     * @param geoDbConn the geoDbConn to set
     */
    public void setGeoDbConn(String geoDbConn) {
        this.geoDbConn = geoDbConn;
    }

    /**
     * @return the geoDbFeatureClassTable
     */
    public String getGeoDbFeatureClassTable() {
        return geoDbFeatureClassTable;
    }

    /**
     * @param geoDbFeatureClassTable the geoDbFeatureClassTable to set
     */
    public void setGeoDbFeatureClassTable(String geoDbFeatureClassTable) {
        this.geoDbFeatureClassTable = geoDbFeatureClassTable;
    }

    /**
     * @return the geoDbBatchSize
     */
    public int getGeoDbBatchSize() {
        return geoDbBatchSize;
    }

    /**
     * @param geoDbBatchSize the geoDbBatchSize to set
     */
    public void setGeoDbBatchSize(int geoDbBatchSize) {
        this.geoDbBatchSize = geoDbBatchSize;
    }

    /**
     * @return the emailAlerts
     */
    public boolean isEmailAlerts() {
        return emailAlerts;
    }

    /**
     * @param emailAlerts the emailAlerts to set
     */
    public void setEmailAlerts(boolean emailAlerts) {
        this.emailAlerts = emailAlerts;
    }

    /**
     * @return the emailUser
     */
    public String getEmailUser() {
        return emailUser;
    }

    /**
     * @param emailUser the emailUser to set
     */
    public void setEmailUser(String emailUser) {
        this.emailUser = emailUser;
    }

    /**
     * @return the smtpMtaHost
     */
    public String getSmtpMtaHost() {
        return smtpMtaHost;
    }

    /**
     * @param smtpMtaHost the smtpMtaHost to set
     */
    public void setSmtpMtaHost(String smtpMtaHost) {
        this.smtpMtaHost = smtpMtaHost;
    }

    /**
     * @return the smtpPort
     */
    public int getSmtpPort() {
        return smtpPort;
    }

    /**
     * @param smtpPort the smtpPort to set
     */
    public void setSmtpPort(int smtpPort) {
        this.smtpPort = smtpPort;
    }

    /**
     * @return the emailOriginator
     */
    public String getEmailOriginator() {
        return emailOriginator;
    }

    /**
     * @param emailOriginator the emailOriginator to set
     */
    public void setEmailOriginator(String emailOriginator) {
        this.emailOriginator = emailOriginator;
    }

    /**
     * @return the emailRecipient
     */
    public String getEmailRecipient() {
        return emailRecipient;
    }

    /**
     * @param emailRecipient the emailRecipient to set
     */
    public void setEmailRecipient(String emailRecipient) {
        this.emailRecipient = emailRecipient;
    }

    /**
     * @return the maxSupplyNodePointDist
     */
    public double getMaxSupplyNodePointDist() {
        return maxSupplyNodePointDist;
    }

    /**
     * @param maxSupplyNodePointDist the maxSupplyNodePointDist to set
     */
    public void setMaxSupplyNodePointDist(double maxSupplyNodePointDist) {
        this.maxSupplyNodePointDist = maxSupplyNodePointDist;
    }
}
