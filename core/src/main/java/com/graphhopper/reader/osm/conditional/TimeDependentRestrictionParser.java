package com.graphhopper.reader.osm.conditional;

import ch.poole.conditionalrestrictionparser.Condition;
import ch.poole.conditionalrestrictionparser.ConditionalRestrictionParser;
import ch.poole.conditionalrestrictionparser.Restriction;
import ch.poole.openinghoursparser.OpeningHoursParser;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeDependentRestrictionParser {
    Map<String, List<ParsedRestriction>> cachedConditionals = new HashMap();

    public List<ParsedRestriction> parse(String conditional) {
        List<ParsedRestriction> parsedRestrictions;
        if (cachedConditionals.containsKey(conditional))
            parsedRestrictions = cachedConditionals.get(conditional);
        else {
            parsedRestrictions = parseConditional(conditional);
            cachedConditionals.put(conditional, parsedRestrictions);
        }
        return parsedRestrictions;
    }

    private List<ParsedRestriction> parseConditional(String conditional) {
        List<ParsedRestriction> parsedRestrictions;

        try {
            ConditionalRestrictionParser crparser = new ConditionalRestrictionParser(new ByteArrayInputStream(conditional.getBytes()));
            ArrayList<Restriction> restrictions = crparser.restrictions();
            parsedRestrictions = new ArrayList<>();

            for (Restriction restriction : restrictions) {
                List<ParsedCondition> parsedConditions = parseRestriction( restriction);

                if (!parsedConditions.isEmpty())
                    parsedRestrictions.add(new ParsedRestriction(restriction.getValue(), parsedConditions));
            }
        } catch (ch.poole.conditionalrestrictionparser.ParseException e) {
            parsedRestrictions = new ArrayList<>(0);
        }
        return parsedRestrictions;
    }

    private List<ParsedCondition> parseRestriction(Restriction restriction) {
        List<ParsedCondition> parsedConditions = new ArrayList<>();

        for (Condition condition : restriction.getConditions()) {
            if (condition.isOpeningHours()) {
                try {
                    OpeningHoursParser ohparser = new OpeningHoursParser(new ByteArrayInputStream(condition.toString().getBytes()));
                    parsedConditions.add(new ParsedCondition(ohparser.rules(false)));
                } catch (Exception e) {
                }
            }
        }

        return parsedConditions;
    }

}
