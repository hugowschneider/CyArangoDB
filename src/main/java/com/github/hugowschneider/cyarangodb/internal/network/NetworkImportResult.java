package com.github.hugowschneider.cyarangodb.internal.network;

/**
 * Represents the result of a network import operation, including the number of nodes and edges imported.
 */
public class NetworkImportResult {
    /**
     * The number of nodes imported.
     */
    private int nodeCount;

    /**
     * The number of edges imported.
     */
    private int edgeCount;

    /**
     * Constructs a new NetworkImportResult.
     *
     * @param nodeCount the number of nodes imported
     * @param edgeCount the number of edges imported
     */
    public NetworkImportResult(int nodeCount, int edgeCount) {
        this.nodeCount = nodeCount;
        this.edgeCount = edgeCount;
    }

    /**
     * Gets the number of nodes imported.
     *
     * @return the number of nodes imported
     */
    public int getNodeCount() {
        return nodeCount;
    }

    /**
     * Gets the number of edges imported.
     *
     * @return the number of edges imported
     */
    public int getEdgeCount() {
        return edgeCount;
    }
}