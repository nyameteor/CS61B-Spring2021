package deque;

import java.util.Comparator;

import org.junit.Test;

import static org.junit.Assert.*;


public class MaxArrayDequeTest {

    @Test
    public void initialComparatorTest() {
        Dog d1 = new Dog("Elyse", 3);
        Dog d2 = new Dog("Sture", 9);
        Dog d3 = new Dog("Benjamin", 15);
        MaxArrayDeque<Dog> ad1 = new MaxArrayDeque<>(Dog.getSizeComparator());
        ad1.addLast(d1);
        ad1.addLast(d2);
        ad1.addLast(d3);
        assertEquals(d3, ad1.max());
    }

    @Test
    public void customComparatorTest() {
        Dog d1 = new Dog("Elyse", 3);
        Dog d2 = new Dog("Sture", 9);
        Dog d3 = new Dog("Benjamin", 15);
        MaxArrayDeque<Dog> ad1 = new MaxArrayDeque<>(Dog.getSizeComparator());
        ad1.addLast(d1);
        ad1.addLast(d2);
        ad1.addLast(d3);
        assertEquals(d2, ad1.max(Dog.getNameComparator()));
    }

    /**
     * Dog class from lecture code
     */
    private static class Dog {
        private String name;
        private int size;

        public Dog(String n, int s) {
            name = n;
            size = s;
        }

        public static Comparator<Dog> getNameComparator() {
            return new NameComparator();
        }

        private static class NameComparator implements Comparator<Dog> {
            @Override
            public int compare(Dog o1, Dog o2) {
                return o1.name.compareTo(o2.name);
            }
        }

        public static Comparator<Dog> getSizeComparator() {
            return new SizeComparator();
        }

        private static class SizeComparator implements Comparator<Dog> {
            @Override
            public int compare(Dog o1, Dog o2) {
                return o1.size - o2.size;
            }
        }
    }
}
