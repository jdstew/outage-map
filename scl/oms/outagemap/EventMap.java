package scl.oms.outagemap;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class represents an OMS/NMS event
 * 
 * @author stewarjd
 * @param <K> Integer, the EVENT_IDX from OMS/NMS
 * @param <V> SupplyNodeMap contains an event's supply nodes (i.e. transformers)
 */
public class EventMap<K, V> extends HashMap {

    private HashMap<Long, String> causeMap;
    private HashMap<Long, String> etorMap;
    private HashMap<Long, Integer> custsByEventMap;
    private HashMap<Long, String> eventBeginMap;
    private HashMap<Long, String> crewDispatchedMap;

    /*
     * Initializes the event map, whcih contains additional hash maps for the
     * outage caus, ETOR, customer count, start time, crew dispatch time.
     */
    public EventMap() {
        super();
        causeMap = new HashMap();
        etorMap = new HashMap();
        custsByEventMap = new HashMap();
        eventBeginMap = new HashMap();
        crewDispatchedMap = new HashMap();
    }

    /**
     * Adds a new record to the outage event.
     * 
     * @param eventIdx, OMS/NMS EVENT_IDX
     * @param supplyIdx, OMS/NMS SUPPLY_IDX
     * @param point, the x/y coordinate of the customer affected
     * @param cause, the cause of the outage
     * @param etor, Estimated Time of Restoral
     * @param custsByEvent, Customer count impacted
     * @param eventBeginText, The start time of the outage
     * @param crewDispatched, The time a crew was dispatched
     */
    public void addPoint(Long eventIdx, Long supplyIdx, Point point, String cause,
            String etor, int custsByEvent, String eventBeginText, String crewDispatched) throws Exception {
        if (this.containsKey(eventIdx)) {
            SupplyNodeMap supplyNodes = (SupplyNodeMap) this.get(eventIdx);
            supplyNodes.addPoint(supplyIdx, point);
        } else {
            causeMap.put(eventIdx, cause);
            etorMap.put(eventIdx, etor);
            custsByEventMap.put(eventIdx, custsByEvent);
            eventBeginMap.put(eventIdx, eventBeginText);
            crewDispatchedMap.put(eventIdx, crewDispatched);
            SupplyNodeMap<Long, PointQueue> supplyNodes = new SupplyNodeMap();
            supplyNodes.addPoint(supplyIdx, point);
            super.put(eventIdx, supplyNodes);
        }
    }

    /**
     * Computes the event's polygons from the underlying supply nodes.
     * 
     * @param eventIdx the event id (EVENT_IDX)
     * @return an array of polygons for the event
     */
    public Polygon[] getEventPolygons(Long eventIdx) {
        SupplyNodeMap supplyNodeMap = (SupplyNodeMap) this.get(eventIdx);
        return supplyNodeMap.getPolygons();
    }

    /**
     * Provides the average center of supply nodes for the outage.
     * 
     * @param eventIdx the event id (EVENT_IDX)
     * @return location for an event's label
     */
    public Point getLabelPoint(Long eventIdx) {
        SupplyNodeMap supplyNodeMap = (SupplyNodeMap) this.get(eventIdx);
        return supplyNodeMap.getLabelPoint();
    }

    /**
     * Provides the number of events.
     * 
     * @return event count
     */
    public int getEventCount() {
        return this.size();
    }

    /**
     * Provides the total count of supply nodes processed.
     * 
     * @return the total count of supply nodes processed
     */
    public int getSupplyNodeCount() {
        Iterator<Long> eventKeyItr = this.keySet().iterator();
        Long eventKey;
        int supplyNodeCount = 0;
        while (eventKeyItr.hasNext()) {
            eventKey = eventKeyItr.next();
            supplyNodeCount += ((SupplyNodeMap) this.get(eventKey)).size();
        }
        return super.size();
    }

    /**
     * Provides basic metrics for events.
     * 
     * @return metrics about the events.
     */
    public String getMetrics() {
        return ("Outage metric: " + this.size() + " events impacting " + this.getSupplyNodeCount() + " supply nodes.");
    }

    /**
     * @param eventIdx the event id (EVENT_IDX)
     * @return the cause
     */
    public String getCause(Long eventIdx) {
        return causeMap.get(eventIdx);
    }

    /**
     * @param eventIdx the event id (EVENT_IDX)
     * @return the etor
     */
    public String getEtor(Long eventIdx) {
        return etorMap.get(eventIdx);
    }

    /**
     * @param eventIdx the event id (EVENT_IDX)
     * @return the custsByEvent
     */
    public Integer getCustsByEvent(Long eventIdx) {
        return custsByEventMap.get(eventIdx);
    }

    /**
     * @param eventIdx the event id (EVENT_IDX)
     * @return the event start time
     */
    public String getEventBegin(Long eventIdx) {
        return eventBeginMap.get(eventIdx);
    }

    /**
     * Provides the appropriate value if a crew has been dispatched to the event.
     * 
     * @param eventIdx the event id (EVENT_IDX)
     * @return the crewDispatched
     */
    public String getCrewDispatched(Long eventIdx) {
        return crewDispatchedMap.get(eventIdx);
    }
}
