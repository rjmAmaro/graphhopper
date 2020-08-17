package com.graphhopper.routing.util;

import com.graphhopper.reader.osm.conditional.TimeDependentRestrictionParser;
import com.graphhopper.reader.osm.conditional.ParsedCondition;
import com.graphhopper.reader.osm.conditional.ParsedRestriction;
import com.graphhopper.routing.EdgeKeys;
import com.graphhopper.routing.profiles.BooleanEncodedValue;
import com.graphhopper.util.DateTimeHelper;
import com.graphhopper.storage.ConditionalEdgesMap;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.util.EdgeIteratorState;

import java.time.ZonedDateTime;
import java.util.List;


public class ConditionalAccessEdgeFilter implements TimeDependentEdgeFilter {
    private final BooleanEncodedValue conditionalEnc;
    private final ConditionalEdgesMap conditionalEdges;
    private final boolean fwd;
    private final boolean bwd;
    private final DateTimeHelper dateTimeHelper;
    private final TimeDependentRestrictionParser timeDependentRestrictionParser = new TimeDependentRestrictionParser();

    public ConditionalAccessEdgeFilter(GraphHopperStorage graph, FlagEncoder encoder) {
        this(graph, encoder.toString());
    }

    public ConditionalAccessEdgeFilter(GraphHopperStorage graph, String encoderName) {
        this(graph, encoderName, true, true);
    }

    ConditionalAccessEdgeFilter(GraphHopperStorage graph, String encoderName, boolean fwd, boolean bwd) {
        EncodingManager encodingManager = graph.getEncodingManager();
        conditionalEnc = encodingManager.getBooleanEncodedValue(EncodingManager.getKey(encoderName, "conditional_access"));
        conditionalEdges = graph.getConditionalAccess(encoderName);
        this.fwd = fwd;
        this.bwd = bwd;
        this.dateTimeHelper = new DateTimeHelper(graph);
    }

    @Override
    public final boolean accept(EdgeIteratorState iter, long time) {
        if (fwd && iter.get(conditionalEnc) || bwd && iter.getReverse(conditionalEnc)) {
            int edgeId = EdgeKeys.getOriginalEdge(iter);
            // for now the filter is used only in the context of fwd search so only edges going out of the base node are explored
            ZonedDateTime zonedDateTime = dateTimeHelper.getZonedDateTime(iter, time);
            String value = conditionalEdges.getValue(edgeId);
            return accept(value, zonedDateTime);
        }
        return true;
    }

    boolean accept(String conditional, ZonedDateTime zonedDateTime) {
        boolean matchValue = false;

        List<ParsedRestriction> restrictions = timeDependentRestrictionParser.parse(conditional);

        if (restrictions!=null) {
            // iterate over restrictions starting from the last one in order to match to the most specific one
            for (int i = restrictions.size() - 1; i >= 0; i--) {
                ParsedRestriction restriction = restrictions.get(i);

                matchValue = "yes".equals(restriction.getValue());

                List<ParsedCondition> conditions = restriction.getConditions();

                // stop as soon as time matches the combined conditions
                if (TimeDependentConditionEvaluator.match(conditions, zonedDateTime))
                    return matchValue;
            }

            // no restrictions with matching conditions found
            return !matchValue;
        }

        return false;
    }

    public boolean acceptsBackward() {
        return bwd;
    }

    public boolean acceptsForward() {
        return fwd;
    }

    @Override
    public String toString() {
        return conditionalEnc + ", bwd:" + bwd + ", fwd:" + fwd;
    }
}
