package scl.oms.outagemap;

import com.esri.core.geometry.Bufferer;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Line;
import com.esri.core.geometry.MultiPoint;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The class represents the points for an individual supply node (i.e.
 * transformer)
 *
 * @author stewarjd
 * @param <E> Points
 */
public class PointQueue<E> extends ConcurrentLinkedQueue {

    private static double expandY;
    private static double expandX;

    private Point labelPoint;
    private boolean modifiedFlag; // used to synchronize updates
    private Polygon thisPolygon;

    /**
     * Initializes queue.
     */
    public PointQueue() {
        super();
        labelPoint = new Point();
        modifiedFlag = true;

        expandX = Config.INSTANCE.getPointExpandDegrees();
        if (Config.INSTANCE.getOutputProjWKID() == 4326) {
            double centerY = Config.INSTANCE.getServiceEnvelope().getCenterY();
            double latInRadians = Math.toRadians(centerY);
            expandY = Config.INSTANCE.getPointExpandDegrees() * Math.cos(latInRadians);
        } else {
            expandY = Config.INSTANCE.getPointExpandDegrees();
        }
    }

    /**
     * Adds a point to the supply node.
     *
     * @param point Point to add.
     * @throws Exception When a point added is further away then the max supply nod distance
     */
    public void addPoint(Point point) throws Exception {
        // check to see if the added point is within an acceptable distance
        if (this.size() > 0) {
            double distFromLabelPt = GeometryEngine.distance(labelPoint, point, null);
            if (distFromLabelPt > Config.INSTANCE.getMaxSupplyNodePointDist()) {
                throw new Exception("Point exceeded max supply noded distance. Distance = " + distFromLabelPt);
            }
            // labelPoint is weighted on the number of customers, not geographic center
            labelPoint.setX(labelPoint.getX() + (point.getX() - labelPoint.getX()) / this.size());
            labelPoint.setY(labelPoint.getY() + (point.getY() - labelPoint.getY()) / this.size());
        } else {
            labelPoint.setX(point.getX());
            labelPoint.setY(point.getY());
        }

        super.add(point);
        modifiedFlag = true; // set true after every point added
    }

