package com.graphhopper.reader.osm.conditional;

import ch.poole.openinghoursparser.Rule;

import java.util.List;

public class ParsedCondition {
    private List<Rule> rules;

    public ParsedCondition(List<Rule> rules) {
        this.rules = rules;
    }

    public List<Rule> getRules() {
        return rules;
    }
}
