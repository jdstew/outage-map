package scl.oms.outagemap;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import oracle.jdbc.OracleResultSet;
import oracle.sql.TIMESTAMP;

/**
 * This class is the application that creates the outage map.
 *
 * @author jstewart
 */
public class OutageMapCreator {

    /**
     * @param args the command line arguments, which ARE NOT USED by this
     * application
     */
    public static void main(String[] args) throws SQLException {

        System.out.print("Determining location of application...");
        String applicationPath = null;
        try {
            String path = OutageMapCreator.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            applicationPath = URLDecoder.decode(path.substring(0, path.lastIndexOf("/") + 1), "UTF-8");

            boolean isWindowsHost = System.getProperty("os.name").contains("indow");
            if (isWindowsHost) {
                // the following will trim an errant '/' in front of C:/
                applicationPath = applicationPath.substring(1);
            }

            System.out.println("'" + applicationPath + "'");
        } catch (UnsupportedEncodingException ex) {
            System.out.println("unable to determine, error thrown.");
            System.out.println(ex.getMessage());
            System.exit(1);
        }

        System.out.print("Starting logging...");
        Logger log = null;
        try {
            log = Log.getLogger(applicationPath);
        } catch (IOException ex) {
            System.out.println("ERROR: unable to establish application logging. " + ex.getMessage());
            System.exit(1);
        }
        System.out.println("completed.");
        log.log(Level.INFO, "Logging established. (environment={0})", Config.INSTANCE.getEnvironmentLabel());

        log.log(Level.INFO, "Loading configuration. (environment={0})", Config.INSTANCE.getEnvironmentLabel());
        try {
            Config.INSTANCE.loadConfig(applicationPath);
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
            System.out.println("ERROR: unable to load configuration data. " + ex.getMessage());
            try {
                EmailAlertSender.send("FAILURE: Outage Map", "Unable to load configuration data. " + ex.toString());
            } catch (Exception ex1) {
                System.out.println(ex1);
            }
            System.exit(1);
        }
        log.log(Level.INFO, "Configuration loaded. (environment={0})", Config.INSTANCE.getEnvironmentLabel());

        if (Config.INSTANCE.isDebugMode()) {
            log.setLevel(Level.ALL);
            log.log(Level.INFO, "Debug mode on (Log.ALL) (environment={0})", Config.INSTANCE.getEnvironmentLabel());
        } else {
            log.setLevel(Level.INFO);
        }

        // Convert service territory envelope as needed to the output WKID
        Config.INSTANCE.setServiceEnvelope(ProjectTool.project(Config.INSTANCE.getServiceEnvelope(),
                Config.INSTANCE.getServiceEnvelopeWkid(), Config.INSTANCE.getOutputProjWKID()));
        Config.INSTANCE.setServiceEnvelopeWkid(Config.INSTANCE.getOutputProjWKID());
        Envelope serviceTerritory = Config.INSTANCE.getServiceEnvelope();

        // Events contain OMS/NMS events, which contain supply nodes, which
        // contain customers that are out.
        EventMap<Long, SupplyNodeMap> events = new EventMap();
        int recordsRead = 0;
        int recordsMapped = 0;
        int recordsOutsideServiceTerritory = 0;
        int eventCount = 0;
        long databaseStartTime = System.currentTimeMillis();

        try {
            // Get outage dataset
            OracleResultSet customersOut = OutageDataFactory.getCustomersOut();

            double customerY;
            double customerX;
            long eventIdx;
            int custsByEvent;
            long supplyIdx;

            DateFormat dateFormat;
            dateFormat = new SimpleDateFormat("h:mm a, MM/dd");

            // Iterate on outage data points by [event][supply node][customer]
            while (customersOut.next()) {

                customerY = customersOut.getCHAR("POINT_Y").doubleValue();
                customerX = customersOut.getCHAR("POINT_X").doubleValue();
                String cause = customersOut.getCHAR("EVENT_CAUSE").stringValue();
                String etor = customersOut.getCHAR("EVENT_ETOR_TEXT").stringValue();
                eventIdx = customersOut.getNUMBER("EVENT_IDX").longValue();
                custsByEvent = customersOut.getNUMBER("CUSTSBYEVENT").intValue();
                TIMESTAMP eventBegin = customersOut.getTIMESTAMP("EVENT_BEGIN");
                TIMESTAMP firstCrewTime = customersOut.getTIMESTAMP("FIRST_CREW_TIME");
                supplyIdx = customersOut.getNUMBER("SUPPLY_IDX").longValue();
                recordsRead++;

                String eventBeginText;
                if (eventBegin != null) {
                    eventBeginText = dateFormat.format(eventBegin.dateValue());
                } else {
                    eventBeginText = "unknown";
                }

                String crewDispatched;
                if (firstCrewTime != null) {
                    crewDispatched = "Dispatched";
                } else {
                    crewDispatched = null;
                }

                // Add points to events
                // note: Point takes X, Y ~ lon, lat
                Point point = ProjectTool.project(new Point(customerX, customerY),
                        Config.INSTANCE.getInputProjWKID(), Config.INSTANCE.getOutputProjWKID());

                if (serviceTerritory.contains(point)) {
                    try {
                        events.addPoint(eventIdx, supplyIdx, point, cause, etor,
                                custsByEvent, eventBeginText, crewDispatched);
                    } catch (Exception ex) {
                        log.log(Level.INFO, "Point for supply node " + supplyIdx + " dropped. " + ex.getMessage());
                    }
                    recordsMapped++;
                } else {
                    recordsOutsideServiceTerritory++;
                    log.log(Level.INFO, "The following point is outside the service territory {0} (environment={1})",
                            new Object[]{point, Config.INSTANCE.getEnvironmentLabel()});
                }
            }

            OutageDataFactory.closeDatabaseConnection();

        } catch (IOException ex) {
            log.log(Level.SEVERE, ex.toString());
            log.log(Level.SEVERE, "Closing logging session. (environment={0})", Config.INSTANCE.getEnvironmentLabel());
            Log.closeLogger();
            try {
                EmailAlertSender.send("FAILURE: Outage Map", "Unable to read outage map database. " + ex.toString());
            } catch (Exception ex1) {
                System.out.println(ex1);
            }
            System.exit(1);
        } catch (SQLException ex) {
            log.log(Level.SEVERE, ex.toString());
            log.log(Level.SEVERE, "Closing logging session. (environment={0})", Config.INSTANCE.getEnvironmentLabel());
            Log.closeLogger();
            try {
                EmailAlertSender.send("FAILURE: Outage Map", "Unable to read outage map database. " + ex.toString());
            } catch (Exception ex1) {
                System.out.println(ex1);
            }
            System.exit(1);
        }

        // Provide database input data metrics
        long databaseFinishTime = System.currentTimeMillis() - databaseStartTime;
        log.log(Level.INFO, "Database metric: {0} customer record points(s) read in {1} milliseconds. (environment={2})",
                new Object[]{recordsRead, databaseFinishTime, Config.INSTANCE.getEnvironmentLabel()});

        // Provide mapped data metrics 
        Iterator<Long> eventKeyItr = events.keySet().iterator();
        Long eventKey;
        int supplyNodeCount = 0;
        while (eventKeyItr.hasNext()) {
            eventKey = eventKeyItr.next();
            supplyNodeCount += ((SupplyNodeMap) events.get(eventKey)).size();
        }
        log.log(Level.INFO, "Input processing metrics: {0} geographic points mapped to {1} supply node(s) and {2} event(s); "
                + " {3} points lied outside service territory. (environment={4})",
                new Object[]{recordsMapped, supplyNodeCount, events.size(),
                    recordsOutsideServiceTerritory, Config.INSTANCE.getEnvironmentLabel()});

        if (Config.INSTANCE.isOutputToKml()) {
            try {
                KmlCreator.createKml(events, applicationPath);
            } catch (IOException ex) {
                log.log(Level.SEVERE, null, ex);
                try {
                    EmailAlertSender.send("FAILURE: Outage Map", "Unable to write to KML file. " + ex.toString());
                } catch (Exception ex1) {
                    System.out.println(ex1);
                }
            }
        }

        // insert code to create outage summary (HTML or JSON?) here

        
        if (Config.INSTANCE.isOutputToGeoDb()) {
            try {
                FeatureClassWriter.writeFeatureClass(events);
            } catch (IOException ex) {
                log.log(Level.SEVERE, null, ex);
                try {
                    EmailAlertSender.send("Warning: Outage Map", "Unable to write to GIS database. " + ex.toString());
                } catch (Exception ex1) {
                    System.out.println(ex1);
                }
            } catch (Exception ex) {
                log.log(Level.SEVERE, null, ex);
                try {
                    EmailAlertSender.send("Warning: Outage Map", "Unable to write to GIS database. " + ex.toString());
                } catch (Exception ex1) {
                    System.out.println(ex1);
                }
            }
        }

        log.log(Level.INFO, "Closing logging session. (environment={0})", Config.INSTANCE.getEnvironmentLabel());
        Log.closeLogger();

        System.out.println("Exiting successfully.");
        System.exit(0);
    }

}
