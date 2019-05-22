package scl.oms.outagemap;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;

/**
 * This class provides a very basic tool to re-project points and envelopes. 
 * This class was created because the publicly available Esri geometry API does not
 * provide a projection capability.  And although the Esri Runtime SDK for Java
 * does provide this method, it does not provide publicly available convex hull
 * method, which is needed for creating supply node polygons.
 * 
 * Note: this method is only valid for approximately Seattle City Light's
 * service territory and should not be used outside that area.
 * 
 * @author jstewart
 */
public class ProjectTool {

    /**
     * An available projection, NAD_1983_HARN_StatePlane_Washington_North_FIPS_4601_Feet 
     */
    public static final int WSP83_WKID = 2926; 

    /**
     * An available coordinate system, GCS_WGS_1984 {degree = 0.0174532925199433}
     */
    public static final int WGS84_WKID = 4326; 

    private static final Point centerPoint2926 = new Point(1272182.854492180000, 226606.453125000000);
    private static final Point centerPoint4326 = new Point(-122.325972345344, 47.611394642960);

    /**
     * Re-projects a point.
     * 
     * NOTE: this method is only valid for approximately Seattle City Light's
     * service territory and should not be used outside that area.
     * 
     * @param point
     * @param wkidIn
     * @param wkidOut
     * @return a re-projected point
     */
    public static Point project(Point point, int wkidIn, int wkidOut) {
        if (wkidIn == wkidOut) {
            return point;
        }
        if ((wkidIn == WSP83_WKID) && (wkidOut == WGS84_WKID)) {
            return ProjectTool.projectWSP83_WKIDtoWGS84_WKID(point);
        } else if ((wkidIn == WGS84_WKID) && (wkidOut == WSP83_WKID)) {
            return ProjectTool.projectWGS84_WKIDtoWSP83_WKID(point);
        } else {
            throw new Error("Unknown wkid specified by .project method.");
        }
    }

    /**
     * Re-projects an envelope.
     * 
     * @param envelope
     * @param wkidIn
     * @param wkidOut
     * @return a re-projected envelope
     */
    public static Envelope project(Envelope envelope, int wkidIn, int wkidOut) {
        if (wkidIn == wkidOut) {
            return envelope;
        }
        if ((wkidIn == WSP83_WKID) && (wkidOut == WGS84_WKID)) {
            Point lowerLeftPoint = ProjectTool.projectWSP83_WKIDtoWGS84_WKID(envelope.getLowerLeft());
            Point upperRightPoint = ProjectTool.projectWSP83_WKIDtoWGS84_WKID(envelope.getUpperRight());
            return new Envelope(lowerLeftPoint.getX(), lowerLeftPoint.getY(),
                    upperRightPoint.getX(), upperRightPoint.getY());
        } else if ((wkidIn == WGS84_WKID) && (wkidOut == WSP83_WKID)) {
            Point lowerLeftPoint = ProjectTool.projectWGS84_WKIDtoWSP83_WKID(envelope.getLowerLeft());
            Point upperRightPoint = ProjectTool.projectWGS84_WKIDtoWSP83_WKID(envelope.getUpperRight());
            return new Envelope(lowerLeftPoint.getX(), lowerLeftPoint.getY(),
                    upperRightPoint.getX(), upperRightPoint.getY());
        } else {
            throw new Error("Unknown wkid specified by .project method.");
        }
    }
    
    /**
     * This method re-projects a point in Washington State Plane to 
     * WGS84 for use in degree-based mapping.
     * 
     * The following method was derived from NOAA Manual NOS NGS 5
     * State Plane Coordinate System of 1983
     * James E. Stem
     * March 1990
     * 
     * The method is specific to Washington North, Zone # 4601
     * with regard to defining and computed constants, and other coefficients.
     * (re: pages 44-45 and appendix C)
     * 
     * @param point in Washington State Plane (feet)
     * @return a re-projected point in WGS84 (degrees)
     */
    private static Point projectWSP83_WKIDtoWGS84_WKID(Point point) {
        final double N_VAL_AT_BO  =  124292.3869;     // No
        final double E_VAL_AT_LO  =  500000.0;        // Eo
        final double RADIUS_AT_BO = 5729486.217;      // Ro
        final double LON_ORIGIN   = 120.8333333333;   // Lo
        final double LAT_ORIGIN   =  48.1179151437;   // Bo
        final double SIN_LAT_ORG  =   0.7445203266;   // Sin(Bo)

        final double METER2FEET = 3.2808400000;

        final double G1_COEFFICIENT =  8.993922319E-06;
        final double G2_COEFFICIENT = -7.072700000E-15;
        final double G3_COEFFICIENT = -3.673840000E-20;
        final double G4_COEFFICIENT = -1.470500000E-27;
        
        double northing = point.getY(); 
        double easting  = point.getX();
        
        double northingPrime = (northing/METER2FEET) - N_VAL_AT_BO;
        double eastingPrime  = (easting/METER2FEET) - E_VAL_AT_LO;
        double radiusPrime   = RADIUS_AT_BO - northingPrime;
        double gamma         = Math.toDegrees(Math.atan(eastingPrime/radiusPrime));
        double lambda        = LON_ORIGIN - gamma/(Math.sin(Math.toRadians(LAT_ORIGIN)));
               
        double upsilon  = northingPrime - eastingPrime * (Math.tan(Math.toRadians(gamma/2.0)));
        double deltaPhi = upsilon * (G1_COEFFICIENT + upsilon * (G2_COEFFICIENT + upsilon * (G3_COEFFICIENT + upsilon * (G4_COEFFICIENT))));
        
        double omega = LAT_ORIGIN + deltaPhi;
        
        /*
        Note: longitude is returned as a negative (west) value
        */
        Point projectedPoint = new Point(-lambda, omega);
        return projectedPoint;
    }

