package bstmap;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

public class BST<Key extends Comparable<Key>, Value> implements Iterable<Key> {

    private class Node {
        private Key key;
        private Value val;
        private Node left, right;

        public Node(Key key, Value val) {
            this(key, val, null, null);
        }

        public Node(Key key, Value val, Node left, Node right) {
            this.key = key;
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    private Node root;

    public Boolean contains(Key key) {
        return find(root, key) != null;
    }

    public Value find(Key key) {
        Node x = find(root, key);
        if (x == null) {
            return null;
        }
        return x.val;
    }

    private Node find(Node x, Key key) {
        if (x == null) {
            return null;
        }
        int cmp = key.compareTo(x.key);
        if (cmp < 0) {
            return find(x.left, key);
        } else if (cmp > 0) {
            return find(x.right, key);
        } else {
            return x;
        }
    }

    public void insert(Key key, Value val) {
        root = insert(root, key, val);
    }

    private Node insert(Node x, Key key, Value val) {
        if (x == null) {
            return new Node(key, val);
        }
        int cmp = key.compareTo(x.key);
        if (cmp < 0) {
            x.left = insert(x.left, key, val);
        } else if (cmp > 0) {
            x.right = insert(x.right, key, val);
        } else {
            x.val = val;
        }
        return x;
    }

    public void delete(Key key) {
        root = delete(root, key);
    }

    private Node delete(Node x, Key key) {
        if (x == null) {
            return null;
        }
        int cmp = key.compareTo(x.key);
        if (cmp < 0) {
            x.left = delete(x.left, key);
        } else if (cmp > 0) {
            x.right = delete(x.right, key);
        } else {
            if (x.left == null && x.right == null) {
                // no children
                x = null;
            } else if (x.left == null) {
                // 1 child
                x = x.right;
            } else if (x.right == null) {
                // 1 child
                x = x.left;
            } else {
                // 2 children
                Node t = x;
                // find successor
                Node s = findMin(t.right);
                x.right = deleteMin(t.right);
                s.left = x.left;
                s.right = x.right;
                x = s;
            }
        }
        return x;
    }

    private Node findMin(Node x) {
        if (x == null) {
            throw new NoSuchElementException();
        }
        if (x.left == null) {
            return x;
        } else {
            return findMin(x.left);
        }
    }

    private Node findMax(Node x) {
        if (x == null) {
            throw new NoSuchElementException();
        }
        if (x.right == null) {
            return x;
        } else {
            return findMax(x.right);
        }
    }

    private Node deleteMin(Node x) {
        if (x == null) {
            return null;
        }
        if (x.left == null) {
            if (x.right == null) {
                x = null;
            } else {
                x = x.right;
            }
        } else {
            x = deleteMin(x.left);
        }
        return x;
    }

    private Node deleteMax(Node x) {
        if (x == null) {
            return null;
        }
        if (x.right == null) {
            if (x.left == null) {
                x = null;
            } else {
                x = x.left;
            }
        } else {
            x = deleteMax(x.right);
        }
        return x;
    }

    @Override
    public Iterator<Key> iterator() {
        return new BSTIterator();
    }

    private class BSTIterator implements Iterator<Key> {
        private Node x;
        private Stack<Node> st;
        private Boolean canPushLeft;

        public BSTIterator() {
            x = root;
            st = new Stack<>();
            st.push(x);
            canPushLeft = true;
        }

        @Override
        public boolean hasNext() {
            return !st.empty();
        }

        @Override
        public Key next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Node t = st.peek();
            if (canPushLeft) {
                while (t.left != null) {
                    st.push(t.left);
                    t = st.peek();
                }
                canPushLeft = false;
            }
            t = st.pop();
            if (t.right != null) {
                st.push(t.right);
                canPushLeft = true;
            }
            return t.key;
        }
    }
}
