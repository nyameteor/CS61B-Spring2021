package tester;

import static org.junit.Assert.*;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;
import student.StudentArrayDeque;

public class TestArrayDequeEC {
    @Test
    public void randomizedTest() {
        ArrayDequeSolution<Integer> ad = new ArrayDequeSolution<>();
        StudentArrayDeque<Integer> sad = new StudentArrayDeque<>();

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        int N = 5000;
        for (int i = 0; i < N; i++) {
            int operationNumber = StdRandom.uniform(0, 7);
            if (operationNumber == 0) {
                // addFirst
                int randVal = StdRandom.uniform(0, 100);
                sb.append(String.format("addFirst(%d)\n", randVal));
                ad.addFirst(randVal);
                sad.addFirst(randVal);
            } else if (operationNumber == 1) {
                // addLast
                int randVal = StdRandom.uniform(0, 100);
                sb.append(String.format("addLast(%d)\n", randVal));
                ad.addLast(randVal);
                sad.addLast(randVal);
            } else if (operationNumber == 2) {
                // isEmpty
                sb.append("isEmpty()\n");
                assertEquals(sb.toString(), ad.isEmpty(), sad.isEmpty());
            } else if (operationNumber == 3) {
                // size
                sb.append("size()\n");
                assertEquals(sb.toString(), ad.size(), sad.size());
            } else if (operationNumber == 4) {
                // removeFirst
                if (ad.isEmpty()) {
                    continue;
                }
                sb.append("removeFirst()\n");
                assertEquals(sb.toString(), ad.removeFirst(), sad.removeFirst());
            } else if (operationNumber == 5) {
                // removeLast
                if (ad.isEmpty()) {
                    continue;
                }
                sb.append("removeLast()\n");
                assertEquals(sb.toString(), ad.removeLast(), sad.removeLast());
            } else if (operationNumber == 6) {
                // get
                if (ad.isEmpty()) {
                    continue;
                }
                sb.append("get()\n");
                int randIndex = StdRandom.uniform(0, ad.size());
                assertEquals(sb.toString(), ad.get(randIndex), sad.get(randIndex));
            }
        }
    }
}
