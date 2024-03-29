package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
     * The global-log file to store all the commit ids ever made.
     * DOES NOT exist in real git.
     */
    public static final File GLOBAL_LOG_FILE = join(REF_DIR, "global-log");

    /**
     * Default branch name.
     */
    public static final String DEFAULT_BRANCH_NAME = "master";

    /**
     * Default filenames to ignore.
     */
    public static final Set<String> DEFAULT_IGNORE_FILES = Set.of(".gitlet");

    /**
     * Date pattern.
     */
    static final String DATE_PATTERN = "EEE MMM dd HH:mm:ss yyyy Z";

    /**
     * Date format.
     */
    static final SimpleDateFormat DATE_FORMAT = getDateFormat(DATE_PATTERN);

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

        GlobalLog globalLog = new GlobalLog();
        globalLog.getCommitIds().add(objId(commit));
        writeGlobalLog(globalLog);
    }

    static void addCmd(String filePath) {
        File file = pathToFile(filePath);
        if (!file.exists()) {
            throw error("File does not exist.");
        }

        Blob blob = saveFileAsBlob(file);

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

        makeCommit(message, getHeadCommitId());
    }

    private static void makeCommit(String message, String... parentCommitIds) {
        Index index = readIndex();
        List<Tree> trees = indexToTrees(index);
        for (Tree tree : trees) {
            putObj(tree);
        }
        Tree rootTree = trees.get(trees.size() - 1);

        List<String> parentIds = List.of(parentCommitIds);

        Commit commit = new Commit(new Date(), objId(rootTree), message, parentIds);
        String commitId = objId(commit);
        putObj(commit);

        Branch branch = readBranch(getHeadBranchName());
        branch.setCommitId(commitId);
        writeBranch(branch);

        GlobalLog globalLog = readGlobalLog();
        globalLog.getCommitIds().add(objId(commit));
        writeGlobalLog(globalLog);
    }

    static void rmCmd(String filePath) {
        File file = pathToFile(filePath);
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
                    DATE_FORMAT.format(commit.getDate()),
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
                        + "%s\n"
                        + "=== Staged Files ===\n"
                        + "%s\n"
                        + "=== Removed Files ===\n"
                        + "%s\n"
                        + "=== Modifications Not Staged For Commit ===\n"
                        + "%s\n"
                        + "=== Untracked Files ===\n"
                        + "%s\n",
                displayBranches.stream().map(item -> item + "\n")
                        .collect(Collectors.joining("")),
                stagedFiles.stream().map(item -> item + "\n")
                        .collect(Collectors.joining("")),
                removedFiles.stream().map(item -> item + "\n")
                        .collect(Collectors.joining("")),
                displayNotStagedFiles.stream().map(item -> item + "\n")
                        .collect(Collectors.joining("")),
                untrackedFiles.stream().map(item -> item + "\n")
                        .collect(Collectors.joining(""))
        );
    }

    static void checkoutFileCmd(String filePath) {
        checkoutFileCmd(getHeadCommitId(), filePath);
    }

    static void checkoutFileCmd(String prefixOfCommitId, String filePath) {
        String commitId;
        try {
            commitId = idFromPrefix(prefixOfCommitId);
            lookupObj(commitId, Commit.class);
        } catch (GitletException e) {
            throw error("No commit with that id exists.");
        }
        Blob blob;
        try {
            blob = lookupBlob(commitId, filePath);
        } catch (GitletException e) {
            throw error("File does not exist in that commit.");
        }
        restoreFile(pathToFile(filePath), blob);
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

    static void resetCmd(String prefixOfCommitId) {
        Commit commit;
        String commitId;
        try {
            commitId = idFromPrefix(prefixOfCommitId);
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

    static void mergeCmd(String branchName) {
        validateNoUncommittedChanges();
        List<String> branchNames = listBranchNames();
        if (!branchNames.contains(branchName)) {
            throw error("A branch with that name does not exist.");
        }
        if (Objects.equals(getHeadBranchName(), branchName)) {
            throw error("Cannot merge a branch with itself.");
        }

        String curCommitId = getHeadCommitId();
        String givenCommitId = readBranch(branchName).getCommitId();
        String splitPointId = objId(lookupSplitPointCommit(curCommitId, givenCommitId));
        Commit curCommit = lookupObj(curCommitId, Commit.class);
        Commit givenCommit = lookupObj(givenCommitId, Commit.class);
        Commit splitPointCommit = lookupObj(splitPointId, Commit.class);
        Tree curTree = lookupObj(curCommit.getTreeId(), Tree.class);
        Tree givenTree = lookupObj(givenCommit.getTreeId(), Tree.class);
        Tree splitPointTree = lookupObj(splitPointCommit.getTreeId(), Tree.class);

        validateNoFilesOverwriting(givenTree);

        if (Objects.equals(splitPointId, givenCommitId)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        } else if (Objects.equals(splitPointId, curCommitId)) {
            checkoutBranchCmd(branchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        Map<String, String> curModifiedMap = new HashMap<>(),
                curAddedMap = new HashMap<>(),
                curDeletedMap = new HashMap<>(),
                givenModifiedMap = new HashMap<>(),
                givenAddedMap = new HashMap<>(),
                givenDeletedMap = new HashMap<>();
        diffTreeChanges(splitPointTree, curTree,
                curModifiedMap, curAddedMap, curDeletedMap);
        diffTreeChanges(splitPointTree, givenTree,
                givenModifiedMap, givenAddedMap, givenDeletedMap);

        boolean conflict = handleMerge(curModifiedMap, curAddedMap, curDeletedMap,
                givenModifiedMap, givenAddedMap, givenDeletedMap);

        makeCommit(String.format("Merged %s into %s.", branchName, getHeadBranchName()),
                curCommitId, givenCommitId);
        if (conflict) {
            throw error("Encountered a merge conflict.");
        }
    }

    /**
     * Handle merge to update files and index, return true if encountered a merge conflict.
     */
    private static boolean handleMerge(Map<String, String> curModifiedMap,
                                       Map<String, String> curAddedMap,
                                       Map<String, String> curDeletedMap,
                                       Map<String, String> givenModifiedMap,
                                       Map<String, String> givenAddedMap,
                                       Map<String, String> givenDeletedMap) {
        Index index = readIndex();
        boolean conflict = false;
        for (String path : givenDeletedMap.keySet()) {
            if (!curModifiedMap.containsKey(path)) {
                // Any files present at the split point, unmodified in the current branch,
                // and absent in the given branch should be removed (and untracked).
                deleteFile(pathToFile(path));
                index.removeLeaf(pathToParts(path));
            }
        }
        for (String path : givenAddedMap.keySet()) {
            if (!curAddedMap.containsKey(path)) {
                // Any files that were not present at the split point and are present
                // only in the given branch should be checked out and staged.
                String id = givenAddedMap.get(path);
                restoreFile(path, id);
                index.addLeaf(pathToParts(path), id);
            } else {
                String curId = curAddedMap.get(path);
                String givenId = givenAddedMap.get(path);
                if (!Objects.equals(curId, givenId)) {
                    // Conflict.
                    conflict = true;
                    writeConflictFile(path, curId, givenId);
                    Blob blob = saveFileAsBlob(path);
                    index.addLeaf(pathToParts(path), objId(blob));
                }
            }
        }
        for (String path : curModifiedMap.keySet()) {
            if (givenDeletedMap.containsKey(path)) {
                // Conflict.
                conflict = true;
                String curId = curModifiedMap.get(path);
                writeConflictFile(path, curId, null);
                Blob blob = saveFileAsBlob(path);
                index.addLeaf(pathToParts(path), objId(blob));
            }
        }
        for (String path : givenModifiedMap.keySet()) {
            if (curDeletedMap.containsKey(path)) {
                String givenId = givenModifiedMap.get(path);
                // Conflict.
                conflict = true;
                writeConflictFile(path, null, givenId);
                Blob blob = saveFileAsBlob(path);
                index.addLeaf(pathToParts(path), objId(blob));
            }
            if (!curModifiedMap.containsKey(path)) {
                // Any files that have been modified in the given branch since the split
                // point, but not modified in the current branch since the split point
                // should be changed to their versions in the given branch.
                String id = givenModifiedMap.get(path);
                restoreFile(path, id);
                index.addLeaf(pathToParts(path), id);
            } else {
                String curId = curModifiedMap.get(path);
                String givenId = givenModifiedMap.get(path);
                if (!Objects.equals(curId, givenId)) {
                    // Conflict.
                    conflict = true;
                    writeConflictFile(path, curId, givenId);
                    Blob blob = saveFileAsBlob(path);
                    index.addLeaf(pathToParts(path), objId(blob));
                }
            }
        }
        writeIndex(index);
        return conflict;
    }

    private static void validateNoUncommittedChanges() {
        List<String> stagedFiles = new LinkedList<>();
        List<String> removedFiles = new LinkedList<>();
        diffStagedFiles(stagedFiles, removedFiles);
        if (!stagedFiles.isEmpty() || !removedFiles.isEmpty()) {
            throw error("You have uncommitted changes.");
        }
    }

    private static void validateNoFilesOverwriting(Tree givenTree) {
        List<String> untrackedFiles = new LinkedList<>();
        diffUntrackedFiles(untrackedFiles);
        if (anyFileInTree(givenTree, untrackedFiles)) {
            throw error("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
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

    static GlobalLog readGlobalLog() {
        return readObject(GLOBAL_LOG_FILE, GlobalLog.class);
    }

    static void writeGlobalLog(GlobalLog globalLog) {
        writeObject(GLOBAL_LOG_FILE, globalLog);
    }

    static class GlobalLog implements Serializable {
        List<String> commitIds;

        GlobalLog() {
            this.commitIds = new LinkedList<>();
        }

        public List<String> getCommitIds() {
            return commitIds;
        }
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
            if (entry.isBlob()) {
                node.childMap.put(entry.name, new Index.Node(entry.name, entry.id));
            } else if (entry.isTree()) {
                Index.Node childNode = new Index.Node(entry.name, null, new TreeMap<>());
                Tree childTree = lookupObj(entry.id, Tree.class);
                treeToIndexHelper(childTree, childNode);
                node.childMap.put(entry.name, childNode);
            }
        }
    }

    /**
     * Diff two trees to get modified, added and deleted maps,
     * map entry is filepath -> id.
     */
    static void diffTreeChanges(Tree fromTree, Tree toTree,
                                Map<String, String> modifiedMap,
                                Map<String, String> addedMap,
                                Map<String, String> deletedMap) {
        List<FileInfo> modifiedFiles = new LinkedList<>(),
                addedFiles = new LinkedList<>(),
                deletedFiles = new LinkedList<>();
        diffTreeChanges(fromTree, toTree, modifiedFiles, addedFiles, deletedFiles);
        modifiedMap.putAll(FileInfo.toPathMap(modifiedFiles));
        addedMap.putAll(FileInfo.toPathMap(addedFiles));
        deletedMap.putAll(FileInfo.toPathMap(deletedFiles));
    }

    /**
     * Diff two trees to get modified, added and deleted files.
     */
    static void diffTreeChanges(Tree fromTree, Tree toTree,
                                List<FileInfo> modifiedFiles,
                                List<FileInfo> addedFiles,
                                List<FileInfo> deletedFiles) {
        List<String> parts = new LinkedList<>();
        for (Tree.Entry entry : fromTree.getEntryMap().values()) {
            diffTreeChangesHelper(entry,
                    toTree.getEntryMap().getOrDefault(entry.name, null),
                    parts, modifiedFiles, addedFiles, deletedFiles);
        }
        for (Tree.Entry entry : toTree.getEntryMap().values()) {
            if (!fromTree.getEntryMap().containsKey(entry.name)) {
                diffTreeChangesHelper(null, entry, parts,
                        modifiedFiles, addedFiles, deletedFiles);
            }
        }
    }

    private static void diffTreeChangesHelper(Tree.Entry fromEntry, Tree.Entry toEntry,
                                              List<String> parts,
                                              List<FileInfo> modifiedFiles,
                                              List<FileInfo> addedFiles,
                                              List<FileInfo> deletedFiles) {
        if (Objects.isNull(fromEntry)) {
            parts.add(toEntry.name);
            String path = relativePath(partsToPath(parts));
            if (toEntry.isBlob()) {
                addedFiles.add(new FileInfo(path, toEntry.id));
            } else {
                Tree childToTree = lookupObj(toEntry.id, Tree.class);
                for (Tree.Entry entry : childToTree.getEntryMap().values()) {
                    diffTreeChangesHelper(null, entry, parts,
                            modifiedFiles, addedFiles, deletedFiles);
                }
            }
            parts.remove(parts.size() - 1);
        } else if (Objects.isNull(toEntry)) {
            parts.add(fromEntry.name);
            String path = relativePath(partsToPath(parts));
            if (fromEntry.isBlob()) {
                deletedFiles.add(new FileInfo(path, fromEntry.id));
            } else {
                Tree childFromTree = lookupObj(fromEntry.id, Tree.class);
                for (Tree.Entry entry : childFromTree.getEntryMap().values()) {
                    diffTreeChangesHelper(entry, null, parts,
                            modifiedFiles, addedFiles, deletedFiles);
                }
            }
            parts.remove(parts.size() - 1);
        } else {
            parts.add(fromEntry.name);
            if (Objects.equals(fromEntry.type, toEntry.type)) {
                if (fromEntry.isTree()) {
                    Tree childFromTree = lookupObj(fromEntry.id, Tree.class);
                    Tree childToTree = lookupObj(toEntry.id, Tree.class);
                    for (Tree.Entry entry : childFromTree.getEntryMap().values()) {
                        diffTreeChangesHelper(entry,
                                childToTree.getEntryMap().getOrDefault(entry.name, null),
                                parts, modifiedFiles, addedFiles, deletedFiles);
                    }
                    for (Tree.Entry entry : childToTree.getEntryMap().values()) {
                        if (!childFromTree.getEntryMap().containsKey(entry.name)) {
                            diffTreeChangesHelper(null, entry, parts,
                                    modifiedFiles, addedFiles, deletedFiles);
                        }
                    }
                } else {
                    if (!Objects.equals(fromEntry.id, toEntry.id)) {
                        String path = relativePath(partsToPath(parts));
                        modifiedFiles.add(new FileInfo(path, toEntry.id));
                    }
                }
            } else {
                if (fromEntry.isBlob()) {
                    // `file` is a file in fromEntry, and a directory in toEntry
                    String path = relativePath(partsToPath(parts));
                    deletedFiles.add(new FileInfo(path, fromEntry.id));
                    Tree childToTree = lookupObj(toEntry.id, Tree.class);
                    for (Tree.Entry childToEntry : childToTree.getEntryMap().values()) {
                        diffTreeChangesHelper(null, childToEntry, parts,
                                modifiedFiles, addedFiles, deletedFiles);
                    }
                } else {
                    // `file` is a file in toEntry, and a directory in fromEntry
                    String path = relativePath(partsToPath(parts));
                    addedFiles.add(new FileInfo(path, toEntry.id));
                    Tree childFromTree = lookupObj(fromEntry.id, Tree.class);
                    for (Tree.Entry childFromEntry : childFromTree.getEntryMap().values()) {
                        diffTreeChangesHelper(childFromEntry, null, parts,
                                modifiedFiles, addedFiles, deletedFiles);
                    }
                }
            }
            parts.remove(parts.size() - 1);
        }
    }

    static class FileInfo {
        String path;
        String id;

        FileInfo(String path, String id) {
            this.path = path;
            this.id = id;
        }

        public static Map<String, String> toPathMap(List<FileInfo> files) {
            return files.stream()
                    .collect(Collectors.toMap(file -> file.path, file -> file.id));
        }
    }

    /* OBJECT UTILS */

    /**
     * Lookup an object from object database by id, throw error if not exists.
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
        List<String> commitIds = readGlobalLog().getCommitIds();
        List<Commit> commits = commitIds.stream()
                .map(id -> lookupObj(id, Commit.class))
                .filter(filter::accept)
                .collect(Collectors.toList());
        Collections.reverse(commits);
        return commits;
    }

    /**
     * Lookup the commits from all branches.
     */
    static List<Commit> lookupCommitsFromAllBranches() {
        return lookupCommitsFromAllBranches(commit -> true);
    }

    /**
     * Lookup the commits from all branches that satisfy the specified filter.
     */
    static List<Commit> lookupCommitsFromAllBranches(CommitFilter filter) {
        List<Commit> commits = new LinkedList<>();
        List<String> commitIds = listBranchNames().stream()
                .map((name) -> readBranch(name).getCommitId())
                .collect(Collectors.toList());
        lookupCommitsFromAllBranchesHelper(commitIds, filter, new HashSet<>(), commits);
        return commits;
    }

    private static void lookupCommitsFromAllBranchesHelper(List<String> commitIds,
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
            lookupCommitsFromAllBranchesHelper(commit.getParentIds(),
                    filter, seenCommitIds, result);
        }
    }

    @FunctionalInterface
    interface CommitFilter {
        boolean accept(Commit commit);
    }

    /**
     * Lookup the split point commit of two given commits, the split point is
     * the latest common ancestor of two commits.
     */
    static Commit lookupSplitPointCommit(String commitId1, String commitId2) {
        TreeMap<Integer, String> splitPointMap = new TreeMap<>();
        lookupSplitPointCommitHelper(commitId1, commitId2,
                commitId1, commitId2, 0, splitPointMap);
        String splitPointId = splitPointMap.firstEntry().getValue();
        return lookupObj(splitPointId, Commit.class);
    }

    private static void lookupSplitPointCommitHelper(String id1, String id2,
                                                     String commitId1, String commitId2,
                                                     int depth,
                                                     TreeMap<Integer, String> splitPointMap) {
        if (Objects.equals(id1, id2)) {
            splitPointMap.put(depth, id1);
            return;
        }
        Commit commit1 = lookupObj(id1, Commit.class);
        Commit commit2 = lookupObj(id2, Commit.class);
        List<String> parentIdList1 = commit1.getParentIds();
        if (parentIdList1.isEmpty()) {
            parentIdList1.add(commitId2);
        }
        List<String> parentIdList2 = commit2.getParentIds();
        if (parentIdList2.isEmpty()) {
            parentIdList2.add(commitId1);
        }
        for (String parentId1 : parentIdList1) {
            for (String parentId2 : parentIdList2) {
                lookupSplitPointCommitHelper(parentId1, parentId2, commitId1, commitId2,
                        depth + 1, splitPointMap);
            }
        }
    }

    /**
     * Lookup the blob with filePath from the given commitId,
     * throw error if the blob not exists.
     */
    static Blob lookupBlob(String commitId, String filePath) {
        Commit commit = lookupObj(commitId, Commit.class);
        Tree tree = lookupObj(commit.getTreeId(), Tree.class);
        Blob blob = lookupBlobHelper(tree, pathToParts(relativePath(filePath)));
        if (Objects.isNull(blob)) {
            throw error("Blob does not exist in Commit");
        }
        return blob;
    }

    private static Blob lookupBlobHelper(Tree tree, List<String> parts) {
        if (parts.size() < 1) {
            return null;
        }
        String part = parts.get(0);
        if (tree.getEntryMap().containsKey(part)) {
            Tree.Entry entry = tree.getEntryMap().get(part);
            if (entry.isBlob() && parts.size() == 1) {
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
            File file = pathToFile(partsToPath(parts));
            if (!dstTree.getEntryMap().containsKey(srcEntry.name)) {
                // Remove files or directories that are not in dstTree.
                deleteFileOrDir(file);
            }
            parts.remove(parts.size() - 1);
        }
        for (Tree.Entry dstEntry : dstTree.getEntryMap().values()) {
            parts.add(dstEntry.name);
            File file = pathToFile(partsToPath(parts));
            if (!srcTree.getEntryMap().containsKey(dstEntry.name)) {
                // Restore files or directories that are not in srcTree.
                if (dstEntry.isBlob()) {
                    Blob blob = lookupObj(dstEntry.id, Blob.class);
                    restoreFile(file, blob);
                } else {
                    restoreDir(parts, dstTree);
                }
            } else {
                Tree.Entry srcEntry = srcTree.getEntryMap().get(dstEntry.name);
                if (Objects.equals(srcEntry.type, dstEntry.type)) {
                    if (srcEntry.isTree()) {
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
                    if (srcEntry.isBlob()) {
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
        File dir = pathToFile(partsToPath(parts));
        if (!dir.exists()) {
            createDir(dir);
        }
        for (Tree.Entry entry : tree.getEntryMap().values()) {
            parts.add(entry.name);
            File file = pathToFile(partsToPath(parts));
            if (entry.isBlob()) {
                Blob blob = lookupObj(entry.id, Blob.class);
                restoreFile(file, blob);
            } else {
                Tree childTree = lookupObj(entry.id, Tree.class);
                restoreDir(parts, childTree);
            }
            parts.remove(parts.size() - 1);
        }
    }

    static void restoreFile(String path, String blobId) {
        Blob blob = lookupObj(blobId, Blob.class);
        restoreFile(pathToFile(path), blob);
    }

    static void restoreFile(File file, Blob blob) {
        writeFile(file, blob.getContent());
    }

    /**
     * Replace the contents of the conflicted file with curBlobId and givenBlobId,
     * curBlobId or givenBlobId can be null.
     */
    static void writeConflictFile(String path, String curBlobId, String givenBlobId) {
        String curContent = Objects.nonNull(curBlobId)
                ? lookupObj(curBlobId, Blob.class).getContent() : "";
        String givenContent = Objects.nonNull(givenBlobId)
                ? lookupObj(givenBlobId, Blob.class).getContent() : "";
        String content = String.format("<<<<<<< HEAD\n"
                        + "%s"
                        + "=======\n"
                        + "%s"
                        + ">>>>>>>\n",
                curContent, givenContent);
        writeFile(pathToFile(path), content);
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

    static Blob saveFileAsBlob(String path) {
        return saveFileAsBlob(pathToFile(path));
    }

    /**
     * Create a blob of the file and save it to object database,
     * return the saved blob.
     */
    static Blob saveFileAsBlob(File file) {
        Blob blob = createBlob(file);
        putObj(blob);
        return blob;
    }

    static Blob createBlob(String path) {
        return createBlob(pathToFile(path));
    }

    static Blob createBlob(File file) {
        return new Blob(readContentsAsString(file));
    }

    static String idFromPrefix(String prefixOfId) {
        int length = prefixOfId.length();
        if (length == 40) {
            return prefixOfId;
        } else if (length >= 2 && length < 40) {
            String dirName = prefixOfId.substring(0, 2);
            String fileName = prefixOfId.substring(2);
            List<String> dirNames = Arrays.stream(listFiles(OBJECT_DIR))
                    .map(File::getName).collect(Collectors.toList());
            if (dirNames.contains(dirName)) {
                List<String> fileNames = plainFilenamesIn(join(OBJECT_DIR, dirName));
                if (Objects.nonNull(fileNames)) {
                    if (length == 2 && fileNames.size() == 1) {
                        return dirName + fileNames.get(0);
                    }
                    List<String> matchedFileNames = fileNames.stream()
                            .filter(f -> f.startsWith(fileName)).collect(Collectors.toList());
                    if (matchedFileNames.size() == 1) {
                        return dirName + matchedFileNames.get(0);
                    }
                }
            }
        }
        throw error("Incorrect prefix.");
    }

    /* FILE UTILS */

    static File[] listFiles(File dir) {
        return listFiles(dir, DEFAULT_IGNORE_FILES);
    }

    static File[] listFiles(File dir, Set<String> ignoreFiles) {
        return dir.listFiles((d, name) ->
                !ignoreFiles.contains(Utils.join(d, name).getName()));
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
     * Convert path to file.
     */
    static File pathToFile(String filePath) {
        return join(CWD, relativePath(filePath));
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

    static List<String> pathToParts(String filePath) {
        Path path = Paths.get(relativePath(filePath));
        return StreamSupport.stream(path.spliterator(), false)
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    static String partsToPath(List<String> parts) {
        return relativePath(join(CWD, parts.toArray(String[]::new)));
    }

    static void createDir(File dir) {
        dir.mkdir();
    }

    /**
     * Write content to file, will create parent directories as needed.
     */
    static void writeFile(File file, String content) {
        createParentDirs(file);
        writeContents(file, content);
    }

    /**
     * Create parent directories of file as needed.
     */
    static void createParentDirs(File file) {
        List<String> parts = pathToParts(relativePath(file));
        if (parts.size() >= 2) {
            String parentDir = partsToPath(parts.subList(0, parts.size() - 1));
            try {
                Files.createDirectories(Paths.get(parentDir));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static void deleteFile(File file) {
        file.delete();
    }

    /**
     * Delete a file or delete a directory recursively.
     */
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
