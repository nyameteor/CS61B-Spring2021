package bstmap;

import java.util.*;

public class BSTMap<K extends Comparable<K>, V> implements Map61B<K, V> {
    private Node node = null;
    private int size = 0;

    private class Node {
        private final K key;
        private V value;
        private Node left;
        private Node right;

        public Node(K key, V value, Node left, Node right) {
            this.key = key;
            this.value = value;
            this.left = left;
            this.right = right;
        }

        public Node(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public void setValue(V value) {
            this.value = value;
        }
    }

    V findValue(Node node, K key) {
        Node findNode = find(node, key);
        if (findNode == null) {
            return null;
        } else {
            return findNode.value;
        }
    }

    Node find(Node node, K key) {
        if (node == null) {
            return null;
        }
        int cmp = node.key.compareTo(key);
        if (cmp == 0) {
            return node;
        } else if (cmp > 0) {
            return find(node.left, key);
        } else {
            return find(node.right, key);
        }
    }

    Node insert(Node node, K key, V value) {
        if (node == null) {
            return new Node(key, value);
        }
        int cmp = node.key.compareTo(key);
        if (cmp == 0) {
            node.setValue(value);
        } else if (cmp > 0) {
            node.left = insert(node.left, key, value);
        } else {
            node.right = insert(node.right, key, value);
        }
        return node;
    }

    public BSTMap() {
    }

    @Override
    public void clear() {
        this.node = null;
        this.size = 0;
    }

    @Override
    public boolean containsKey(K key) {
        return find(this.node, key) != null;
    }

    @Override
    public V get(K key) {
        return findValue(this.node, key);
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public void put(K key, V value) {
        this.node = insert(this.node, key, value);
        this.size += 1;
    }

    @Override
    public Set<K> keySet() {
        Set<K> result = new TreeSet<>();
        for (K key : this) {
            result.add(key);
        }
        return result;
    }

    @Override
    public V remove(K key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<K> iterator() {
        return new BSTMapIterator();
    }

    private class BSTMapIterator implements Iterator<K> {
        private Node p;

        private Boolean canAddLeft;

        private Stack<Node> stack;

        public BSTMapIterator() {
            p = node;
            canAddLeft = true;
            stack = new Stack<>();
            stack.push(p);
        }

        @Override
        public boolean hasNext() {
            return !stack.empty();
        }

        @Override
        public K next() {
            if (stack.empty()) {
                throw new NoSuchElementException();
            }
            Node topNode = stack.peek();
            if (canAddLeft) {
                while (topNode.left != null) {
                    stack.push(topNode.left);
                    topNode = stack.peek();
                }
                canAddLeft = false;
            }
            topNode = stack.pop();
            if (topNode.right != null) {
                stack.push(topNode.right);
                canAddLeft = true;
            }
            return topNode.key;
        }
    }
}
