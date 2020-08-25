package com.graphhopper.search;

import com.graphhopper.storage.Directory;

import java.util.HashMap;
import java.util.Map;

public class ConditionalIndex extends NameIndex {
    Map<String, Long> values = new HashMap<>();
    String cachedName = new String();
    Long cachedIndex;

    @Override
    public long put(String name) {
        // microoptimization to minimize the number of hashmap queries
        if (cachedName.equals(name))
            return cachedIndex;

        cachedName = name;
        cachedIndex = values.get(name);

        if (cachedIndex == null) {
            cachedIndex = super.put(name);
            values.put(name, cachedIndex);
        }

        return cachedIndex;
    }

    public ConditionalIndex(Directory dir, String filename) {
        super(dir, filename);
    }

}
