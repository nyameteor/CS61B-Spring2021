package gitlet;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Represents the gitlet index.
 */
public class Index implements Dumpable {
    static class Node implements Serializable {
        final String name;
        final String id;
        Map<String, Node> childMap;

        public Node(String name, String id, Map<String, Node> childMap) {
            this.name = name;
            this.id = id;
            this.childMap = childMap;
        }

        public Node(String name, String id) {
            this.name = name;
            this.id = id;
            this.childMap = null;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "name='" + name + '\'' +
                    ", id='" + id + '\'' +
                    ", childMap=" + childMap +
                    '}';
        }
    }

    final Node root;

    private static final String ROOT_NODE_NAME = ".";

    public Index() {
        this.root = new Node(ROOT_NODE_NAME, null, new TreeMap<>());
    }

    @Override
    public String toString() {
        return "Index{" +
                "root=" + root +
                '}';
    }

    @Override
    public void dump() {
        System.out.println(this);
    }

    /**
     * Add a leaf node with pathParts, will create its parent node if not created.
     */
    void addLeaf(List<String> parts, String id) {
        addLeaf(this.root, parts, id);
    }

    /**
     * Remove a leaf node with pathParts, will remove its parent node if it has no child.
     */
    void removeLeaf(List<String> parts) {
        removeLeaf(this.root, null, parts);
    }

    /**
     * Get a leaf node with pathParts.
     */
    Node getLeaf(List<String> parts) {
        return getLeaf(this.root, parts);
    }

    private Node getLeaf(Node node, List<String> parts) {
        if (isLeaf(node) && parts.size() == 1) {
            if (node.name.equals(parts.get(0))) {
                return node;
            }
        }
        for (String key : node.childMap.keySet()) {
            Node leaf = getLeaf(node.childMap.get(key), parts.subList(1, parts.size()));
            if (Objects.nonNull(leaf)) {
                return leaf;
            }
        }
        return null;
    }

    private void addLeaf(Node node, List<String> parts, String id) {
        if (parts.size() == 1) {
            String filename = parts.get(0);
            // Leaf node represents a blob object, it has id for comparing.
            node.childMap.put(filename, new Node(filename, id));
        } else {
            // Other node represents a tree object
            String dirname = parts.get(0);
            if (!node.childMap.containsKey(dirname)) {
                node.childMap.put(dirname, new Node(dirname, null, new TreeMap<>()));
            }
            addLeaf(node.childMap.get(dirname), parts.subList(1, parts.size()), id);
        }
    }

    private void removeLeaf(Node parentNode, Node grandparentNode, List<String> parts) {
        if (parts.size() == 1) {
            String filename = parts.get(0);
            if (parentNode.childMap.containsKey(filename)) {
                Node node = parentNode.childMap.get(filename);
                if (isLeaf(node)) {
                    parentNode.childMap.remove(filename);
                }
                return;
            }
        }
        String dirname = parts.get(0);
        if (parentNode.childMap.containsKey(dirname)) {
            removeLeaf(parentNode.childMap.get(dirname), parentNode,
                    parts.subList(1, parts.size()));
        }
        if (parentNode.childMap.size() == 0 && !isRoot(parentNode)) {
            grandparentNode.childMap.remove(parentNode.name);
        }
    }

    static boolean isLeaf(Node node) {
        return Objects.isNull(node.childMap);
    }

    static boolean isRoot(Node node) {
        return ROOT_NODE_NAME.equals(node.name);
    }
}
