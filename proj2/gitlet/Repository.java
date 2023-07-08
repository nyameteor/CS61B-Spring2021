package gitlet;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gitlet.Utils.*;

/**
 * Represents a gitlet repository.
 */
public class Repository {

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /**
     * The object directory.
     */
    public static final File OBJECT_DIR = join(GITLET_DIR, "objects");

    /**
     * The reference directory.
     */
    public static final File REF_DIR = join(GITLET_DIR, "refs");

    /**
     * The branch head reference directory.
     */
    public static final File HEADS_DIR = join(REF_DIR, "heads");

    /**
     * The Head file.
     */
    public static final File HEAD_FILE = join(GITLET_DIR, "HEAD");

    /**
     * The index file.
     */
    public static final File INDEX_FILE = join(GITLET_DIR, "index");

    /**
     * Default branch name.
     */
    public static final String DEFAULT_BRANCH_NAME = "master";

    /**
     * Default filenames to ignore.
     */
    public static final Set<String> DEFAULT_IGNORE_FILES = Set.of(".gitlet");

    /**
     * System file separator.
     */
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    /**
     * Date pattern.
     */
    static final String RFC2822_DATE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss Z";

    /**
     * Date format.
     */
    static final SimpleDateFormat RFC2822_DATE_FORMAT = getDateFormat(RFC2822_DATE_PATTERN);

    /* COMMANDS */

    static void initCmd() {
        if (GITLET_DIR.exists()) {
            throw error(
                    "A Gitlet version-control system already exists in the current directory.");
        }
        createDir(GITLET_DIR);
        createDir(OBJECT_DIR);
        createDir(REF_DIR);
        createDir(HEADS_DIR);

        Index index = new Index();
        writeIndex(index);

        // For initial commit, make a commit manually.
        Tree rootTree = indexToTrees(index).get(0);
        putObj(rootTree);
        Commit commit = new Commit(new Date(0L), objId(rootTree), "initial commit",
                new LinkedList<>());
        putObj(commit);
        Branch branch = new Branch(DEFAULT_BRANCH_NAME, objId(commit));
        writeBranch(branch);

        Head head = new Head();
        head.setBranchName(branch.getName());
        writeHead(head);
    }

    static void addCmd(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw error("File does not exist.");
        }

        Blob blob = createBlob(file);
        putObj(blob);

