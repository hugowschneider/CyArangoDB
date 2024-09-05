package com.github.hugowschneider.cyarangodb.internal;

import org.w3c.dom.Node;

/**
 * A class that holds constant values for edge and node columns.
 */
public class Constants {

    /**
     * A class that holds constant values for edge columns.
     */
    static public class EdgeColumns {
        /**
         * The ID of the edge.
         */
        public static final String ID = "Id";
        
        /**
         * The collection to which the edge belongs.
         */
        public static final String COLLECTION = "Collection";
        
        /**
         * The target node of the edge.
         */
        public static final String TO = "To";
        
        /**
         * The source node of the edge.
         */
        public static final String FROM = "From";
        
        /**
         * The data associated with the edge.
         */
        public static final String DATA = "Data";
        
        /**
         * The revision of the edge.
         */
        public static final String REVISION = "Revision";
        
        /**
         * The color of the edge.
         */
        public static final String COLOR = "Color";
        
        /**
         * The name of the edge.
         */
        public static final String NAME = "name";
        
        /**
         * The key of the edge.
         */
        public static final String KEY = "Key";

        /**
         * A class that holds constant values for edge columns.
         */
        public EdgeColumns() {
        }
    }

    /**
     * A class that holds constant values for node columns.
     */
    static public class NodeColumns {
        
        
        /**
         * The ID of the node.
         */
        public static final String ID = "Id";
        
        /**
         * The collection to which the node belongs.
         */
        public static final String COLLECTION = "Collection";
        
        /**
         * The key of the node.
         */
        public static final String KEY = "Key";
        
        /**
         * The data associated with the node.
         */
        public static final String DATA = "Data";
        
        /**
         * The revision of the node.
         */
        public static final String REVISION = "Revision";
        
        /**
         * The color of the node.
         */
        public static final String COLOR = "Color";
        
        /**
         * The name of the node.
         */
        public static final String NAME = "name";

        /**
         * A class that holds constant values for node columns.
         */
        public NodeColumns() {
        }
    }

    /**
     * A class that holds constant values for edge columns.
     */
    public Constants() {
    }
}
