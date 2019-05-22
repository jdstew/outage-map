package scl.oms.outagemap;

import com.esri.core.geometry.Envelope;
import static com.esri.core.geometry.Geometry.Type.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * This class represents a supply node.
 *
 * @author stewarjd
 * @param <K> Integer
 * @param <V> HashSet
 */
public class SupplyNodeMap<K, V> extends HashMap {

    private Polygon supplyNodePolygon;
    private Point labelPoint;
    private boolean modifiedFlag; // used to synchronize updates
    private Polygon[] thesePolygons;

    /**
     * Initializes the supply node map.
     */
    public SupplyNodeMap() {
        super(Config.INSTANCE.getSupplyNodeMapCapacity(), Config.INSTANCE.getSupplyNodeMapLoading());
        modifiedFlag = true;
    }

    /**
     * Adds a point to the supply node.
     *
     * @param supplyNodeId
     * @param point
     */
    public void addPoint(Long supplyNodeId, Point point) throws Exception {
        if (super.containsKey(supplyNodeId)) {
            PointQueue pointQueue = (PointQueue) this.get(supplyNodeId);
            pointQueue.addPoint(point);
        } else {
            PointQueue<Point> pointQueue = new PointQueue();
            pointQueue.addPoint(point);
            this.put(supplyNodeId, pointQueue);
        }
        modifiedFlag = true; // set true after every point added
    }

    /**
     * Computes the event's polygons from the underlying supply nodes and
     * points.
     *
     * @return Polygons that represent the event's supply nodes.
     */
    public Polygon[] getPolygons() {
        if (modifiedFlag == false) {
            return thesePolygons;
        }

        // compute the envelope of supply nodes
        // this is used to order the supply nodes circularly
        Envelope eventEnvelope = new Envelope();
        {
            PointQueue pointQueue;
            for (Object hashMapValue : this.values()) {
                pointQueue = (PointQueue) hashMapValue;
                eventEnvelope.merge(pointQueue.getLabelPoint());
            }
        }
        Point eventCenterPoint = eventEnvelope.getCenter();

        // order the supply node polygons
        ConcurrentSkipListMap<Double, Polygon> orderedSupplyNodePolygons = new ConcurrentSkipListMap();
        Iterator<Long> supplyNodeItr = this.keySet().iterator();
        Long supplyNodeKey;
        while (supplyNodeItr.hasNext()) {
            supplyNodeKey = supplyNodeItr.next();
            PointQueue pointQueue = (PointQueue) this.get(supplyNodeKey);

            Point supplyNodeLabel = pointQueue.getLabelPoint();
            double rotationAngle = Math.atan2(
                    (supplyNodeLabel.getY() - eventCenterPoint.getY()),
                    (supplyNodeLabel.getX() - eventCenterPoint.getX()));

            orderedSupplyNodePolygons.put(rotationAngle, pointQueue.getPolygon());
        }

        ArrayList<Polygon> supplyNodeArray = new ArrayList(orderedSupplyNodePolygons.values());
        if (Config.INSTANCE.isMergeOn()) {
            SpatialReference sr = SpatialReference.create(Config.INSTANCE.getOutputProjWKID());

            // the following conditional do statement loops untill all 
            // of the polygons in an event are merged (theoretically)
            boolean mergingOccured;
            do {
                mergingOccured = false;

                SEARCH_FOR_OVERLAP:
                for (Polygon polygonInArray : supplyNodeArray) {
                    for (Polygon testPolygon : supplyNodeArray) {
                        if (testPolygon == polygonInArray) {
                            break;
                        }
                        if (GeometryEngine.overlaps(polygonInArray, testPolygon, sr)) {
                            Polygon combinedPolygon = (Polygon) GeometryEngine.union(new Polygon[]{polygonInArray, testPolygon}, sr);
                            GeometryTool.cleanPolygon(combinedPolygon);
                            
                            supplyNodeArray.remove(testPolygon);
                            supplyNodeArray.remove(polygonInArray);
                            supplyNodeArray.add(combinedPolygon);
                            mergingOccured = true;
                            break SEARCH_FOR_OVERLAP;
                        }
                    }
                }
            } while (mergingOccured);
        }

        thesePolygons = supplyNodeArray.toArray(new Polygon[supplyNodeArray.size()]);

        // check again and remove for any interior paths
        // find polygon with greatest number of points (aka largest polygon)
        Polygon largestPolygon = null;
        for (Polygon testPolygon : thesePolygons) {
            GeometryTool.cleanPolygon(testPolygon);
            if (largestPolygon != null) {
                if (testPolygon.getPointCount() > largestPolygon.getPointCount()) {
                    largestPolygon = testPolygon;
                }
            } else {
                largestPolygon = testPolygon;
            }
        }
        // put the label point on the largest polygon
        labelPoint = GeometryTool.getPolygonCenterOfMass(largestPolygon);

        modifiedFlag = false; // set false after every update
        return thesePolygons;
    }

    /**
     * Provides a point for a label of the event, which is average location of
     * the supply node labels for this event.
     *
     * Note that the label location is computed and set during the .getPolygon()
     * method and remains unchanged as long as no new points are added.
     *
     * @return If the getPolygons method had been previously called, then a
     * Point at the centroid of the event, else a null value.
     */
    public Point getLabelPoint() {
        if (labelPoint == null) {
            this.getPolygons();
        }
        return labelPoint;
    }

    /**
     * Gets the count of supply nodes.
     *
     * @return count of supply nodes
     */
    public int getSupplyNodeCount() {
        return this.size();
    }

    /**
     * Gets the total count of points in the supply nodes.
     *
     * @return count of points in the supply nodes
     */
    public int getPointCount() {
        int pointCount = 0;
        HashMap points;
        Iterator iterator = this.entrySet().iterator();
        while (iterator.hasNext()) {
            points = (HashMap) iterator.next();
            pointCount += points.size();
        }
        return pointCount;
    }
}
