package com.github.hugowschneider.cyarangodb.internal.network;

import com.arangodb.util.RawJson;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Map;

/**
 * Validates the results of a query to determine if they represent edges or paths.
 */
public class QueryResultValidator {
    private Gson gson;
    private List<RawJson> docs;
    private Boolean isEdge;
    private Boolean isPath;

    /**
     * Constructs a new QueryResultValidator instance.
     *
     * @param docs the list of RawJson documents to validate
     */
    public QueryResultValidator(List<RawJson> docs) {
        gson = new GsonBuilder().create();
        this.docs = docs;
        this.isEdge = null;
        this.isPath = null;
    }

    /**
     * Determines if the list of documents represents an edge list.
     * A list is considered an edge list if each document contains both "_from" and "_to" keys.
     *
     * @return true if the documents represent an edge list, false otherwise
     */
    public boolean isEdgeList() {
        if (isEdge != null) {
            return isEdge;
        }
        for (RawJson doc : docs) {
            Map<String, Object> jsonMap = gson.fromJson(doc.get(), new TypeToken<Map<String, Object>>() {}.getType());
            if (jsonMap.containsKey("_from") && jsonMap.containsKey("_to")) {
                return isEdge = true;
            }
        }
        return isEdge = false;
    }

    /**
     * Determines if the list of documents represents a path list.
     * A list is considered a path list if each document contains both "vertices" and "edges" keys,
     * and the "edges" list contains documents with "_from" and "_to" keys, while the "vertices" list contains documents with "_id" keys.
     *
     * @return true if the documents represent a path list, false otherwise
     */
    public boolean isPathList() {
        if (isPath != null) {
            return isPath;
        }
        for (RawJson doc : docs) {
            Map<String, Object> jsonMap = gson.fromJson(doc.get(), new TypeToken<Map<String, Object>>() {}.getType());
            if (jsonMap.containsKey("vertices") && jsonMap.containsKey("edges")) {
                Object edgesObj = jsonMap.get("edges");
                Object verticesObj = jsonMap.get("vertices");

                if (edgesObj instanceof List && verticesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> edges = (List<Map<String, Object>>) edgesObj;
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> vertices = (List<Map<String, Object>>) verticesObj;
                    if (!edges.isEmpty() && !vertices.isEmpty()) {
                        boolean hasEdges = false;
                        boolean hasNodes = false;
                        for (Map<String, Object> edge : edges) {
                            if (edge.containsKey("_from") && edge.containsKey("_to")) {
                                hasEdges = true;
                                break;
                            }
                        }
                        for (Map<String, Object> vertex : vertices) {
                            if (vertex.containsKey("_id")) {
                                hasNodes = true;
                                break;
                            }
                        }
                        return isPath = hasEdges && hasNodes;
                    }
                }
            }
        }
        return isPath = false;
    }

    /**
     * Checks if a node or edge with the specified ID is present in the documents.
     *
     * @param id the ID to check for
     * @return true if the ID is present, false otherwise
     */
    public boolean isNodeEdgePresent(String id) {
        if (this.isEdgeList()) {
            for (RawJson doc : docs) {
                Map<String, Object> jsonMap = gson.fromJson(doc.get(), new TypeToken<Map<String, Object>>() {}.getType());
                if (jsonMap.get("_from").equals(id) || jsonMap.get("_to").equals(id)) {
                    return true;
                }
            }
        } else if (this.isPathList()) {
            for (RawJson doc : docs) {
                Map<String, Object> jsonMap = gson.fromJson(doc.get(), new TypeToken<Map<String, Object>>() {}.getType());
                if (jsonMap.containsKey("vertices") && jsonMap.containsKey("edges")) {
                    Object edgesObj = jsonMap.get("edges");
                    Object verticesObj = jsonMap.get("vertices");

                    if (edgesObj instanceof List && verticesObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> edges = (List<Map<String, Object>>) edgesObj;
                        if (!edges.isEmpty()) {
                            for (Map<String, Object> edge : edges) {
                                if (edge.get("_from").equals(id) || edge.get("_to").equals(id)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }
}