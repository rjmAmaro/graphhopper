package com.graphhopper.reader.osm.conditional;

import java.util.List;

public class ParsedRestriction {
    public String value;
    public List<ParsedCondition> conditions;

    public ParsedRestriction(String value, List<ParsedCondition> conditions) {
        this.value = value;
        this.conditions = conditions;
    }

    public String getValue() {
        return value;
    }

    public List<ParsedCondition> getConditions() {
        return conditions;
    }
}
