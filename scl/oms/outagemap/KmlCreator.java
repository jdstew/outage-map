package scl.oms.outagemap;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a KML file.
 *
 * @author jstewart
 */
public class KmlCreator {
    
    static boolean hasCovexedEvents = false; // flag to make one attempt a shrinking KML file size

    /**
     * Creates a KML file.
     *
     * @param applicationPath the path to the application's main class or .jar
     * file
     * @param eventMap
     * @throws IOException
     */
    public static void createKml(EventMap eventMap, String applicationPath) throws IOException {

        long kmlStartTime = System.currentTimeMillis();
        int eventsMapped = 0;
        int polygonsMapped = 0;
        int pointsMapped = 0;

        Logger log = Log.getLogger();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        DecimalFormat decimalFormat = new DecimalFormat("0.0000000");

        String outputFilePath;
        if (Config.INSTANCE.getKmlDirectory().length() == 0) {
            outputFilePath = applicationPath + "kml/" + Config.INSTANCE.getFileOutputName() + ".kml";
        } else {
            outputFilePath = Config.INSTANCE.getKmlDirectory() + '/' + Config.INSTANCE.getFileOutputName() + ".kml";
        }
        log.log(Level.INFO, "Attempting to write KMl file to {0}. (environment={1})",
                new Object[]{outputFilePath, Config.INSTANCE.getEnvironmentLabel()});

        try (FileWriter outputFile = new FileWriter(outputFilePath)) {

            // write to output stream instead of StringBuilder
            StringBuilder kml = new StringBuilder();
            outputFile.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n");
            outputFile.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\r\n");
            outputFile.write("<Document>\r\n");
            outputFile.write("<!-- ");
            outputFile.write(dateFormat.format(new Date(kmlStartTime)));
            outputFile.write(" -->\r\n");
            outputFile.write("<name>Outage Area</name>\r\n");
            outputFile.write("<Style id=\"displayName-value\">\r\n");
            outputFile.write("<PolyStyle>\r\n");
            outputFile.write("<color>990000FF</color>\r\n");
            outputFile.write("<outline>0</outline>\r\n");
            outputFile.write("</PolyStyle>\r\n");
            outputFile.write("<BalloonStyle>\r\n");
            outputFile.write("<text>$[outageInfo]</text>\r\n");
            outputFile.write("</BalloonStyle>\r\n");
            outputFile.write("</Style>\r\n");
            outputFile.write("<Style id=\"displayName-event-marker\">\r\n");
            outputFile.write("<BalloonStyle>\r\n");
            outputFile.write("<text>$[outageInfo]</text>\r\n");
            outputFile.write("</BalloonStyle>\r\n");
            outputFile.write("<IconStyle>\r\n");
            outputFile.write("<scale>1</scale>\r\n");
            outputFile.write("<Icon>\r\n");
            outputFile.write("<href>outage_highlight_marker.png</href>\r\n");
            outputFile.write("</Icon>\r\n");
            outputFile.write("</IconStyle>\r\n");
            outputFile.write("</Style>\r\n");
            outputFile.write("<Style id=\"Srv_Display\">\r\n");
            outputFile.write("<LineStyle>\r\n");
            outputFile.write("<color>88550000</color>\r\n");
            outputFile.write("<width>2</width>\r\n");
            outputFile.write("</LineStyle>\r\n");
            outputFile.write("<PolyStyle>\r\n");
            outputFile.write("<color>000000FF</color>\r\n");
            outputFile.write("<fill>0</fill>\r\n");
            outputFile.write("<outline>1</outline>\r\n");
            outputFile.write("</PolyStyle>\r\n");
            outputFile.write("</Style>\r\n");

            // initiate iterator loop on events
            Iterator<Long> eventKeyItr = eventMap.keySet().iterator();
            Long eventKey;
            eventsMapped = 0;
            while (eventKeyItr.hasNext()) {
                eventKey = eventKeyItr.next();

                outputFile.write("<Placemark id=\"");
                outputFile.write(eventKey.toString());
                outputFile.write(" - Marker\">\r\n");
                outputFile.write("<styleUrl>#displayName-event-marker</styleUrl>\r\n");
                outputFile.write("<ExtendedData>\r\n");
                outputFile.write("<Data name=\"outageInfo\">\r\n");
                outputFile.write("<value><![CDATA[<iframe>Outage start: ");
                outputFile.write(eventMap.getEventBegin(eventKey));
                outputFile.write("<br/>Est. customers affected: ");
                outputFile.write(eventMap.getCustsByEvent(eventKey).toString());
                outputFile.write("<br/>"); // "Est. restoration: " ... provided by db materialized view
                outputFile.write(eventMap.getEtor(eventKey));
                outputFile.write("<br/>Cause: ");
                outputFile.write(eventMap.getCause(eventKey));
                if (eventMap.getCrewDispatched(eventKey) != null) {
                    outputFile.write("<br/>Crew status: ");
                    outputFile.write(eventMap.getCrewDispatched(eventKey));
                }
                outputFile.write("</iframe>]]></value>\r\n");
                outputFile.write("</Data>\r\n");
                outputFile.write("</ExtendedData>\r\n");
                outputFile.write("<Point>\r\n");
                outputFile.write("<coordinates>");
                Point labelPoint = eventMap.getLabelPoint(eventKey);
                outputFile.write(Double.toString(labelPoint.getX()));
                outputFile.write(",");
                outputFile.write(Double.toString(labelPoint.getY()));
                outputFile.write(",0</coordinates>\r\n");
                outputFile.write("</Point>\r\n");
                outputFile.write("</Placemark>\r\n");

                // initiate iterator loop on event polygons
                Polygon[] eventPolygons = null;

                if (Config.INSTANCE.isConvexHullEvents()) {
                    Geometry[] geometries = GeometryEngine.convexHull(eventMap.getEventPolygons(eventKey), true);
                    eventPolygons = new Polygon[geometries.length];
                    for (int i = 0; i < geometries.length; i++) {
                        eventPolygons[i] = (Polygon) geometries[i];
                    }
                } else {
                    eventPolygons = eventMap.getEventPolygons(eventKey);
                }
                for (int eventPolygonIndex = 0; eventPolygonIndex < eventPolygons.length; eventPolygonIndex++) {
                    outputFile.write("<Placemark id=\"");
                    outputFile.write(eventKey + "-" + eventPolygonIndex); //  {note: event number-hyphen-polygon number}
                    outputFile.write("\">\r\n");
                    outputFile.write("<styleUrl>#displayName-value</styleUrl>\r\n");
                    outputFile.write("<ExtendedData>\r\n");
                    outputFile.write("<Data name=\"outageInfo\">\r\n");
                    outputFile.write("<value><![CDATA[<iframe>Outage start: ");
                    outputFile.write(eventMap.getEventBegin(eventKey));
                    outputFile.write("<br/>Est. customers affected: ");
                    outputFile.write(eventMap.getCustsByEvent(eventKey).toString());
                    outputFile.write("<br/>"); // "Est. restoration: " ... provided by db materialized view
                    outputFile.write(eventMap.getEtor(eventKey));
                    outputFile.write("<br/>Cause: ");
                    outputFile.write(eventMap.getCause(eventKey));
                    if (eventMap.getCrewDispatched(eventKey) != null) {
                        outputFile.write("<br/>Crew status: ");
                        outputFile.write(eventMap.getCrewDispatched(eventKey));
                    }
                    outputFile.write("</iframe>]]></value>\r\n");
                    outputFile.write("</Data>\r\n");
                    outputFile.write("</ExtendedData>\r\n");
                    outputFile.write("<Polygon>\r\n");
                    outputFile.write("<altitudeMode />\r\n");
                    outputFile.write("<outerBoundaryIs>\r\n");
                    outputFile.write("<LinearRing>\r\n");
                    outputFile.write("<coordinates>");
                    Polygon polygon = eventPolygons[eventPolygonIndex];
                    if (polygon == null) {
                        log.log(Level.FINEST, "WARNING: polygon in KmlCreator is null. (environment={0})", Config.INSTANCE.getEnvironmentLabel());
                    }
                    for (int i = 0; i < polygon.getPointCount(); i++) {
                        Point point = polygon.getPoint(i);
                        outputFile.write(decimalFormat.format(point.getX()));
                        outputFile.write(",");
                        outputFile.write(decimalFormat.format(point.getY()));
                        outputFile.write(",0 ");
                        pointsMapped++;
                    }
                    outputFile.write("</coordinates>\r\n");
                    outputFile.write("</LinearRing>\r\n");
                    outputFile.write("</outerBoundaryIs>\r\n");
                    outputFile.write("</Polygon>\r\n");
                    outputFile.write("</Placemark>\r\n");
                    polygonsMapped++;
                }
                eventsMapped++;
            }

            // finish file
            outputFile.write("</Document>\r\n");
            outputFile.write("</kml>");
        }

        File kmlFile = new File(outputFilePath);
        if ((kmlFile.length() > Config.INSTANCE.getFileMaxSizeBytes()) && (KmlCreator.hasCovexedEvents == false)) {
            log.log(Level.INFO, "KML output exceeds maximum file size, creating convex hull from each event. (environment={0})", Config.INSTANCE.getEnvironmentLabel());
            Config.INSTANCE.setConvexHullEvents(true);
            KmlCreator.hasCovexedEvents = true;
            KmlCreator.createKml(eventMap, applicationPath);
            try {
                EmailAlertSender.send("Alert: Outage Map", "Map has been generalize due to Google size limitation.");
            } catch (Exception ex1) {
                System.out.println(ex1);
            }
        }

        long kmlFinishTime = System.currentTimeMillis() - kmlStartTime;
        log.log(Level.INFO, "KML metric: {0} event(s) mapped into {1} polygons with {2} polygon points in {3} milliseconds. (environment={4})",
                new Object[]{eventsMapped, polygonsMapped, pointsMapped, kmlFinishTime, Config.INSTANCE.getEnvironmentLabel()});

    }
}
