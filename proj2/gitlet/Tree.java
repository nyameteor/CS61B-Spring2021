package gitlet;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a gitlet tree object.
 */
public class Tree implements Dumpable {
    private static class Entry implements Serializable {
        private String name;
        private String type;
        private String id;

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
