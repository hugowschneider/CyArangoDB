package com.github.hugowschneider.cyarangodb.internal.network;

public class NetworkImportResult {
    private int nodeCount;
    private int edgeCount;

    public NetworkImportResult(int nodeCount, int edgeCount) {
        this.nodeCount = nodeCount;
        this.edgeCount = edgeCount;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public int getEdgeCount() {
        return edgeCount;
    }
}
