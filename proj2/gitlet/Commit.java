package gitlet;


import java.util.Date;
import java.util.List;

/**
 * Represents a gitlet commit object.
 */
public class Commit extends Obj {

    private Date date;

    private String treeId;

    private String message;

    private List<String> parentIds;

    public Commit(Date date, String treeId, String message, List<String> parentIds) {
        this.date = date;
        this.treeId = treeId;
        this.message = message;
        this.parentIds = parentIds;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getTreeId() {
        return treeId;
    }

    public void setTreeId(String treeId) {
        this.treeId = treeId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getParentIds() {
        return parentIds;
    }

    public void setParentIds(List<String> parentIds) {
        this.parentIds = parentIds;
    }

    @Override
    public String toString() {
        return "Commit{" +
                "date=" + date +
                ", treeId='" + treeId + '\'' +
                ", message='" + message + '\'' +
                ", parentIds=" + parentIds +
                '}';
    }

    @Override
    public void dump() {
        System.out.println(this);
    }
}
