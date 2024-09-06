package com.github.hugowschneider.cyarangodb.internal.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;

import com.arangodb.ArangoDatabase;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.util.RawJson;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Adapts ArangoDB data to Cytoscape networks.
 */
public class ArangoNetworkAdapter {
    /**
     * Represents a path consisting of edges and nodes.
     */
    private static class Path {
        /**
         * The list of edges in the path.
         */
        private List<BaseEdgeDocument> edges;

        /**
         * The list of nodes in the path.
         */
        private List<BaseDocument> nodes;

        /**
         * Constructs a new Path.
         */
        public Path() {
            this.edges = new ArrayList<>();
            this.nodes = new ArrayList<>();
        }

        /**
         * Gets the list of edges in the path.
         *
         * @return the list of edges
         */
        public List<BaseEdgeDocument> getEdges() {
            return edges;
        }

        /**
         * Gets the list of nodes in the path.
         *
         * @return the list of nodes
         */
        public List<BaseDocument> getNodes() {
            return nodes;
        }
    }

    /**
     * Factory for creating Cytoscape networks.
     */
    private CyNetworkFactory networkFactory;

    /**
     * The ArangoDB database instance.
     */
    private ArangoDatabase database;

    /**
     * Map of loaded nodes by their IDs.
     */
    private Map<String, BaseDocument> loadedNodes;

    /**
     * Map of Cytoscape nodes by their IDs.
     */
    private Map<String, CyNode> nodes;

    /**
     * ObjectMapper for JSON processing.
     */
    private ObjectMapper mapper;

    /**
     * List of edge IDs.
     */
    private List<String> edges;

    /**
     * The Cytoscape network.
     */
    private CyNetwork network;

    /**
     * Gson instance for JSON processing.
     */
    private Gson gson;

    /**
     * Constructs a new ArangoNetworkAdapter.
     *
     * @param database       the ArangoDB database instance
     * @param networkFactory the factory for creating Cytoscape networks
     */
    public ArangoNetworkAdapter(ArangoDatabase database, CyNetworkFactory networkFactory) {
        this.mapper = new ObjectMapper();
        this.loadedNodes = new HashMap<>();
        this.nodes = new HashMap<>();
        this.edges = new ArrayList<>();
        this.networkFactory = networkFactory;
        this.database = database;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Deserializes a JSON string to an ArangoNetworkMetadata object.
     *
     * @param metadata the JSON string
     * @return the ArangoNetworkMetadata object
     */
    public ArangoNetworkMetadata deserialArangoNetworkMetadata(String metadata) {
        return gson.fromJson(metadata, ArangoNetworkMetadata.class);
    }

    /**
     * Retrieves a node from the database or returns a cached node if it has already
     * been loaded.
     *
     * @param id the ID of the node
     * @return the BaseDocument representing the node
     */
    public BaseDocument getOrRetriveNode(String id) {
        if (this.loadedNodes.get(id) != null) {
            return this.loadedNodes.get(id);
        } else {
            String[] parts = id.split("/");
            String collection = parts[0];
            String key = parts[1];

            BaseDocument document = database.collection(collection).getDocument(key, BaseDocument.class);
            this.loadedNodes.put(document.getId(), document);
            return document;
        }
    }

    /**
     * Retrieves or creates a Cytoscape node for the given ID.
     *
     * @param id      the ID of the node
     * @param network the Cytoscape network
     * @return the Cytoscape node
     */
    public CyNode getOrCreateCyNode(String id, CyNetwork network) {
        if (nodes.get(id) != null) {
            return nodes.get(id);
        } else {
            CyNode node = network.addNode();

            BaseDocument doc = this.loadedNodes.get(id);
            String collection = id.split("/")[0];

            nodes.put(id, node);

            CyTable table = network.getDefaultNodeTable();
            CyRow row = table.getRow(node.getSUID());
            row.set(Constants.NodeColumns.ID, doc.getId());
            row.set(Constants.NodeColumns.COLLECTION, collection);
            row.set(Constants.NodeColumns.KEY, doc.getKey());
            row.set(Constants.NodeColumns.REVISION, doc.getRevision());
            row.set(Constants.NodeColumns.NAME, String.format("%1$s (%2$s)", getName(doc), collection));
            row.set(Constants.NodeColumns.COLOR, ArangoNetworkStyle.computeColorIndex(collection));
            try {
                row.set(Constants.NodeColumns.DATA,
                        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc));
            } catch (JsonProcessingException e) {
                row.set(Constants.NodeColumns.DATA,
                        String.format("Error reading node Data: %1$s", e.getMessage()));
            }

            return node;
        }
    }

    /**
     * Import a list of RawJson documents representing edges to a Cytoscape network.
     *
     * @param docs the list of RawJson documents
     * @return the Cytoscape network
     */
    public CyNetwork importEdges(List<RawJson> docs, ArangoNetworkMetadata metadata) {
        List<BaseEdgeDocument> edges = jsonToEdges(docs);
        return importNetwork(edges, metadata);
    }

