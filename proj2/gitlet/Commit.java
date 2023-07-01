package gitlet;


import java.util.Date;
import java.util.List;

/**
 * Represents a gitlet commit object.
 */
public class Commit implements Dumpable {

    private Date date;

    private Tree tree;

    private String message;

    private List<Commit> parents;

    public Commit(Date date, Tree tree, String message, List<Commit> parents) {
        this.date = date;
        this.tree = tree;
        this.message = message;
        this.parents = parents;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Tree getTree() {
        return tree;
    }

    public void setTree(Tree tree) {
        this.tree = tree;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Commit> getParents() {
        return parents;
    }

    public void setParents(List<Commit> parents) {
        this.parents = parents;
    }

    @Override
    public String toString() {
        return "Commit{" +
                "date=" + date +
                ", tree=" + tree +
                ", message='" + message + '\'' +
                ", parents=" + parents +
                '}';
    }

    @Override
    public void dump() {
        System.out.println(this);
    }
}
