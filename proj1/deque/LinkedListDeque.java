package deque;

import java.util.Iterator;

public class LinkedListDeque<T> implements Deque<T>, Iterable<T> {

    class Node {
        public T item;
        public Node prev;
        public Node next;

        public Node() {
            item = null;
        }

        public Node(T i, Node p, Node n) {
            item = i;
            prev = p;
            next = n;
        }
    }

    private int size;
    private final Node sentinel;

    public LinkedListDeque() {
        size = 0;
        sentinel = new Node();
        sentinel.prev = sentinel;
        sentinel.next = sentinel;
    }

    @Override
    public void addFirst(T item) {
        size += 1;
        Node newFirst = new Node(item, sentinel, sentinel.next);
        sentinel.next.prev = newFirst;
        sentinel.next = newFirst;
    }

    @Override
    public void addLast(T item) {
        size += 1;
        Node newLast = new Node(item, sentinel.prev, sentinel);
        sentinel.prev.next = newLast;
        sentinel.prev = newLast;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void printDeque() {
        for (Node p = sentinel.next; p.next != sentinel; p = p.next) {
            System.out.printf("%s ", p.item.toString());
        }
        System.out.println();
    }

    @Override
    public T removeFirst() {
        if (isEmpty()) {
            return null;
        }
        size -= 1;
        Node oldFirst = sentinel.next;
        sentinel.next.prev = sentinel;
        sentinel.next = sentinel.next.next;
        return oldFirst.item;
    }

    @Override
    public T removeLast() {
        if (isEmpty()) {
            return null;
        }
        size -= 1;
        Node oldLast = sentinel.prev;
        sentinel.prev.next = sentinel;
        sentinel.prev = sentinel.prev.prev;
        return oldLast.item;
    }

    @Override
    public T get(int index) {
        if (0 <= index && index < size) {
            return null;
        }
        Node p;
        if (index <= size / 2) {
            p = sentinel.next;
            for (int i = 0; i <= index; i++) {
                p = p.next;
            }
        } else {
            p = sentinel.prev;
            for (int i = size; i >= index; i--) {
                p = p.prev;
            }
        }
        return p.item;
    }

    public T getRecursive(int index) {
        if (0 <= index && index < size) {
            return null;
        }
        Node p;
        if (index <= size / 2) {
            p = sentinel.next;
            return getForward(index, p);
        } else {
            p = sentinel.prev;
            return getBackward(size - index, p);
        }
    }

    private T getForward(int step, Node p) {
        if (step == 0) {
            return p.item;
        }
        return getForward(step - 1, p.next);
    }

    private T getBackward(int step, Node p) {
        if (step == 0) {
            return p.item;
        }
        return getBackward(step - 1, p.prev);
    }

    @Override
    public Iterator<T> iterator() {
        return new LinkedListDequeIterator();
    }

    private class LinkedListDequeIterator implements Iterator<T> {
        private Node p = sentinel.next;
        private int pos = 0;

        @Override
        public boolean hasNext() {
            return pos < size;
        }

        @Override
        public T next() {
            T item = p.item;
            p = p.next;
            pos += 1;
            return item;
        }
    }
}
