/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scl.oms.outagemap;

import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import oracle.jdbc.pool.OracleDataSource;
import oracle.spatial.geometry.JGeometry;
import oracle.sql.DATE;
import oracle.sql.STRUCT;

/**
 *
 * @author jstewart
 */
public class FeatureClassWriter {

    public static final int WGS84_SRID = 4326; //WGS84
    public static final int WSP83_SRID = 2926; //HARN/WO.WA-NF

    private static OracleConnection targetGeoDBConn;

    private static OracleConnection getDatabaseConnection() throws IOException, SQLException {
        Logger log = Log.getLogger();
        log.log(Level.FINEST, "Creating target Oracle geodatabase connection, using: {0} (environment={1})",
                new Object[]{Config.INSTANCE.getGeoDbConn(), Config.INSTANCE.getEnvironmentLabel()});
        OracleDataSource sourceODS = new OracleDataSource();
        sourceODS.setURL("jdbc:oracle:thin:" + Config.INSTANCE.getGeoDbConn());
        log.log(Level.FINEST, "Target OracleDataSource set to: {0} (environment={1})",
                new Object[]{sourceODS.getURL(), Config.INSTANCE.getEnvironmentLabel()});
        try {
            return (OracleConnection) sourceODS.getConnection();
        } catch (java.sql.SQLException e) {
            log.severe(e.toString());
            throw e;
        } catch (Exception e) {
            log.severe(e.toString());
            throw e;
        }
    }

    public static void writeFeatureClass(EventMap eventMap) throws IOException, SQLException, Exception {
        targetGeoDBConn = FeatureClassWriter.getDatabaseConnection();
        targetGeoDBConn.setDefaultExecuteBatch(Config.INSTANCE.getGeoDbBatchSize());
        Logger log = Log.getLogger();

        long geoDbUpdateStart = System.currentTimeMillis();
        int eventsMapped = 0;
        int polygonsMapped = 0;
        int pointsMapped = 0;

        // initiate iterator loop on events
        Iterator<Long> eventKeyItr = eventMap.keySet().iterator();
        Long eventKey;

        String insertSql = "INSERT INTO " + Config.INSTANCE.getGeoDbFeatureClassTable()
                + "(SHAPE, EVENT_IDX) VALUES (?, ?)";
        PreparedStatement insertStatement = targetGeoDBConn.prepareStatement(insertSql);

        // iterate over events
        while (eventKeyItr.hasNext()) {
          
            eventKey = eventKeyItr.next();

            // iterate over polygons for a single event
            Polygon[] eventPolygons = null;
            eventPolygons = eventMap.getEventPolygons(eventKey);

            for (int eventPolygonIndex = 0; eventPolygonIndex < eventPolygons.length; eventPolygonIndex++) {               
                Polygon polygon = eventPolygons[eventPolygonIndex];

                int coordsIndex = 0;
                double[] coords = new double[polygon.getPointCount() * 2];

                // iterate over points for a single  polygon
                for (int i = 0; i < polygon.getPointCount(); i++) {
                    Point point = polygon.getPoint(i);
                    coords[coordsIndex++] = point.getX();
                    coords[coordsIndex++] = point.getY();
                    pointsMapped++;
                }
                // the value '2' in the next line indicates 2D coordinates
                JGeometry sdo_geometry = JGeometry.createLinearPolygon(coords, 2, FeatureClassWriter.WGS84_SRID);
                STRUCT structuredObject = JGeometry.store(targetGeoDBConn, sdo_geometry);
                log.log(Level.ALL, "STRUCT.dump() ==> {0}", structuredObject.dump());
                ((OraclePreparedStatement) insertStatement).setObject(1, structuredObject);
                ((OraclePreparedStatement) insertStatement).setLong(2, eventKey);
                insertStatement.execute();
                polygonsMapped++;
            }
            eventsMapped++;
        }
        
        insertStatement.close();

        long geoDbUpdateTime = System.currentTimeMillis() - geoDbUpdateStart;
        log.log(Level.INFO, "Geodatabase update metric: {0} event(s) mapped into {1} polygons with {2} polygon points in {3} milliseconds. (environment={4})",
                new Object[]{eventsMapped, polygonsMapped, pointsMapped, geoDbUpdateTime, Config.INSTANCE.getEnvironmentLabel()});
        
        FeatureClassWriter.closeDatabaseConnection();
    }

    /**
     * Closes the database connection. Note that the database connection can not
     * be closed while the OracleResultSet from .getCustomersOut () is or will
     * be read.
     *
     * @throws IOException
     */
    public static void closeDatabaseConnection() throws IOException {
        try {
            targetGeoDBConn.close();
        } catch (SQLException ex) {
            Logger log = Log.getLogger();
            log.getLogger(OutageDataFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
