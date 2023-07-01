package gitlet;

/**
 * Represents a gitlet blob object.
 */
public class Blob implements Dumpable {
    private String content;

    public Blob(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Blob{" +
                "content='" + content + '\'' +
                '}';
    }

    @Override
    public void dump() {
        System.out.println(this);
    }
}