        Index index = readIndex();
        index.addLeaf(pathToParts(relativePath(file)), objId(blob));
        writeIndex(index);
    }

    static void commitCmd(String message) {
        if (Objects.isNull(message) || message.trim().isEmpty()) {
            throw error("Please enter a commit message.");
        }

        List<String> stagedFiles = new LinkedList<>();
        List<String> removedFiles = new LinkedList<>();
        diffStagedFiles(stagedFiles, removedFiles);
        if (stagedFiles.size() == 0 && removedFiles.size() == 0) {
            throw error("No changes added to the commit.");
        }

        Index index = readIndex();
        List<Tree> trees = indexToTrees(index);
        for (Tree tree : trees) {
            putObj(tree);
        }
        Tree rootTree = trees.get(trees.size() - 1);

        List<String> parentIds = new LinkedList<>();
        String headCommitId = getHeadCommitId();
        parentIds.add(headCommitId);

        Commit commit = new Commit(new Date(), objId(rootTree), message, parentIds);
        String commitId = objId(commit);
        putObj(commit);

        Branch branch = readBranch(getHeadBranchName());
        branch.setCommitId(commitId);
        writeBranch(branch);
    }

    static void rmCmd(String filePath) {
        File file = new File(filePath);
        Index index = readIndex();
        Index commitIndex = treeToIndex(lookupObj(getHeadTreeId(), Tree.class));
        List<String> pathParts = pathToParts(relativePath(file));
        Index.Node stagedNode = index.getLeaf(pathParts);
        Index.Node commitedNode = commitIndex.getLeaf(pathParts);
        if (Objects.nonNull(stagedNode)) {
            // If file is currently staged for addition.
            if (Objects.nonNull(commitedNode) && Objects.equals(stagedNode.id, commitedNode.id)) {
                // If the file is tracked in the current commit, stage it for removal
                // and remove the file from the working directory.
                index.removeLeaf(pathParts);
                if (file.exists()) {
                    deleteFile(file);
                }
            } else {
                // Else, unstage the file.
                index.removeLeaf(pathParts);
            }
            writeIndex(index);
        } else {
            throw error("No reason to remove the file.");
        }
    }

    static void logCmd() {
        List<Commit> commits = lookupCommits(getHeadCommitId());
        printCommits(commits);
    }

    static void globalLogCmd() {
        List<Commit> commits = lookupGlobalCommits();
        printCommits(commits);
    }

    static void findCmd(String message) {
        List<Commit> commits = lookupGlobalCommits(commit ->
                Objects.equals(message, commit.getMessage()));
        if (commits.isEmpty()) {
            throw error("Found no commit with that message.");
        }
        List<String> commitIds = commits.stream()
                .map(Repository::objId)
                .collect(Collectors.toList());
        System.out.println(String.join("\n", commitIds));
    }

    private static void printCommits(List<Commit> commits) {
        StringBuilder sb = new StringBuilder();
        for (Commit commit : commits) {
            String mergeInfo = commit.getParentIds().size() <= 1
                    ? ""
                    : "Merge: " + commit.getParentIds().stream()
                    .map(Repository::shortId)
                    .collect(Collectors.joining(" ")) + "\n";
            sb.append(String.format("===\n"
                            + "commit %s\n"
                            + "%s"
                            + "Date: %s\n"
                            + "%s\n\n",
                    objId(commit),
                    mergeInfo,
                    RFC2822_DATE_FORMAT.format(commit.getDate()),
                    commit.getMessage())
            );
        }
        System.out.printf("%s", sb);
    }

    static void statusCmd() {
        Head head = readHead();
        String currentBranch = head.getBranchName();
        List<String> branches = listBranchNames();
        List<String> displayBranches = branches.stream()
                .map((branch) -> Objects.equals(currentBranch, branch)
                        ? String.format("*%s", branch) : branch)
                .collect(Collectors.toList());

        List<String> stagedFiles = new LinkedList<>();
        List<String> removedFiles = new LinkedList<>();
        diffStagedFiles(stagedFiles, removedFiles);

        List<String> modifiedFiles = new LinkedList<>();
        List<String> deletedFiles = new LinkedList<>();
        diffNotStagedFiles(modifiedFiles, deletedFiles);

        List<String> untrackedFiles = new LinkedList<>();
        diffUntrackedFiles(untrackedFiles);

        List<String> displayNotStagedFiles = Stream
                .concat(modifiedFiles.stream()
                                .map((file) -> String.format("%s (modified)", file)),
                        deletedFiles.stream()
                                .map((file) -> String.format("%s (deleted)", file)))
                .collect(Collectors.toList());

        System.out.printf("=== Branches ===\n"
                        + "%s\n\n"
                        + "=== Staged Files ===\n"
                        + "%s\n\n"
                        + "=== Removed Files ===\n"
                        + "%s\n\n"
                        + "=== Modifications Not Staged For Commit ===\n"
                        + "%s\n\n"
                        + "=== Untracked Files ===\n"
                        + "%s\n\n",
                String.join("\n", displayBranches),
                String.join("\n", stagedFiles),
                String.join("\n", removedFiles),
                String.join("\n", displayNotStagedFiles),
                String.join("\n", untrackedFiles)
        );
    }

    static void checkoutFileCmd(String filePath) {
        checkoutFileCmd(getHeadCommitId(), filePath);
    }

    static void checkoutFileCmd(String commitId, String filePath) {
        Commit commit = lookupObj(commitId, Commit.class);
        if (Objects.isNull(commit)) {
            throw error("No commit with that id exists.");
        }
        Blob blob = lookupBlob(commitId, filePath);
        if (Objects.isNull(blob)) {
            throw error("File does not exist in that commit.");
        }
        restoreFile(filePath, blob);
    }

    static void checkoutBranchCmd(String branchName) {
        List<String> branchNames = listBranchNames();
        if (!branchNames.contains(branchName)) {
            throw error("No such branch exists.");
        }
        if (Objects.equals(branchName, getHeadBranchName())) {
            throw error("No need to checkout the current branch.");
        }

        String commitId = readBranch(branchName).getCommitId();
        Commit commit = lookupObj(commitId, Commit.class);
        Tree tree = lookupObj(commit.getTreeId(), Tree.class);

        validateNoFilesOverwriting(tree);

        restoreWd(tree);

        writeIndex(treeToIndex(tree));

        Head head = readHead();
        head.setBranchName(branchName);
        writeHead(head);
    }

    static void branchCmd(String branchName) {
        List<String> branchNames = listBranchNames();
        if (branchNames.contains(branchName)) {
            throw error("A branch with that name already exists.");
        }
        Branch branch = new Branch(branchName, getHeadCommitId());
        writeBranch(branch);
    }

    static void rmBranchCmd(String branchName) {
        List<String> branchNames = listBranchNames();
        if (!branchNames.contains(branchName)) {
            throw error("A branch with that name does not exist.");
        }
        if (Objects.equals(branchName, getHeadBranchName())) {
            throw error("Cannot remove the current branch.");
        }
        removeBranch(branchName);
    }

    static void resetCmd(String commitId) {
        Commit commit;
        try {
            commit = lookupObj(commitId, Commit.class);
        } catch (GitletException e) {
            throw error("No commit with that id exists.");
        }
        Tree tree = lookupObj(commit.getTreeId(), Tree.class);

        validateNoFilesOverwriting(tree);

        restoreWd(tree);

        writeIndex(treeToIndex(tree));

        Branch branch = readBranch(getHeadBranchName());
        branch.setCommitId(commitId);
        writeBranch(branch);
    }

    private static void validateNoFilesOverwriting(Tree tree) {
        List<String> untrackedFiles = new LinkedList<>();
        diffUntrackedFiles(untrackedFiles);
        if (anyFileInTree(tree, untrackedFiles)) {
            throw error("There is an untracked file in the way; " +
                    "delete it, or add and commit it first.");
        }
    }

    /* REFERENCE UTILS */

    static Head readHead() {
        return readObject(HEAD_FILE, Head.class);
    }

    static void writeHead(Head head) {
        writeObject(HEAD_FILE, head);
    }

    static String getHeadTreeId() {
        String commitId = getHeadCommitId();
        Commit commit = lookupObj(commitId, Commit.class);
        return commit.getTreeId();
    }

    static String getHeadCommitId() {
        Head head = readHead();
        if (Objects.isNull(head)) {
            return null;
        }
        if (Objects.nonNull(head.getCommitId())) {
            return head.getCommitId();
        } else {
            Branch branch = readBranch(head.getBranchName());
            return branch.getCommitId();
        }
    }

    static String getHeadBranchName() {
        Head head = readHead();
        return head.getBranchName();
    }

    static List<String> listBranchNames() {
        return plainFilenamesIn(HEADS_DIR);
    }

    static Branch readBranch(String branchName) {
        return readObject(join(HEADS_DIR, branchName), Branch.class);
    }

    static void writeBranch(Branch branch) {
        writeObject(join(HEADS_DIR, branch.getName()), branch);
    }

    static void removeBranch(String branchName) {
        deleteFile(join(HEADS_DIR, branchName));
    }

    /* INDEX UTILS */

    static Index readIndex() {
        return readObject(INDEX_FILE, Index.class);
    }

    static void writeIndex(Index index) {
        writeObject(INDEX_FILE, index);
    }

    /**
     * Diff index with current commit to get staged files.
     */
    static void diffStagedFiles(List<String> stagedFiles, List<String> removedFiles) {
        Map<String, String> indexPathMap = indexToPathMap(readIndex());
        Tree tree = lookupObj(getHeadTreeId(), Tree.class);
        Map<String, String> commitPathMap = indexToPathMap(treeToIndex(tree));
        for (String path : indexPathMap.keySet()) {
            if (!commitPathMap.containsKey(path)) {
                stagedFiles.add(path);
            } else if (!Objects.equals(indexPathMap.get(path), commitPathMap.get(path))) {
                stagedFiles.add(path);
            }
        }
        for (String path : commitPathMap.keySet()) {
            if (!indexPathMap.containsKey(path)) {
                removedFiles.add(path);
            }
        }
        Collections.sort(stagedFiles);
        Collections.sort(removedFiles);
    }

    /**
     * Diff index with current working directory to get not staged files.
     */
    static void diffNotStagedFiles(List<String> modifiedFiles, List<String> deletedFiles) {
        Map<String, String> indexPathMap = indexToPathMap(readIndex());
        Set<String> cwdFilePaths = listAllFilePaths(CWD);
        for (String path : indexPathMap.keySet()) {
            if (!cwdFilePaths.contains(path)) {
                deletedFiles.add(path);
            } else if (!Objects.equals(objId(createBlob(path)), indexPathMap.get(path))) {
                modifiedFiles.add(path);
            }
        }
        Collections.sort(modifiedFiles);
        Collections.sort(deletedFiles);
    }

    /**
     * Diff index with current working directory to get untracked files.
     * Like real git, if all files in a directory is untracked, use the directory instead of files.
     */
    static void diffUntrackedFiles(List<String> untrackedFiles) {
        Index index = readIndex();
        diffUntrackedFilesHelper(index.root, CWD, new LinkedList<>(), untrackedFiles);
        Collections.sort(untrackedFiles);
    }

    private static void diffUntrackedFilesHelper(Index.Node node, File dir, List<String> parts,
                                                 List<String> untrackedFiles) {
        for (File file : Objects.requireNonNull(listFiles(dir))) {
            String filename = file.getName();
            parts.add(filename);
            if (file.isFile()) {
                if (!node.childMap.containsKey(filename)) {
                    untrackedFiles.add(relativePath(partsToPath(parts)));
                }
            } else {
                if (!node.childMap.containsKey(filename)) {
                    untrackedFiles.add(relativePath(partsToPath(parts)));
                } else {
                    diffUntrackedFilesHelper(
                            node.childMap.get(filename), file, parts, untrackedFiles);
                }
            }
            parts.remove(parts.size() - 1);
        }
    }

    /**
     * Check if there are any given files in the tree.
     */
    private static boolean anyFileInTree(Tree tree, List<String> filePaths) {
        Map<String, String> pathMap = indexToPathMap(treeToIndex(tree));
        for (String filePath : filePaths) {
            if (pathMap.containsKey(filePath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return map of filepath -> id
     */
    static Map<String, String> indexToPathMap(Index index) {
        Map<String, String> pathToId = new HashMap<>();
        indexToPathMapHelper(index.root, new LinkedList<>(), pathToId);
        return pathToId;
    }

    private static void indexToPathMapHelper(Index.Node node, List<String> parts,
                                             Map<String, String> pathToId) {
        parts.add(node.name);
        if (Index.isLeaf(node)) {
            pathToId.put(relativePath(partsToPath(parts)), node.id);
            parts.remove(parts.size() - 1);
            return;
        }
        for (Index.Node n : node.childMap.values()) {
            indexToPathMapHelper(n, parts, pathToId);
        }
        parts.remove(parts.size() - 1);
    }

    static List<Tree> indexToTrees(Index index) {
        List<Tree> trees = new LinkedList<>();
        indexToTreesHelper(index.root, trees);
        return trees;
    }

    private static String indexToTreesHelper(Index.Node node, List<Tree> trees) {
        if (Index.isLeaf(node)) {
            return node.id;
        }
        Map<String, Tree.Entry> entryMap = new HashMap<>();
        for (Index.Node childNode : node.childMap.values()) {
            String childNodeId = indexToTreesHelper(childNode, trees);
            if (Index.isLeaf(childNode)) {
                entryMap.put(childNode.name,
                        new Tree.Entry(childNode.name, Tree.Entry.BLOB_TYPE, childNodeId));
            } else {
                entryMap.put(childNode.name,
                        new Tree.Entry(childNode.name, Tree.Entry.TREE_TYPE, childNodeId));
            }
        }
        Tree newTree = new Tree(entryMap);
        trees.add(newTree);
        return objId(newTree);
    }

    static Index treeToIndex(Tree tree) {
        Index index = new Index();
        treeToIndexHelper(tree, index.root);
        return index;
    }

    private static void treeToIndexHelper(Tree tree, Index.Node node) {
        for (Tree.Entry entry : tree.getEntryMap().values()) {
            if (Tree.Entry.BLOB_TYPE.equals(entry.type)) {
                node.childMap.put(entry.name, new Index.Node(entry.name, entry.id));
            } else if (Tree.Entry.TREE_TYPE.equals(entry.type)) {
                Index.Node childNode = new Index.Node(entry.name, null, new TreeMap<>());
                Tree childTree = lookupObj(entry.id, Tree.class);
                treeToIndexHelper(childTree, childNode);
                node.childMap.put(entry.name, childNode);
            }
        }
    }

    /* OBJECT UTILS */

    /**
     * Lookup an object from object database, return null if file not exists.
     */
    static <T extends Obj> T lookupObj(String id, Class<T> expectedObjClass) {
        File file = objFilepath(id);
        if (!file.exists()) {
            throw error("Object does not exists: %s", file.getPath());
        }
        return readObject(file, expectedObjClass);
    }

    /**
     * Put(insert or update) an object into object database.
     */
    static void putObj(Obj obj) {
        File file = objFilepath(obj);
        if (file.exists()) {
            return;
        }
        File parent = file.getParentFile();
        if (!parent.exists()) {
            createDir(file.getParentFile());
        }
        writeObject(file, obj);
    }

    /**
     * Lookup history commits from the given commitId to the initial commitId.
     */
    static List<Commit> lookupCommits(String commitId) {
        List<Commit> commits = new LinkedList<>();
        Commit curCommit = lookupObj(commitId, Commit.class);
        while (curCommit.getParentIds().size() > 0) {
            commits.add(curCommit);
            curCommit = lookupObj(curCommit.getParentIds().get(0), Commit.class);
        }
        commits.add(curCommit);
        return commits;
    }

    /**
     * Lookup all commits ever made, The order of the commits does not matter.
     */
    static List<Commit> lookupGlobalCommits() {
        return lookupGlobalCommits(commit -> true);
    }

    /**
     * Lookup all commits ever made that satisfy the specified filter,
     * The order of the commits does not matter.
     */
    static List<Commit> lookupGlobalCommits(CommitFilter filter) {
        List<Commit> commits = new LinkedList<>();
        List<String> commitIds = listBranchNames().stream()
                .map((name) -> readBranch(name).getCommitId())
                .collect(Collectors.toList());
        lookupGlobalCommitsHelper(commitIds, filter, new HashSet<>(), commits);
        return commits;
    }

    private static void lookupGlobalCommitsHelper(List<String> commitIds,
                                                  CommitFilter filter,
                                                  Set<String> seenCommitIds,
                                                  List<Commit> result) {
        for (String id : commitIds) {
            if (seenCommitIds.contains(id)) {
                return;
            }
            seenCommitIds.add(id);
            Commit commit = lookupObj(id, Commit.class);
            if (filter.accept(commit)) {
                result.add(commit);
            }
            lookupGlobalCommitsHelper(commit.getParentIds(), filter, seenCommitIds, result);
        }
    }

    @FunctionalInterface
    interface CommitFilter {
        boolean accept(Commit commit);
    }

    /**
     * Lookup the blob with filePath from the given commitId,
     * return null if the blob not exists.
     */
    static Blob lookupBlob(String commitId, String filePath) {
        Commit commit = lookupObj(commitId, Commit.class);
        Tree tree = lookupObj(commit.getTreeId(), Tree.class);
        return lookupBlobHelper(tree, pathToParts(relativePath(filePath)));
    }

    private static Blob lookupBlobHelper(Tree tree, List<String> parts) {
        if (parts.size() < 1) {
            return null;
        }
        String part = parts.get(0);
        if (tree.getEntryMap().containsKey(part)) {
            Tree.Entry entry = tree.getEntryMap().get(part);
            if (Tree.Entry.BLOB_TYPE.equals(entry.type) && parts.size() == 1) {
                return lookupObj(entry.id, Blob.class);
            } else {
                Tree childTree = lookupObj(entry.id, Tree.class);
                return lookupBlobHelper(childTree, parts.subList(1, parts.size()));
            }
        }
        return null;
    }

    /**
     * Restore the working directory to a tree.
     */
    static void restoreWd(Tree dstTree) {
        Tree srcTree = lookupObj(getHeadTreeId(), Tree.class);
        restoreWdHelper(srcTree, dstTree, new LinkedList<>());
    }

    /**
     * Restore the working directory from a srcTree to a dstTree.
     */
    private static void restoreWdHelper(Tree srcTree, Tree dstTree, List<String> parts) {
        for (Tree.Entry srcEntry : srcTree.getEntryMap().values()) {
            parts.add(srcEntry.name);
            File file = pathToFile(relativePath(partsToPath(parts)));
            if (!dstTree.getEntryMap().containsKey(srcEntry.name)) {
                // Remove files or directories that are not in dstTree.
                deleteFileOrDir(file);
            }
            parts.remove(parts.size() - 1);
        }
        for (Tree.Entry dstEntry : dstTree.getEntryMap().values()) {
            parts.add(dstEntry.name);
            File file = pathToFile(relativePath(partsToPath(parts)));
            if (!srcTree.getEntryMap().containsKey(dstEntry.name)) {
                // Restore files or directories that are not in srcTree.
                if (Tree.Entry.BLOB_TYPE.equals(dstEntry.type)) {
                    Blob blob = lookupObj(dstEntry.id, Blob.class);
                    restoreFile(file, blob);
                } else {
                    restoreDir(parts, dstTree);
                }
            } else {
                Tree.Entry srcEntry = srcTree.getEntryMap().get(dstEntry.name);
                if (Objects.equals(srcEntry.type, dstEntry.type)) {
                    if (Tree.Entry.TREE_TYPE.equals(srcEntry.type)) {
                        Tree childSrcTree = lookupObj(srcEntry.id, Tree.class);
                        Tree childDstTree = lookupObj(dstEntry.id, Tree.class);
                        restoreWdHelper(childSrcTree, childDstTree, parts);
                    } else {
                        if (!Objects.equals(srcEntry.id, dstEntry.id)) {
                            Blob blob = lookupObj(dstEntry.id, Blob.class);
                            restoreFile(file, blob);
                        }
                    }
                } else {
                    if (Tree.Entry.BLOB_TYPE.equals(srcEntry.type)) {
                        // `file` is a file in srcTree, and a directory in dstTree.
                        deleteFile(file);
                    } else {
                        // `file` is a file in dstTree, and a directory in srcTree.
                        deleteFileOrDir(file);
                        Blob blob = lookupObj(dstEntry.id, Blob.class);
                        restoreFile(file, blob);
                    }
                }
            }
            parts.remove(parts.size() - 1);
        }
    }

    static void restoreDir(List<String> parts, Tree tree) {
        File dir = pathToFile(relativePath(partsToPath(parts)));
        if (!dir.exists()) {
            createDir(dir);
        }
        for (Tree.Entry entry : tree.getEntryMap().values()) {
            parts.add(entry.name);
            File file = pathToFile(relativePath(partsToPath(parts)));
            if (Tree.Entry.BLOB_TYPE.equals(entry.type)) {
                Blob blob = lookupObj(entry.id, Blob.class);
                restoreFile(file, blob);
            } else {
                Tree childTree = lookupObj(entry.id, Tree.class);
                restoreDir(parts, childTree);
            }
            parts.remove(parts.size() - 1);
        }
    }

    static void restoreFile(String path, Blob blob) {
        restoreFile(pathToFile(path), blob);
    }

    static void restoreFile(File file, Blob blob) {
        writeContents(file, blob.getContent());
    }

    static String objId(Obj obj) {
        return sha1(obj.toString());
    }

    static String shortId(String id) {
        return id.substring(0, 7);
    }

    static File objFilepath(Obj obj) {
        String id = objId(obj);
        return objFilepath(id);
    }

    static File objFilepath(String id) {
        return join(Repository.OBJECT_DIR, id.substring(0, 2), id.substring(2));
    }

    static Blob createBlob(String path) {
        return createBlob(pathToFile(path));
    }

    static Blob createBlob(File file) {
        return new Blob(readContentsAsString(file));
    }

    /* FILE UTILS */

    static File[] listFiles(File dir) {
        return listFiles(dir, DEFAULT_IGNORE_FILES);
    }

    static File[] listFiles(File dir, Set<String> ignoreFiles) {
        return dir.listFiles(
                (d, name) -> !ignoreFiles.contains(Utils.join(d, name).getName()));
    }

    static Set<String> listAllFilePaths(File dir) {
        Set<String> filePaths = new HashSet<>();
        listAllFilePathsHelper(dir, filePaths);
        return filePaths;
    }

    private static void listAllFilePathsHelper(File file, Set<String> filePaths) {
        if (file.isFile()) {
            filePaths.add(relativePath(file));
        } else if (file.isDirectory()) {
            for (File f : listFiles(file)) {
                listAllFilePathsHelper(f, filePaths);
            }
        }
    }

    /**
     * Convert relative path to file.
     */
    static File pathToFile(String relativePath) {
        return join(CWD, relativePath);
    }

    static String relativePath(String filePath) {
        return relativePath(new File(filePath));
    }

    static String relativePath(File file) {
        return relativePath(file, CWD);
    }

    static String relativePath(File file, File base) {
        return base.toURI().relativize(file.toURI()).getPath();
    }

    static List<String> pathToParts(String path) {
        return List.of(path.split(FILE_SEPARATOR));
    }

    static String partsToPath(List<String> parts) {
        return String.join(FILE_SEPARATOR, parts);
    }

    static void createDir(File dir) {
        dir.mkdir();
    }

    static void deleteFile(File file) {
        file.delete();
    }

    // Delete a file or delete a directory recursively.
    static void deleteFileOrDir(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteFileOrDir(f);
                }
            }
        }
        file.delete();
    }

    static boolean isInitialized() {
        return GITLET_DIR.exists();
    }

    /* DATE UTILS */

    static SimpleDateFormat getDateFormat(String pattern) {
        // https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        format.setTimeZone(TimeZone.getDefault());
        return format;
    }
}
