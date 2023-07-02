package gitlet;

/**
 * Represents the gitlet HEAD.
 * HEAD can point to a branch or a commit id (in detached HEAD state).
 */
public class Head implements Dumpable {

    private String branchName;

    private String commitId;

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    @Override
    public String toString() {
        return "Head{" +
                "branchName='" + branchName + '\'' +
                ", commitId='" + commitId + '\'' +
                '}';
    }

    @Override
    public void dump() {
        System.out.println(this);
    }
}
