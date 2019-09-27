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
package com.graphhopper.routing;

import com.graphhopper.routing.profiles.BooleanEncodedValue;
import com.graphhopper.routing.profiles.SimpleBooleanEncodedValue;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.SPTEntry;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;

public class AStarBidirectionConditional extends AStarBidirection {
    private EdgeFilter conditionalFilter;

    public AStarBidirectionConditional(Graph graph, Weighting weighting, TraversalMode tMode) {
        super(graph, weighting, tMode);
        setConditionalFilter(DefaultEdgeFilter.allEdges(flagEncoder.getBooleanEncodedValue("car-conditional_access")));
    }

    @Override
    protected void initCollections(int size) {
        super.initCollections(Math.min(size, 2000));
    }

    @Override
    protected boolean finished() {
        // we need to finish BOTH searches as one might be paused
        if (finishedFrom && finishedTo)
            return true;

        return currFrom.weight + currTo.weight >= bestWeight;
    }

    public RoutingAlgorithm setConditionalFilter(EdgeFilter conditionalFilter) {
        this.conditionalFilter = conditionalFilter;
        return this;
    }

    @Override
    protected boolean bwdSearchCanBeStopped() {
        SPTEntry currEdge = currTo;
        EdgeExplorer explorer = inEdgeExplorer;
        EdgeIterator iter = explorer.setBaseNode(currEdge.adjNode);
        // check for virtual edges
        if(iter instanceof VirtualEdgeIterator)
            return false;
        return isConditional(iter);
    }

    private boolean isConditional(EdgeIteratorState edge) {
        return conditionalFilter.accept(edge);
    }

    @Override
    public String getName() {
        return "astarbi|conditional";
    }

    @Override
    public String toString() {
        return getName() + "|" + weighting;
    }
}