    /**
     * This method re-projects a point in WGS84 to Washington State Plane
     * for use in degree-based mapping.
     * 
     * The following method was derived from NOAA Manual NOS NGS 5
     * State Plane Coordinate System of 1983
     * James E. Stem
     * March 1990
     * 
     * The method is specific to Washington North, Zone # 4601
     * with regard to defining and computed constants, and other coefficients.
     * (re: pages 44-45 and appendix C)
     * 
     * @param point in WGS84 (degrees)
     * @return a re-projected point in Washington State Plane (feet)
     */
    private static Point projectWGS84_WKIDtoWSP83_WKID(Point point) {
        final double LAT_ORIGIN   =  48.1179151437;   // Bo
        final double RADIUS_AT_BO = 5729486.217;      // Ro
        final double LON_ORIGIN   = 120.8333333333;   // Lo
        final double SIN_LAT_ORG  =   0.7445203266;   // Sin(Bo)
        final double E_VAL_AT_LO  =  500000.0;        // Eo
        final double N_VAL_AT_BO  =  124292.3869;     // No
        
        final double METER2FEET = 3.2808400000;
        
        final double L1_COEFFICIENT = 111186.19440;
        final double L2_COEFFICIENT =      9.72145;
        final double L3_COEFFICIENT =      5.61785;
        final double L4_COEFFICIENT =      0.02763;
        
        double deltaPhi = point.getY() - LAT_ORIGIN;
        double upsilon = deltaPhi*(L1_COEFFICIENT + deltaPhi * (L2_COEFFICIENT + deltaPhi * (L3_COEFFICIENT + deltaPhi * (L4_COEFFICIENT))));
        
        double radius = RADIUS_AT_BO - upsilon;
        
        /*
        Note: longitude values are stripped of sign, and assumed to be westerly
        */
        double gamma = (LON_ORIGIN - Math.abs(point.getX())) * SIN_LAT_ORG;
        
        double eastingPrime = radius * Math.sin(Math.toRadians(gamma));
        double northingPrime = upsilon + eastingPrime * Math.tan(Math.toRadians(gamma/2.0));
        
        double easting = (eastingPrime + E_VAL_AT_LO) * METER2FEET;
        double northing = (northingPrime + N_VAL_AT_BO) * METER2FEET;     
        
        Point projectedPoint = new Point(easting, northing);
        return projectedPoint;
    }
    
    public static void main(String[] args) {
        
        /* The following tests the functionality of converting from 
           Washington State Plane coordinates to WGS84 degrees, and
           then back again.
        */
        
        Point inputPoint = new Point();
        Point outputPoint = new Point();
        
        inputPoint.setXY(1257035.46981118, 287618.34930072);
        outputPoint = ProjectTool.projectWSP83_WKIDtoWGS84_WKID(inputPoint);
        System.out.println("Point " + inputPoint.toString() + ", projected to " + outputPoint.toString());
        inputPoint = ProjectTool.projectWGS84_WKIDtoWSP83_WKID(outputPoint);
        System.out.println("Point " + outputPoint.toString() + ", projected to " + inputPoint.toString());
        
        inputPoint.setXY(1245565.87367640, 245576.96206835);
        outputPoint = ProjectTool.projectWSP83_WKIDtoWGS84_WKID(inputPoint);
        System.out.println("Point " + inputPoint.toString() + ", projected to " + outputPoint.toString());
        inputPoint = ProjectTool.projectWGS84_WKIDtoWSP83_WKID(outputPoint);
        System.out.println("Point " + outputPoint.toString() + ", projected to " + inputPoint.toString());
        
        inputPoint.setXY(1297821.26748053, 179360.69715544);
        outputPoint = ProjectTool.projectWSP83_WKIDtoWGS84_WKID(inputPoint);
        System.out.println("Point " + inputPoint.toString() + ", projected to " + outputPoint.toString());
        inputPoint = ProjectTool.projectWGS84_WKIDtoWSP83_WKID(outputPoint);
        System.out.println("Point " + outputPoint.toString() + ", projected to " + inputPoint.toString());
        
        inputPoint.setXY(1262630.39152590, 166156.14787915);
        outputPoint = ProjectTool.projectWSP83_WKIDtoWGS84_WKID(inputPoint);
        System.out.println("Point " + inputPoint.toString() + ", projected to " + outputPoint.toString());
        inputPoint = ProjectTool.projectWGS84_WKIDtoWSP83_WKID(outputPoint);
        System.out.println("Point " + outputPoint.toString() + ", projected to " + inputPoint.toString());

    }
}
