package scl.oms.outagemap;

import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;

/**
 * This static class is used to house geometry operations not provided with
 * Esri's Java API
 *
 * @author jstewart
 */
public class GeometryTool {

    /**
     * The following method was derived from the following source:
     * http://paulbourke.net/geometry/polygonmesh/ JAVA code submitted by Ramón
     * Talavera Downloaded: 2015-06-11
     *
     * Modified by jstewart to use Esri Java API objects
     *
     * This method computes the area of a polygon.
     *
     */
    public static double getSignedPolygonArea(Polygon polygon) {
        int i, j;
        double area = 0.0;

        for (i = 0; i < polygon.getPointCount(); i++) {
            j = (i + 1) % polygon.getPointCount();
            Point pointI = polygon.getPoint(i);
            Point pointJ = polygon.getPoint(j);
            area += pointI.getX() * pointJ.getY();
            area -= pointI.getY() * pointJ.getX();
        }
        area /= 2.0;

        return (area);
        //return(area < 0 ? -area : area); for unsigned
    }

    /**
     * The following method was derived from the following source:
     * http://paulbourke.net/geometry/polygonmesh/ JAVA code submitted by Ramón
     * Talavera Downloaded: 2015-06-11
     *
     * Modified by jstewart to use Esri Java API objects
     *
     * This method computes the center of mass of a polygon.
     *
     */
    public static Point getPolygonCenterOfMass(Polygon polygon) {
        double centerX = 0.0, centerY = 0.0;
        double area = GeometryTool.getSignedPolygonArea(polygon);
        int i, j;

        double factor = 0.0;
        for (i = 0; i < polygon.getPointCount(); i++) {
            j = (i + 1) % polygon.getPointCount();
            Point pointI = polygon.getPoint(i);
            Point pointJ = polygon.getPoint(j);
            factor = (pointI.getX() * pointJ.getY() - pointJ.getX() * pointI.getY());
            centerX += (pointI.getX() + pointJ.getX()) * factor;
            centerY += (pointI.getY() + pointJ.getY()) * factor;
        }
        area *= 6.0;
        factor = 1 / area;
        centerX *= factor;
        centerY *= factor;

        return new Point(centerX, centerY);
    }

    /**
     * This method removes all interior and all but the the largest exterior
     * paths from a polygon.  The result is the largest outside polygon.  
     * Additional interior and exterior paths are created when merging (using
     * the union method) polygons.
     * 
     * @param polygon
     * @return the same polygon
     */
    public static Polygon cleanPolygon(Polygon polygon) {
        // remove any interior paths
        if (polygon.getPathCount() > 1) {
            for (int i = 0; i < polygon.getPathCount(); i++) {
                if (!polygon.isExteriorRing(i)) {
                    polygon.removePath(i);
                }
            }
        }

        /*
        remove all exterior paths except for the one with the greatest
        number of points, which should be the 'outside' path ... a test of 
        existing data strongly indicates this process is valid for all cases,
        else the first path is kept
        */
        if (polygon.getPathCount() > 1) {
            // find exterior path with greatest number of points
            int indexOfLargestPath = 0;
            int pointsInLargestPath = 0;
            for (int i = 0; i < polygon.getPathCount(); i++) {
                if (polygon.getPathSize(i) > pointsInLargestPath) {
                    pointsInLargestPath = polygon.getPathSize(i);
                    indexOfLargestPath = i;
                }
            }
            for (int i = 0; i < polygon.getPathCount(); i++) {
                if (i != indexOfLargestPath) {
                    polygon.removePath(i);
                }
            }
        }
        return polygon;
    }
}
