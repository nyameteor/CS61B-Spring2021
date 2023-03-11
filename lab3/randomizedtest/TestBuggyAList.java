package randomizedtest;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by hug.
 */
public class TestBuggyAList {
    @Test
    public void testThreeAddThreeRemove() {
        AListNoResizing<Integer> AL = new AListNoResizing<>();
        BuggyAList<Integer> BL = new BuggyAList<>();
        Integer[] items = new Integer[]{4, 5, 6};
        for (Integer item : items) {
            AL.addLast(item);
            BL.addLast(item);
        }
        for (int i = 0; i < items.length; i++) {
            assertEquals(AL.removeLast(), BL.removeLast());
        }
    }

    @Test
    public void randomizedTest() {
        AListNoResizing<Integer> AL = new AListNoResizing<>();
        BuggyAList<Integer> BL = new BuggyAList<>();

        int N = 5000;
        for (int i = 0; i < N; i += 1) {
            int operationNumber = StdRandom.uniform(0, 4);
            if (operationNumber == 0) {
                // addLast
                int randVal = StdRandom.uniform(0, 100);
                AL.addLast(randVal);
                BL.addLast(randVal);
            } else if (operationNumber == 1) {
                // size
                assertEquals(AL.size(), BL.size());
            } else if (operationNumber == 2) {
                // getLast
                if (AL.size() == 0) {
                    continue;
                }
                assertEquals(AL.getLast(), BL.getLast());
            } else if (operationNumber == 3) {
                // removeLast
                if (AL.size() == 0) {
                    continue;
                }
                assertEquals(AL.removeLast(), BL.removeLast());
            }
        }
    }
}
