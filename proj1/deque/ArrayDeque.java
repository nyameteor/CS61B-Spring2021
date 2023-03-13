package deque;

public class ArrayDeque<T> implements Deque<T> {

    // The index to insert next first element
    private int frontIndex;
    // The index to insert next last element
    private int backIndex;
    private int size;
    private T[] items;

    public ArrayDeque() {
        items = (T[]) new Object[100];
        frontIndex = items.length - 1;
        backIndex = 0;
    }

    private void conditionalExtend() {
        final int EXTEND_REFACTOR = 2;
        if (size == items.length) {
            resize(items.length * EXTEND_REFACTOR);
        }
    }

    private void conditionalShrink() {
        final int SHRINK_REFACTOR = 4;
        if ((size < items.length / SHRINK_REFACTOR) && (size > SHRINK_REFACTOR)) {
            resize(items.length / SHRINK_REFACTOR);
        }
    }

    private void resize(int capacity) {
        T[] a = (T[]) new Object[capacity];
        int i = 0;
        for (; i < size; i++) {
            a[i] = items[(frontIndex + 1 + i) % items.length];
        }
        items = a;
        frontIndex = capacity - 1;
        backIndex = i;
    }

    private int moveForward(int index) {
        index += 1;
        if (index >= items.length) {
            index = index % items.length;
        }
        return index;
    }

    private int moveBackward(int index) {
        index -= 1;
        if (index < 0) {
            index = index + items.length;
        }
        return index;
    }

    @Override
    public void addFirst(T item) {
        conditionalExtend();
        size += 1;
        items[frontIndex] = item;
        frontIndex = moveBackward(frontIndex);
    }

    @Override
    public void addLast(T item) {
        conditionalExtend();
        size += 1;
        items[backIndex] = item;
        backIndex = moveForward(backIndex);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void printDeque() {
        for (int i = 0; i < size(); i++) {
            System.out.printf("%s ", items[(frontIndex + 1 + i) % items.length]);
        }
        System.out.println();
    }

    @Override
    public T removeFirst() {
        if (isEmpty()) {
            return null;
        }
        conditionalShrink();
        size -= 1;
        frontIndex = moveForward(frontIndex);
        T x = items[frontIndex];
        items[frontIndex] = null;
        return x;
    }

    @Override
    public T removeLast() {
        if (isEmpty()) {
            return null;
        }
        conditionalShrink();
        size -= 1;
        backIndex = moveBackward(backIndex);
        T x = items[backIndex];
        items[backIndex] = null;
        return x;
    }

    @Override
    public T get(int index) {
        if (0 <= index && index < size) {
            return items[(frontIndex + 1 + index) % items.length];
        } else {
            return null;
        }
    }
}
