package scl.oms.outagemap;

import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polygon;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a file with GeoJSON data.
 *
 * @author jstewart
 */
public class GeoJsonCreator {

    /**
     * Creates a file with GeoJSON data.
     *
     * @param eventMap
     * @throws IOException is thrown if application can not write to the JSON
     * file.
     */
    public static void createGeoJson(EventMap eventMap, String applicationPath) throws IOException {

        Iterator<Long> eventKeyItr = eventMap.keySet().iterator();
        Long eventKey;
        while (eventKeyItr.hasNext()) {
            eventKey = eventKeyItr.next();
            Polygon[] eventPolygons = eventMap.getEventPolygons(eventKey);

            Logger log = Log.getLogger();
            
            String outputFilePath;
            if (Config.INSTANCE.getKmlDirectory().length() == 0) {
                outputFilePath = applicationPath + "json/" + Config.INSTANCE.getFileOutputName() + ".json";
            } else {
                outputFilePath = Config.INSTANCE.getKmlDirectory() + '/' + Config.INSTANCE.getFileOutputName() + ".json";
            }
            log.log(Level.INFO, "Attempting to write JSON file to '{0}'. (environment={1})",
                    new Object[]{outputFilePath, Config.INSTANCE.getEnvironmentLabel()});

            try (FileWriter outputFile = new FileWriter(outputFilePath)) {
                for (Polygon polygon : eventPolygons) {
                    outputFile.write(GeometryEngine.geometryToGeoJson(polygon));
                }
            }
        }
    }
}
