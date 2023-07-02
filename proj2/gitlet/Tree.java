package gitlet;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a gitlet tree object.
 */
public class Tree extends Obj {
    static final String ENTRY_TYPE_BLOB = "blob";
    static final String ENTRY_TYPE_TREE = "tree";

    static class Entry implements Serializable {
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

    private List<Entry> entries;

    public Tree() {
    }

    public Tree(List<Entry> entries) {
        this.entries = entries;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    @Override
    public String toString() {
        return "Tree{" +
                "entries=" + entries +
                '}';
    }

    @Override
    public void dump() {
        System.out.println(this);
    }
}
