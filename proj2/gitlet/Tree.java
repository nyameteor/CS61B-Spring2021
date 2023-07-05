package gitlet;

import java.io.Serializable;
import java.util.Map;

/**
 * Represents a gitlet tree object.
 */
public class Tree extends Obj {
    static class Entry implements Serializable {
        static final String BLOB_TYPE = "blob";
        static final String TREE_TYPE = "tree";

        String name;
        String type;
        String id;

        public Entry(String name, String type, String id) {
            this.name = name;
            this.type = type;
            this.id = id;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", id='" + id + '\'' +
                    '}';
        }
    }

    private Map<String, Entry> entryMap;

    public Tree() {
    }

    public Tree(Map<String, Entry> entryMap) {
        this.entryMap = entryMap;
    }

    public Map<String, Entry> getEntryMap() {
        return entryMap;
    }

    public void setEntryMap(Map<String, Entry> entryMap) {
        this.entryMap = entryMap;
    }

    @Override
    public String toString() {
        return "Tree{" +
                "entryMap=" + entryMap +
                '}';
    }

    @Override
    public void dump() {
        System.out.println(this);
    }
}
