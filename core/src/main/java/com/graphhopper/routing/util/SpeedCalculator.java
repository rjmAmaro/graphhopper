package com.graphhopper.routing.util;

import com.graphhopper.reader.osm.conditional.TimeDependentRestrictionParser;
import com.graphhopper.reader.osm.conditional.ParsedRestriction;
import com.graphhopper.routing.EdgeKeys;
import com.graphhopper.routing.profiles.BooleanEncodedValue;
import com.graphhopper.routing.profiles.DecimalEncodedValue;
import com.graphhopper.util.DateTimeHelper;
import com.graphhopper.storage.ConditionalEdgesMap;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.util.EdgeIteratorState;

import java.time.ZonedDateTime;
import java.util.List;

public class SpeedCalculator {
    protected final DecimalEncodedValue avSpeedEnc;

    private final static double SPEED_FACTOR = 0.9;

    // time-dependent stuff
    private final BooleanEncodedValue conditionalEnc;
    private final ConditionalEdgesMap conditionalEdges;
    private final DateTimeHelper dateTimeHelper;
    private final TimeDependentRestrictionParser timeDependentRestrictionParser = new TimeDependentRestrictionParser();

    public SpeedCalculator(GraphHopperStorage graph, FlagEncoder encoder) {
        avSpeedEnc = encoder.getAverageSpeedEnc();

        // time-dependent stuff
        EncodingManager encodingManager = graph.getEncodingManager();
        String encoderName = encodingManager.getKey(encoder, "conditional_speed");

        if (!encodingManager.hasEncodedValue(encoderName)) {
            throw new IllegalStateException("No conditional speed associated with the flag encoder");
        }

        conditionalEnc = encodingManager.getBooleanEncodedValue(encoderName);
        conditionalEdges = graph.getConditionalSpeed(encoder);

        this.dateTimeHelper = new DateTimeHelper(graph);
    }

    private double getSpeed(EdgeIteratorState edge, boolean reverse) {
        return reverse ? edge.getReverse(avSpeedEnc) : edge.get(avSpeedEnc);
    }

    public double getMaxSpeed(EdgeIteratorState edge, boolean reverse) {
        double baseSpeed = getSpeed(edge, reverse);
        double timeDependentMaxSpeed = 0;

        // retrieve time-dependent maxspeed here
        if (edge.get(conditionalEnc)) {
            int edgeId = EdgeKeys.getOriginalEdge(edge);
            String conditional = conditionalEdges.getValue(edgeId);

            List<ParsedRestriction> restrictions = timeDependentRestrictionParser.parse(conditional);

            if (restrictions!=null) {
                // iterate over restrictions starting from the last one in order to match to the most specific one
                for (int i = restrictions.size() - 1; i >= 0; i--) {
                    double value = AbstractFlagEncoder.parseSpeed(restrictions.get(i).getValue()) * SPEED_FACTOR;
                    if (value > timeDependentMaxSpeed)
                        timeDependentMaxSpeed = value;
                }
            }
        }

        return baseSpeed > timeDependentMaxSpeed ? baseSpeed : timeDependentMaxSpeed;
    }

    public double getSpeed(EdgeIteratorState edge, boolean reverse, long time) {
        double speed = getSpeed(edge, reverse);

        // retrieve time-dependent maxspeed here
        if (time != -1 && edge.get(conditionalEnc)) {
            ZonedDateTime zonedDateTime = dateTimeHelper.getZonedDateTime(edge, time);
            int edgeId = EdgeKeys.getOriginalEdge(edge);
            String value = conditionalEdges.getValue(edgeId);
            double maxSpeed = getSpeed(value, zonedDateTime);
            if (maxSpeed >= 0)
                return maxSpeed * SPEED_FACTOR;
        }

        return speed;
    }

    private double getSpeed(String conditional, ZonedDateTime zonedDateTime)  {
        List<ParsedRestriction> restrictions = timeDependentRestrictionParser.parse(conditional);

        if (restrictions!=null) {
            // iterate over restrictions starting from the last one in order to match to the most specific one
            for (int i = restrictions.size() - 1; i >= 0; i--) {
                ParsedRestriction restriction = restrictions.get(i);
                // stop as soon as time matches the combined conditions
                if (TimeDependentConditionEvaluator.match(restriction.getConditions(), zonedDateTime)) {
                    return AbstractFlagEncoder.parseSpeed(restriction.getValue());
                }
            }
        }

        return -1;
    }

}
