package com.graphhopper.storage;

import com.graphhopper.search.ConditionalIndex;

public class TimeZoneStorage implements GraphExtension {
    private static final int EF_EDGE_BYTES = 2;// memory optimization: store references to time zone ids as shorts
    protected final int EF_EDGE;

    protected DataAccess edges;
    protected int edgeEntryIndex = 0;
    protected int edgeEntryBytes;
    protected int edgeCount;

    ConditionalIndex timeZoneIds;

    private static final String name = "timezones";

    public TimeZoneStorage() {
        EF_EDGE = nextBlockEntryIndex(EF_EDGE_BYTES);
        edgeEntryBytes = edgeEntryIndex;
        edgeCount = 0;
    }

    protected final int nextBlockEntryIndex(int size) {
        int res = edgeEntryIndex;
        edgeEntryIndex += size;
        return res;
    }

    public int entries() {
        return edgeCount;
    }

    /**
     * Set the pointer to the conditional index.
     * @param edgeId    The internal id of the edge in the graph
     * @param value  time zone id
     */
    public void setValue(int edgeId, String value) {
        long conditionalRef = timeZoneIds.put(value);
        if (conditionalRef > Short.MAX_VALUE)
            throw new IllegalStateException("Time zone id storage capacity exceeded, currently limited to short pointer");

        long edgePointer = (long) edgeId * edgeEntryBytes;
        edges.ensureCapacity(edgePointer + edgeEntryBytes);
        edges.setShort(edgePointer, (short) conditionalRef);
        edgeCount++;
    }

    /**
     * Get the pointer to the conditional index.
     * @param edgeId    The internal graph id of the edger
     * @return The index pointing to the conditionals
     */

    public String getValue(int edgeId) {
        long edgePointer = (long) edgeId * edgeEntryBytes;
        return timeZoneIds.get((long) edges.getShort(edgePointer));
    }

    @Override
    public boolean isRequireNodeField() {
        return false;
    }

    @Override
    public boolean isRequireEdgeField() {
        return false;
    }

    @Override
    public int getDefaultNodeFieldValue() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public int getDefaultEdgeFieldValue() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void init(Graph graph, Directory dir) {
        if (edgeCount > 0)
            throw new AssertionError("The conditional restrictions storage must be initialized only once.");

        this.edges = dir.find(name);
        this.timeZoneIds = new ConditionalIndex(dir, "timezoneids");
    }

    @Override
    public GraphExtension create(long byteCount) {
        edges.create(byteCount * edgeEntryBytes);
        timeZoneIds.create(byteCount);
        return this;
    }

    @Override
    public boolean loadExisting() {
        if (!edges.loadExisting())
            throw new IllegalStateException("Unable to load storage '" + name + "'. Corrupt file or directory?");

        edgeEntryBytes = edges.getHeader(0);
        edgeCount = edges.getHeader(4);

        timeZoneIds.loadExisting();

        return true;
    }

    @Override
    public void setSegmentSize(int bytes) {
        edges.setSegmentSize(bytes);
        timeZoneIds.setSegmentSize(bytes);
    }

    @Override
    public void flush() {
        edges.setHeader(0, edgeEntryBytes);
        edges.setHeader(4, edgeCount);
        edges.flush();
        timeZoneIds.flush();
    }

    @Override
    public void close() {
        edges.close();
        timeZoneIds.close();
    }

    @Override
    public long getCapacity() {
        return timeZoneIds.getCapacity() + edges.getCapacity();
    }

    @Override
    public GraphExtension copyTo(GraphExtension clonedStorage) {

        if (!(clonedStorage instanceof TimeZoneStorage)) {
            throw new IllegalStateException("the extended storage to clone must be the same");
        }

        TimeZoneStorage clonedTC = (TimeZoneStorage) clonedStorage;

        edges.copyTo(clonedTC.edges);
        clonedTC.edgeCount = edgeCount;

        return clonedStorage;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

};


