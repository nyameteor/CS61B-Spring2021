package bstmap;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class BSTMap<K extends Comparable<K>, V> implements Map61B<K, V> {
    private BST<K, V> bst;
    private int size;

    public BSTMap() {
        this.bst = new BST<>();
        this.size = 0;
    }

    @Override
    public void clear() {
        this.bst = new BST<>();
        this.size = 0;
    }

    @Override
    public boolean containsKey(K key) {
        return bst.contains(key);
    }

    @Override
    public V get(K key) {
        return bst.find(key);
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public void put(K key, V value) {
        if (!containsKey(key)) {
            size += 1;
        }
        bst.insert(key, value);
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
        V val = bst.find(key);
        if (val != null) {
            bst.delete(key);
            size -= 1;
        }
        return val;
    }

    @Override
    public V remove(K key, V value) {
        V val = bst.find(key);
        if (val != null && value == val) {
            bst.delete(key);
            size -= 1;
        }
        return val;
    }

    @Override
    public Iterator<K> iterator() {
        return bst.iterator();
    }

}