    /**
     * Import a list of RawJson documents representing paths to a Cytoscape network.
     *
     * @param docs the list of RawJson documents
     * @return the Cytoscape network
     */
    public CyNetwork importPaths(List<RawJson> docs, ArangoNetworkMetadata metadata) {
        Path path = jsonToPath(docs);
        path.getNodes().forEach((vertex) -> {
            loadedNodes.put(vertex.getId(), vertex);
        });
        return importNetwork(path.getEdges(), metadata);
    }

    /**
     * Expands a network with a list of paths.
     *
     * @param docs   the list of RawJson documents representing paths
     * @param nodeId the ID of the node to expand from
     * @return the list of new Cytoscape nodes
     */
    public List<CyNode> expandNodeWithPath(List<RawJson> docs, ArangoNetworkMetadata.NodeExpansionMetadata metadata) {
        Path path = jsonToPath(docs);
        path.getNodes().forEach((vertex) -> {
            if (!loadedNodes.containsKey(vertex.getId())) {
                loadedNodes.put(vertex.getId(), vertex);
            }
        });
        return expandNode(path.getEdges(), metadata);
    }

    /**
     * Expands a network with a list of edges.
     *
     * @param docs   the list of RawJson documents representing edges
     * @param nodeId the ID of the node to expand from
     * @return the list of new Cytoscape nodes
     */
    public List<CyNode> expandNodeWithEdges(List<RawJson> docs, ArangoNetworkMetadata.NodeExpansionMetadata metadata) {
        List<BaseEdgeDocument> edges = jsonToEdges(docs);
        return expandNode(edges, metadata);
    }

    /**
     * Gets the name of a document.
     *
     * @param doc the document
     * @return the name of the document
     */
    private String getName(BaseDocument doc) {
        for (String key : doc.getProperties().keySet()) {
            if (key.equalsIgnoreCase(Constants.NodeColumns.NAME)) {
                return (String) doc.getAttribute(key);
            }
        }
        return doc.getKey();
    }

