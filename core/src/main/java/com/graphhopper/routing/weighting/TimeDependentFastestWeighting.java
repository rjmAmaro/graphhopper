/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing.weighting;

import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.routing.util.*;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Parameters.Routing;


/**
 * Calculates the fastest route with the specified vehicle (VehicleEncoder). Calculates the weight
 * in seconds.
 * <p>
 *
 * @author Peter Karich
 */
public class TimeDependentFastestWeighting extends FastestWeighting {
    private SpeedCalculator speedCalculator;

    public TimeDependentFastestWeighting(FlagEncoder encoder, PMap map, GraphHopperStorage graph) {
        super(encoder, map);

        if (graph != null)
            init(graph);
    }

    @Override
    public void init(GraphHopperStorage graph) {
        this.speedCalculator = new SpeedCalculator(graph, this.flagEncoder);
    }

    @Override
    public double calcWeight(EdgeIteratorState edge, boolean reverse, int prevOrNextEdgeId) {
        return super.calcWeight(edge, reverse, prevOrNextEdgeId); //calcWeight(edge, speedCalculator.getMaxSpeed(edge, reverse));
    }

    @Override
    public double calcWeight(EdgeIteratorState edge, boolean reverse, int prevOrNextEdgeId, long at) {
        return calcWeight(edge, speedCalculator.getSpeed(edge, reverse, at));
    }

    private double calcWeight(EdgeIteratorState edge, double speed) {
        if (speed == 0)
            return Double.POSITIVE_INFINITY;

        double time = edge.getDistance() / speed * SPEED_CONV;

        // add direction penalties at start/stop/via points
        boolean unfavoredEdge = edge.get(EdgeIteratorState.UNFAVORED_EDGE);
        if (unfavoredEdge)
            time += headingPenalty;

        return time;
    }

    @Override
    public long calcMillis(EdgeIteratorState edge, boolean reverse, int prevOrNextEdgeId, long at) {
        double time = calcWeight(edge, reverse, prevOrNextEdgeId, at);
        if (time == Double.POSITIVE_INFINITY)
            throw new IllegalStateException("Speed cannot be 0 for unblocked edge, use access properties to mark edge blocked! " +
                    "Should only occur for shortest path calculation. See #242.");
        return (long) (time * 1000);
    }

    @Override
    public String getName() {
        return "td_fastest";
    }
}
