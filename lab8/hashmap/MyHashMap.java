package hashmap;

import java.util.*;

/**
 *  A hash table-backed Map implementation. Provides amortized constant time
 *  access to elements via get(), remove(), and put() in the best case.
 *
 *  Assumes null keys will never be inserted, and does not resize down upon remove().
 *  @author YOUR NAME HERE
 */
public class MyHashMap<K, V> implements Map61B<K, V> {

    /**
     * Protected helper class to store key/value pairs
     * The protected qualifier allows subclass access
     */
    protected class Node {
        K key;
        V value;

        Node(K k, V v) {
            key = k;
            value = v;
        }
    }

    /* Instance Variables */
    private Collection<Node>[] buckets;

    private final int initialSize;

    private final double maxLoad;

    private int size;

    /** Constructors */
    public MyHashMap() {
        this(16);
    }

    public MyHashMap(int initialSize) {
        this(initialSize, 0.75);
    }

    /**
     * MyHashMap constructor that creates a backing array of initialSize.
     * The load factor (# items / # buckets) should always be <= loadFactor
     *
     * @param initialSize initial size of backing array
     * @param maxLoad maximum load factor
     */
    public MyHashMap(int initialSize, double maxLoad) {
        this.buckets = createTable(initialSize);
        this.initialSize = initialSize;
        this.maxLoad = maxLoad;
        this.size = 0;
    }

    /**
     * Returns a new node to be placed in a hash table bucket
     */
    private Node createNode(K key, V value) {
        return new Node(key, value);
    }

    /**
     * Returns a data structure to be a hash table bucket
     *
     * The only requirements of a hash table bucket are that we can:
     *  1. Insert items (`add` method)
     *  2. Remove items (`remove` method)
     *  3. Iterate through items (`iterator` method)
     *
     * Each of these methods is supported by java.util.Collection,
     * Most data structures in Java inherit from Collection, so we
     * can use almost any data structure as our buckets.
     *
     * Override this method to use different data structures as
     * the underlying bucket type
     *
     * BE SURE TO CALL THIS FACTORY METHOD INSTEAD OF CREATING YOUR
     * OWN BUCKET DATA STRUCTURES WITH THE NEW OPERATOR!
     */
    protected Collection<Node> createBucket() {
        return new ArrayList<>();
    }

    /**
     * Returns a table to back our hash table. As per the comment
     * above, this table can be an array of Collection objects
     *
     * BE SURE TO CALL THIS FACTORY METHOD WHEN CREATING A TABLE SO
     * THAT ALL BUCKET TYPES ARE OF JAVA.UTIL.COLLECTION
     *
     * @param tableSize the size of the table to create
     */
    private Collection<Node>[] createTable(int tableSize) {
        return new Collection[tableSize];
    }

    // Compute hashCode and reduce to get index
    private int getIndex(K key) {
        return getIndex(this.buckets, key);
    }

    private int getIndex(Collection<Node>[] buckets, K key) {
        return Math.floorMod(key.hashCode(), buckets.length);
    }

    private Node putNode(Node node) {
        return putNode(this.buckets, node);
    }

    // Return the inserted node if node's key is not present, else return null
    private Node putNode(Collection<Node>[] buckets, Node node) {
        int index = getIndex(buckets, node.key);
        if (buckets[index] == null) {
            buckets[index] = createBucket();
        }
        // Update node's value if key is present
        for (Node x : buckets[index]) {
            if (x.key.equals(node.key)) {
                x.value = node.value;
                return null;
            }
        }
        // Or insert new node
        buckets[index].add(node);
        return node;
    }

    private Node removeNode(K key) {
        return removeNode(this.buckets, key);
    }

    // Return the removed node if node's key is present, else return null
    private Node removeNode(Collection<Node>[] buckets, K key) {
        int index = getIndex(buckets, key);
        if (buckets[index] == null) {
            return null;
        }
        for (Node x : buckets[index]) {
            if (x.key.equals(key)) {
                Node res = createNode(x.key, x.value);
                buckets[index].remove(x);
                if (buckets[index].size() == 0) {
                    buckets[index] = null;
                }
                return res;
            }
        }
        return null;
    }

    private Node removeNode(K key, V value) {
        return removeNode(this.buckets, key, value);
    }

    // Return the removed node if node's key is present and mapped to specified value,
    // else return null
    private Node removeNode(Collection<Node>[] buckets, K key, V value) {
        int index = getIndex(buckets, key);
        if (buckets[index] == null) {
            return null;
        }
        for (Node x : buckets[index]) {
            if (x.key.equals(key) && x.value.equals(value)) {
                Node res = createNode(x.key, x.value);
                buckets[index].remove(x);
                if (buckets[index].size() == 0) {
                    buckets[index] = null;
                }
                return res;
            }
        }
        return null;
    }

    private void conditionalResize() {
        double curLoad = 1.0 * this.size / this.buckets.length;
        if (curLoad >= maxLoad) {
            Collection<Node>[] newBuckets = createTable(2 * this.buckets.length);
            // Copy elements to new bucket
            for (Collection<Node> bucket : this.buckets) {
                if (bucket == null) {
                    continue;
                }
                for (Node x : bucket) {
                    putNode(newBuckets, createNode(x.key, x.value));
                }
            }
            this.buckets = newBuckets;
        }
    }

    @Override
    public void clear() {
        this.buckets = createTable(initialSize);
        this.size = 0;
    }

    @Override
    public boolean containsKey(K key) {
        Collection<Node> bucket = this.buckets[getIndex(key)];
        if (bucket == null) {
            return false;
        }
        for (Node x : bucket) {
            if (x.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(K key) {
        Collection<Node> bucket = this.buckets[getIndex(key)];
        if (bucket == null) {
            return null;
        }
        for (Node x : bucket) {
            if (x.key.equals(key)) {
                return x.value;
            }
        }
        return null;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public void put(K key, V value) {
        conditionalResize();
        Node x = putNode(createNode(key, value));
        if (x == null) {
            return;
        }
        this.size += 1;
    }

    @Override
    public Set<K> keySet() {
        Set<K> s = new HashSet<>();
        for (K key : this) {
            s.add(key);
        }
        return s;
    }

    @Override
    public V remove(K key) {
        Node x = removeNode(key);
        if (x == null) {
            return null;
        }
        this.size -= 1;
        return x.value;
    }

    @Override
    public V remove(K key, V value) {
        Node x = removeNode(key, value);
        if (x == null) {
            return null;
        }
        this.size -= 1;
        return x.value;
    }

    @Override
    public Iterator<K> iterator() {
        return new MyHashMapIterator();
    }

    private class MyHashMapIterator implements Iterator<K> {

        // Iterator of bucket
        private Iterator<Node> bIterator;

        // Index of bucket
        private int index = 0;

        public MyHashMapIterator() {
            // Advance to first non-null bucket
            while (index < buckets.length && buckets[index] == null) {
                index += 1;
            }
            // No next non-null bucket
            if (index >= buckets.length) {
                return;
            }
            bIterator = buckets[index].iterator();
        }

        @Override
        public boolean hasNext() {
            if (bIterator.hasNext()) {
                return true;
            }
            // Advance to next non-null bucket
            index += 1;
            while (index < buckets.length && buckets[index] == null) {
                index += 1;
            }
            // No next non-null bucket
            if (index >= buckets.length) {
                return false;
            }
            bIterator = buckets[index].iterator();
            return true;
        }

        @Override
        public K next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return bIterator.next().key;
        }
    }

}
