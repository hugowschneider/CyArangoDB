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

public class ArangoNetworkAdapter {
    private CyNetworkFactory networkFactory;
    private ArangoDatabase database;
    private Map<String, BaseDocument> loadedNodes;
    private Map<String, CyNode> nodes;
    private ObjectMapper mapper;
    private List<String> edges;
    private CyNetwork network;

    public ArangoNetworkAdapter(ArangoDatabase database, CyNetworkFactory networkFactory) {
        this.mapper = new ObjectMapper();
        this.loadedNodes = new HashMap<>();
        this.nodes = new HashMap<>();
        this.edges = new ArrayList<>();
        this.networkFactory = networkFactory;
    }

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

    private String getName(BaseDocument doc) {

        if (doc.getAttribute("Name") != null) {
            return (String) doc.getAttribute("Name");
        }
        if (doc.getAttribute("name") != null) {
            return (String) doc.getAttribute("name");
        }

        return doc.getKey();

    }

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
            row.set("Id", doc.getId());
            row.set("Collection", collection);
            row.set("Key", doc.getKey());
            row.set("Revision", doc.getRevision());
            row.set("name", String.format("%1$s (%2$s)", getName(doc), collection));
            row.set("Color", ArangoNetworkStyle.computeColorIndex(collection));
            try {
                row.set("Data",
                        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc));
            } catch (JsonProcessingException e) {
                row.set("Data",
                        String.format("Error reading node Data: %1$s", e.getMessage()));
            }

            return node;
        }

    }

    public CyNetwork adaptEdges(List<RawJson> docs) {

        List<BaseEdgeDocument> edges = jsonToEdges(docs);

        return adapt(edges);

    }

    private List<BaseEdgeDocument> jsonToEdges(List<RawJson> docs) {
        List<BaseEdgeDocument> edges = docs.stream().map(doc -> {
            try {
                return mapper.readValue(doc.get(), BaseEdgeDocument.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse document", e);
            }
        }).collect(Collectors.toList());
        return edges;
    }

    private static class Path {
        private List<BaseEdgeDocument> edges;
        private List<BaseDocument> nodes;

        public Path() {
            this.edges = new ArrayList<>();
            this.nodes = new ArrayList<>();
        }

        public List<BaseEdgeDocument> getEdges() {
            return edges;
        }

        public List<BaseDocument> getNodes() {
            return nodes;
        }

    }

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

    public CyNetwork adaptPaths(List<RawJson> docs) {
        Path path = jsonToPath(docs);
        path.getNodes().forEach((vertex) -> {
            loadedNodes.put(vertex.getId(), vertex);
        });
        return adapt(path.getEdges());
    }

    private CyNetwork adapt(Iterable<BaseEdgeDocument> edges) {
        network = networkFactory.createNetwork();
        CyTable cyNodeTable = network.getDefaultNodeTable();
        CyTable cyEdgeTable = network.getDefaultEdgeTable();

        cyNodeTable.createColumn("Id", String.class, true);
        cyNodeTable.createColumn("Collection", String.class, true);
        cyNodeTable.createColumn("Key", String.class, true);
        cyNodeTable.createColumn("Data", String.class, true);
        cyNodeTable.createColumn("Revision", String.class, true);
        cyNodeTable.createColumn("Color", Integer.class, true);

        cyEdgeTable.createColumn("Id", String.class, true);
        cyEdgeTable.createColumn("Collection", String.class, true);
        cyEdgeTable.createColumn("To", String.class, true);
        cyEdgeTable.createColumn("From", String.class, true);
        cyEdgeTable.createColumn("Data", String.class, true);
        cyEdgeTable.createColumn("Revision", String.class, true);
        cyEdgeTable.createColumn("Color", Integer.class, true);

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
            row.set("name", String.format("%1$s (%2$s)", getName(edge), collection));
            row.set("Id", edge.getId());
            row.set("Collection", collection);
            row.set("To", edge.getTo());
            row.set("From", edge.getFrom());
            try {
                row.set("Data", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(edge));
            } catch (JsonProcessingException e) {
                row.set("Data", String.format("Error reading edge Data: %1$s", e.getMessage()));
            }
            row.set("Revision", edge.getRevision());
            row.set("Color", ArangoNetworkStyle.computeColorIndex(collection));

        });

        return network;
    }

    public List<CyNode> expandWithPath(List<RawJson> docs, String nodeId) {
        Path path = jsonToPath(docs);
        path.getNodes().forEach((vertex) -> {
            if (!loadedNodes.containsKey(vertex.getId())) {
                loadedNodes.put(vertex.getId(), vertex);
            }
        });
        return expand(path.getEdges(), nodeId);
    }

    public List<CyNode> expandWithEdges(List<RawJson> docs, String nodeId) {
        List<BaseEdgeDocument> edges = jsonToEdges(docs);
        return expand(edges, nodeId);
    }

    private List<CyNode> expand(List<BaseEdgeDocument> edges, String nodeId) {
        List<CyNode> newNodes = new ArrayList<>();
        newNodes.add(nodes.get(nodeId));
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

            boolean addTo = nodes.get(to.getId()) != null;
            boolean addFrom = nodes.get(from.getId()) != null;

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
            row.set("name", String.format("%1$s (%2$s)", getName(edge), collection));
            row.set("Id", edge.getId());
            row.set("Collection", collection);
            row.set("To", edge.getTo());
            row.set("From", edge.getFrom());
            try {
                row.set("Data", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(edge));
            } catch (JsonProcessingException e) {
                row.set("Data", String.format("Error reading edge Data: %1$s", e.getMessage()));
            }
            row.set("Revision", edge.getRevision());
            row.set("Color", ArangoNetworkStyle.computeColorIndex(collection));

        });

        return newNodes;
    }
}