    /**
     * Converts a list of RawJson documents to a list of BaseEdgeDocument objects.
     *
     * @param docs the list of RawJson documents
     * @return the list of BaseEdgeDocument objects
     */
    private List<BaseEdgeDocument> jsonToEdges(List<RawJson> docs) {
        return docs.stream().map(doc -> {
            try {
                return mapper.readValue(doc.get(), BaseEdgeDocument.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse document", e);
            }
        }).collect(Collectors.toList());
    }

    /**
     * Converts a list of RawJson documents to a Path object.
     *
     * @param docs the list of RawJson documents
     * @return the Path object
     */
    private Path jsonToPath(List<RawJson> docs) {
        Path path = new Path();

        docs.forEach(doc -> {
            try {
                // Parse RawJson string into a JSON object
                JsonNode jsonNode = mapper.readTree(doc.get());

                // Extract edges and vertices fields
                JsonNode edgesNode = jsonNode.get("edges");
                JsonNode verticesNode = jsonNode.get("vertices");

                // Deserialize edges
                List<BaseEdgeDocument> edges = mapper.readValue(
                        edgesNode.toString(),
                        new TypeReference<List<BaseEdgeDocument>>() {
                        });
                path.getEdges().addAll(edges);

                // Deserialize vertices
                List<BaseDocument> vertices = mapper.readValue(
                        verticesNode.toString(),
                        new TypeReference<List<BaseDocument>>() {
                        });
                path.getNodes().addAll(vertices);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return path;
    }

    /**
     * Adapts a list of BaseEdgeDocument objects to a Cytoscape network.
     *
     * @param edges the list of BaseEdgeDocument objects
     * @return the Cytoscape network
     */
    private CyNetwork importNetwork(Iterable<BaseEdgeDocument> edges, ArangoNetworkMetadata metadata) {
        network = networkFactory.createNetwork();
        CyTable cyNodeTable = network.getDefaultNodeTable();
        CyTable cyEdgeTable = network.getDefaultEdgeTable();
        CyTable cyNetworkTable = network.getDefaultNetworkTable();

        cyNodeTable.createColumn(Constants.NodeColumns.ID, String.class, true);
        cyNodeTable.createColumn(Constants.NodeColumns.COLLECTION, String.class, true);
        cyNodeTable.createColumn(Constants.NodeColumns.KEY, String.class, true);
        cyNodeTable.createColumn(Constants.NodeColumns.DATA, String.class, true);
        cyNodeTable.createColumn(Constants.NodeColumns.REVISION, String.class, true);
        cyNodeTable.createColumn(Constants.NodeColumns.COLOR, Integer.class, true);

        cyEdgeTable.createColumn(Constants.EdgeColumns.ID, String.class, true);
        cyEdgeTable.createColumn(Constants.EdgeColumns.COLLECTION, String.class, true);
        cyEdgeTable.createColumn(Constants.EdgeColumns.TO, String.class, true);
        cyEdgeTable.createColumn(Constants.EdgeColumns.FROM, String.class, true);
        cyEdgeTable.createColumn(Constants.EdgeColumns.DATA, String.class, true);
        cyEdgeTable.createColumn(Constants.EdgeColumns.REVISION, String.class, true);
        cyEdgeTable.createColumn(Constants.EdgeColumns.COLOR, Integer.class, true);
        cyEdgeTable.createColumn(Constants.NodeColumns.KEY, String.class, true);

        cyNetworkTable.createColumn(Constants.NetworkColumns.ID, String.class, true);
        cyNetworkTable.createColumn(Constants.NetworkColumns.ARANGO_NETWORK_METADATA, String.class, false);

        CyRow networkRow = cyNetworkTable.getRow(network.getSUID());
        networkRow.set(Constants.NetworkColumns.ARANGO_NETWORK_METADATA, gson.toJson(metadata));

        this.loadedNodes.values().forEach((doc) -> {
            getOrCreateCyNode(doc.getId(), network);
        });

        edges.forEach((edge) -> {
            if (this.edges.contains(edge.getId())) {
                return;
            }

            this.edges.add(edge.getId());

            BaseDocument to = getOrRetriveNode(edge.getTo());
            BaseDocument from = getOrRetriveNode(edge.getFrom());

            CyNode toNode = getOrCreateCyNode(to.getId(), network);
            CyNode fromNode = getOrCreateCyNode(from.getId(), network);

            CyEdge cyEdge = network.addEdge(toNode, fromNode, true);
            String collection = edge.getId().split("/")[0];
            CyRow row = cyEdgeTable.getRow(cyEdge.getSUID());
            addEdgeAttributes(edge, collection, row);
        });

        return network;
    }

    /**
     * Adds attributes to a Cytoscape edge.
     *
     * @param edge       the BaseEdgeDocument representing the edge
     * @param collection the collection name
     * @param row        the Cytoscape row to add attributes to
     */
    private void addEdgeAttributes(BaseEdgeDocument edge, String collection, CyRow row) {
        row.set(Constants.EdgeColumns.ID, edge.getId());
        row.set(Constants.EdgeColumns.COLLECTION, collection);
        row.set(Constants.EdgeColumns.KEY, edge.getKey());
        row.set(Constants.EdgeColumns.TO, edge.getTo());
        row.set(Constants.EdgeColumns.FROM, edge.getFrom());
        row.set(Constants.EdgeColumns.NAME, String.format("%1$s (%2$s)", getName(edge), collection));
        try {
            row.set(Constants.EdgeColumns.DATA, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(edge));
        } catch (JsonProcessingException e) {
            row.set(Constants.EdgeColumns.DATA, String.format("Error reading edge Data: %1$s", e.getMessage()));
        }
        row.set(Constants.EdgeColumns.REVISION, edge.getRevision());
        row.set(Constants.EdgeColumns.COLOR, ArangoNetworkStyle.computeColorIndex(collection));
    }

    /**
     * Expands a network with a list of edges.
     *
     * @param edges  the list of BaseEdgeDocument objects
     * @param nodeId the ID of the node to expand from
     * @return the list of new Cytoscape nodes
     */
    private List<CyNode> expandNode(List<BaseEdgeDocument> edges,
            ArangoNetworkMetadata.NodeExpansionMetadata metadata) {
        List<CyNode> newNodes = new ArrayList<>();

        this.loadedNodes.values().forEach((doc) -> {
            getOrCreateCyNode(doc.getId(), network);
        });
        CyTable cyEdgeTable = network.getDefaultEdgeTable();
        edges.forEach((edge) -> {
            if (this.edges.contains(edge.getId())) {
                return;
            }

            this.edges.add(edge.getId());

            BaseDocument to = getOrRetriveNode(edge.getTo());
            BaseDocument from = getOrRetriveNode(edge.getFrom());

            boolean addTo = nodes.get(to.getId()) == null;
            boolean addFrom = nodes.get(from.getId()) == null;

            CyNode toNode = getOrCreateCyNode(to.getId(), network);
            CyNode fromNode = getOrCreateCyNode(from.getId(), network);
            if (addTo) {
                newNodes.add(toNode);
            }
            if (addFrom) {
                newNodes.add(fromNode);
            }

            CyEdge cyEdge = network.addEdge(toNode, fromNode, true);
            String collection = edge.getId().split("/")[0];
            CyRow row = cyEdgeTable.getRow(cyEdge.getSUID());
            addEdgeAttributes(edge, collection, row);
        });

        ArangoNetworkMetadata networkMetadata = deserialArangoNetworkMetadata(
                network.getDefaultNetworkTable().getRow(network.getSUID())
                        .get(Constants.NetworkColumns.ARANGO_NETWORK_METADATA, String.class));

        networkMetadata.addNodeExpansion(metadata);

        return newNodes;
    }
}