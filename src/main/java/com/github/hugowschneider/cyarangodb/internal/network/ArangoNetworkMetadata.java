package com.github.hugowschneider.cyarangodb.internal.network;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents the metadata of an ArangoDB network.
 * This class stores information about the queries used to generate the network.
 */
public class ArangoNetworkMetadata {

    /**
     * Represents the metadata of a node expansion.
     * This class stores information about the query used to expand a node.
     */
    public static class NodeExpansionMetadata {
        private String nodeId;
        private String query;
        private String connectionId;

        /**
         * Constructor.
         * 
         * @param nodeId       Node id.
         * @param query        Query used to expand the node.
         * @param connectionId Connection id.
         */
        public NodeExpansionMetadata(String nodeId, String query, String connectionId) {
            this.nodeId = nodeId;
            this.query = query;
            this.connectionId = connectionId;
        }

        /**
         * Get the node id.
         * 
         * @return Node id.
         */
        public String getNodeId() {
            return nodeId;
        }

        /**
         * Get the query used to expand the node.
         * 
         * @return Query used to expand the node.
         */
        public String getQuery() {
            return query;
        }

        /**
         * Get the connection id.
         * 
         * @return Connection id.
         */
        public String getConnectionId() {
            return connectionId;
        }
    }

    /**
     * Represents the metadata of a network expansion.
     * This class stores information about the query used to expand the network.
     */
    public static class NetworkExpansionMetadata {
        private String query;
        private String connectionId;

        /**
         * Constructor.
         * 
         * @param query        Query used to expand the node.
         * @param connectionId Connection id.
         */
        public NetworkExpansionMetadata(String query, String connectionId) {
            this.query = query;
            this.connectionId = connectionId;
        }

        /**
         * Get the query used to expand the node.
         * 
         * @return Query used to expand the node.
         */
        public String getQuery() {
            return query;
        }

        /**
         * Get the connection id.
         * 
         * @return Connection id.
         */
        public String getConnectionId() {
            return connectionId;
        }
    }

    /**
     * Query used to generate the network.
     */
    private String query;

    /**
     * Connection id used to generate the network.
     */
    private String connectionId;
    /**
     * List of node expansions.
     */
    private List<NodeExpansionMetadata> nodeExpansions;

    /**
     * List of node expansions.
     */
    private List<NetworkExpansionMetadata> networkExpansions;

    /**
     * Constructor.
     * 
     * @param query        Query used to generate the network.
     * @param connectionId Connection id used to generate the network.
     */
    public ArangoNetworkMetadata(String query, String connectionId) {
        this.query = query;
        this.connectionId = connectionId;
        nodeExpansions = new ArrayList<>();
        networkExpansions = new ArrayList<>();
    }

    /**
     * Get the query used to generate the network.
     * 
     * @return Query used to generate the network.
     */
    public String getQuery() {
        return query;
    }

    /**
     * Get the connection id used to generate the network.
     * 
     * @return Connection id used to generate the network.
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * Get the list of node expansions.
     * 
     * @return List of node expansions.
     */
    public List<NodeExpansionMetadata> getNodeExpansions() {
        return nodeExpansions;
    }

    /**
     * Get the list of network expansions.
     * 
     * @return List of network expansions.
     */
    public List<NetworkExpansionMetadata> getNetworkExpansions() {
        return networkExpansions;
    }

    /**
     * Add a node expansion to the list.
     * 
     * @param metadata Node expansion metadata.
     */
    public void addNodeExpansion(NodeExpansionMetadata metadata) {
        nodeExpansions.add(metadata);
    }

    /**
     * Add a network expansion to the list.
     * 
     * @param metadata Network expansion metadata.
     */
    public void addNetworkExpansion(NetworkExpansionMetadata metadata) {
        networkExpansions.add(metadata);
    }

}