    /**
     * Computes the polygon for this supply node, which is buffered at at the
     * configuration value density
     *
     * @return The supply node polygon.
     */
    public Polygon getPolygon() {
        if (modifiedFlag == false) {
            return thisPolygon;
        }

        if (this.size() == 0) {
            return null; // consider throwing an exception instead of this
        }

        MultiPoint multipointGeometry = new MultiPoint();

        // create a multi point geometry
        Iterator<Point> pointItr = this.iterator();
        while (pointItr.hasNext()) {
            Point point = pointItr.next();
            multipointGeometry.add(point);
        }

        Geometry convexHullGeometry = GeometryEngine.convexHull(multipointGeometry);

        Polygon unbufferedPolygon = null;
        switch (convexHullGeometry.getType()) {
            case Point:
                Point point = (Point) convexHullGeometry;
                unbufferedPolygon = PointQueue.getPolygonFrom1Point(point);
                break;
            case Line:
                Line line = (Line) convexHullGeometry;
                Point startPoint = new Point(line.getStartX(), line.getStartY());
                Point endPoint = new Point(line.getEndX(), line.getEndY());
                unbufferedPolygon = PointQueue.getPolygonFrom2Points(startPoint, endPoint);
                break;
            case MultiPoint:
                MultiPoint multiPoint = (MultiPoint) convexHullGeometry;
                switch (multiPoint.getPointCount()) {
                    case 1:
                        Point p = (Point) multiPoint.getPoint(0);
                        unbufferedPolygon = PointQueue.getPolygonFrom1Point(p);
                        break;
                    case 2:
                        Point a = (Point) multiPoint.getPoint(0);
                        Point b = (Point) multiPoint.getPoint(1);
                        unbufferedPolygon = PointQueue.getPolygonFrom2Points(a, b);
                        break;
                    default:
                        return null; // Multipoint with more than 2 points not found in dev
                }
                break;
            case Polygon:
                unbufferedPolygon = (Polygon) convexHullGeometry;
                if (unbufferedPolygon.getPathCount() > 1) {
                    // note: this case not detected in dev
                    System.out.println("ARGH!!!!: you need to trim inside paths.");
                }
                if (unbufferedPolygon.calculateArea2D() == 0.0) {
                    unbufferedPolygon = PointQueue.getPolygonFrom1Point(unbufferedPolygon.getPoint(0));
                }
                break;
            case Polyline:
                Polyline polyline = (Polyline) convexHullGeometry;
                switch (polyline.getPointCount()) {
                    case 1:
                        Point p = (Point) polyline.getPoint(0);
                        unbufferedPolygon = PointQueue.getPolygonFrom1Point(p);
                        break;
                    case 2:
                        Point a = (Point) polyline.getPoint(0);
                        Point b = (Point) polyline.getPoint(1);
                        unbufferedPolygon = PointQueue.getPolygonFrom2Points(a, b);
                        break;
                    default:
                        return null; // Polyline with more than 2 points not found in dev
                }
                break;
            default:
                System.out.println("PointQueue.getPolygon().convexHullGeometry.getType() isn't understood, equals" + convexHullGeometry.getType());
                return null; // no other known Geometry types
        }

        if (Config.INSTANCE.isBufferOn()) {

            SpatialReference sr = SpatialReference.create(Config.INSTANCE.getOutputProjWKID());
            thisPolygon = (Polygon) Bufferer.buffer(unbufferedPolygon, Config.INSTANCE.getBufferDistDegrees(),
                    sr, Config.INSTANCE.getDensifyDistDegrees(),
                    Config.INSTANCE.getDensifyMaxVertices(), null);
        } else {
            thisPolygon = unbufferedPolygon;
        }

        modifiedFlag = false; // set false after every update
        return thisPolygon;
    }

    /**
     * Provides a point for a label of the supply node, which is weighted on the
     * average customer location, not the average geographical location.
     *
     * Note that the label location is computed and set during the .getPolygon()
     * method and remains unchanged as long as no new points are added.
     *
     * @return If the getPolygon method had been previously called, then a Point
     * at the centroid of the supply node, else a null value.
     */
    public Point getLabelPoint() {
        if (labelPoint == null) {
            this.getPolygon();
        }
        return labelPoint;
    }

    // Creates a small diamond polygon centered on a single point.
    private static Polygon getPolygonFrom1Point(Point point) {
        MultiPoint multipointGeometry = new MultiPoint();
        multipointGeometry.add(new Point(point.getX() + expandX, point.getY()));
        multipointGeometry.add(new Point(point.getX() - expandX, point.getY()));
        multipointGeometry.add(new Point(point.getX(), point.getY() + expandY));
        multipointGeometry.add(new Point(point.getX(), point.getY() - expandY));
        return (Polygon) GeometryEngine.convexHull(multipointGeometry);
    }

    // Adds a small diamond centered on the midpoint between two points.
    private static Polygon getPolygonFrom2Points(Point a, Point b) {
        double midPointX = (a.getX() + b.getX()) / 2.0;
        double midPointY = (a.getY() + b.getY()) / 2.0;
        MultiPoint multipointGeometry = new MultiPoint();
        multipointGeometry.add(a);
        multipointGeometry.add(new Point(midPointX + expandX, midPointY));
        multipointGeometry.add(new Point(midPointX - expandX, midPointY));
        multipointGeometry.add(new Point(midPointX, midPointY + expandY));
        multipointGeometry.add(new Point(midPointX, midPointY - expandY));
        multipointGeometry.add(b);
        return (Polygon) GeometryEngine.convexHull(multipointGeometry);
    }
}
