# Gitlet Design Document

Source code: [gitlet](gitlet)

## Classes and Data Structures

### Blob

A `blob` object is used to store file data.

### Tree

A `tree` object is used to store directory information.

Example content:

```text
blob    <id1>    file1.txt
tree    <id2>    dir1
```

### Commit

A `commit` object points to a single `tree` object, and contains other meta-information.

### Index

The `index` containing a tree of cached files in current working tree. It has these properties:

- The index contains all the information necessary to generate a single `tree` object.
- The index can compare between the `tree` object and the working tree.

Example content:

```text
                   CWD
                 /     \
                /       \
               /         \
              /           \
            dir1         file1.txt, 
           /   \           <id1>
          /     \
         /       \
        /         \
   file2.txt,  file3.txt,
     <id2>      <id3>
```

### Branch

A `branch` containing a pointer to a branch.

### Head

The `head` containing a pointer to current branch.
According to the specification, in Gitlet, we will never be in a detached HEAD state since there is
no `checkout` command that will move the HEAD pointer to a specific commit.

## Algorithms

### Finding split point

The split point is the **latest common ancestor** of two branch heads.

```text
      A---B---C---D  other
     /         \
E---F---G---H---I---J  main
```

In the above example, the split point of `main` and `other` is `C`.

We can treat this problem as get first **intersection of two linked lists** if each node has only
one parent node, but in git, a commit node may have multiple parent nodes. Currently, I just
use `DFS` to get all intersections of two branches. And the intersection with minimum `step` is
the split point. This method is slow and may need refactoring.

### Comparing two trees

To get the modified, added and deleted files between two commits, We need to compare
the two trees of these two commits.

```text
                   CWD                                     CWD
                 /     \                                 /     \
                /       \                               /       \
               /         \                             /         \
              /           \                           /           \
            dir1         file1.txt,                 dir1         file1.txt,
           <id4>           <id1>                   <id7>           <id5>
           /   \                                   /   \
          /     \                                 /     \
         /       \                               /       \
        /         \                             /         \
   file2.txt,  file3.txt,                  file2.txt,  file4.txt,
     <id2>       <id3>                       <id6>       <id3>
     
                fromTree                                 toTree
```

In the above example, the modified files are `["file1.txt", "dir1/file2.txt"]`, the added files
are `["dir1/file4.txt"]`, the deleted files are `["dir1/file3.txt"]`(actually `"dir1/file3.txt"` is
renamed, but in Gitlet we treat it as being deleted and then added).

We can write a recursive function to compare the `fromTree` and `toTree`,
start with `diff(fromNode, toNode)` (some helpful arguments are not represented):

- If `fromNode` is null and `toNode` is not null.
    - If `toNode` is a blob, add it to `addedFiles`.
    - Else, call `diff(null, childToNode)` for each childNode in `childToNodes`.
- Else, if `fromNode` is not null and `toNode` is null.
    - If `fromNode` is a blob, add it to `deletedFiles`.
    - Else, call `diff(childFromNode, null)` for each childNode in `childFromNodes`.
- Else, if both nodes exist (Note that when calling this function, we will ensure that two existing
  nodes have the same name):
    - If two nodes have the same id, we can just return.
    - Else, if both nodes are blobs, add `toNode` to `modifiedFiles`.
    - Else, if both nodes are trees:
        - call `diff(childFromNode, childToNode)` for each childNodes with same name.
        - call `diff(childFromNode, null)` for each childNode only present in `childFromNodes`.
        - call `diff(null, childToNode)` for each childNode only present in `childToNodes`.
    - Else:
        - call `diff(fromNode, null)`.
        - call `diff(null, toNode)`.

## Persistence

```text
CWD/                            # Current working directory
  .gitlet/                      # All persistence data for Gitlet
    HEAD                        # Pointer to current branch
    index                       # Index as a staging area
    objects/[0-9a-f][0-9a-f]/   # Objects (blobs, trees, commits)
    refs/                       # References
      heads/<name>              # Pointers to branches
      global-log                # Pointers to all commits (DOES NOT exist in real git)
```

## References

- [Gitlet Specification](https://sp21.datastructur.es/materials/proj/proj2/proj2)
- [Git Internals - Git Objects](https://book.git-scm.com/book/en/v2/Git-Internals-Git-Objects)
- [Git Repository Layout](https://git-scm.com/docs/gitrepository-layout)
