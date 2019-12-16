package cz.jaro.alarmmorning;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RecentListTest {

    private final int A = 100;
    private final int B = 200;
    private final int C = 300;
    private final int D = 400;

    @Test
    public void t100_add_1() {
        RecentList<Integer> list = new RecentList<>(Integer.class);
        assertTrue(list.isEmpty());

        list.add(A);
        assertFalse("isEmpty", list.isEmpty());
        assertThat("get", list.get(0), is(A));
        assertThat("size", list.size(), is(1));

        list.clear();
        assertTrue("isEmpty", list.isEmpty());
    }

    @Test
    public void t110_add_2() {
        RecentList<Integer> list = new RecentList<>(Integer.class);
        assertTrue("isEmpty", list.isEmpty());

        list.add(A);

        list.add(B);
        assertFalse("isEmpty", list.isEmpty());
        assertThat("get", list.get(1), is(B));
        assertThat("size", list.size(), is(2));

        list.clear();
        assertTrue("isEmpty", list.isEmpty());
    }

    @Test
    public void t200_removeFirst() {
        RecentList<Integer> list = new RecentList<>(Integer.class);

        list.add(A);
        assertThat("removeFirst", list.removeFirst(), is(A));
        assertTrue("isEmpty", list.isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void t210_removeFirst_empty() {
        RecentList<Integer> list = new RecentList<>(Integer.class);

        list.removeFirst();
    }

    @Test
    public void t300_set_get() {
        RecentList<Integer> list = new RecentList<>(Integer.class);

        list.add(A);
        list.add(B);
        assertThat("get(0)", list.get(0), is(A));
        assertThat("get(1)", list.get(1), is(B));

        list.set(0, C);
        assertThat("get(0)", list.get(0), is(C));
        assertThat("get(1)", list.get(1), is(B));

        list.set(0, D);
        assertThat("get(0)", list.get(0), is(D));
        assertThat("get(1)", list.get(1), is(B));

        list.set(1, C);
        assertThat("get(0)", list.get(0), is(D));
        assertThat("get(1)", list.get(1), is(C));

        assertFalse("isEmpty", list.isEmpty());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void t350_get_outOfBounds_1() {
        RecentList<Integer> list = new RecentList<Integer>(Integer.class);

        list.get(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void t351_get_outOfBounds_2() {
        RecentList<Integer> list = new RecentList<Integer>(Integer.class);

        list.get(0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void t352_get_outOfBounds_3() {
        RecentList<Integer> list = new RecentList<Integer>(Integer.class);

        list.add(B);
        list.get(1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void t360_set_outOfBounds_1() {
        RecentList<Integer> list = new RecentList<Integer>(Integer.class);

        list.set(-1, A);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void t361_set_outOfBounds_2() {
        RecentList<Integer> list = new RecentList<Integer>(Integer.class);

        list.set(0, A);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void t362_set_outOfBounds_3() {
        RecentList<Integer> list = new RecentList<Integer>(Integer.class);

        list.add(B);
        list.set(1, A);
    }

    @Test
    public void t400_iterator() {
        RecentList<Integer> list = new RecentList<>(Integer.class);

        int count = 0;
        for (int a : list) {
            count++;
        }

        assertThat(count, is(0));
    }

    @Test
    public void t410_iterator() {
        RecentList<Integer> list = new RecentList<>(Integer.class);

        list.add(A);

        int count = 0;
        for (int a : list) {
            assertThat("iter 0", a, is(A));
            count++;
        }

        assertThat(count, is(1));
    }

    @Test
    public void t420_iterator() {
        RecentList<Integer> list = new RecentList<>(Integer.class);

        list.add(A);
        list.add(B);

        int count = 0;
        for (int a : list) {
            if (count == 0)
                assertThat("iter " + count, a, is(A));
            else if (count == 1)
                assertThat("iter " + count, a, is(B));
            count++;
        }

        assertThat(count, is(2));
    }

    @Test
    public void t500_rotation() {
        RecentList<Integer> list = new RecentList<>(Integer.class);

        final int MAX = 100;

        for (int i = 0; i < MAX; i += 2) {
            assertThat("size", list.size(), is(i / 2));

            list.add(i);
            assertThat("get", list.get(i / 2), is(i));
            assertThat("size", list.size(), is(i / 2 + 1));

            list.add(i + 1);
            assertThat("get", list.get(i / 2 + 1), is(i + 1));
            assertThat("size", list.size(), is(i / 2 + 2));

            assertThat("removeFirst", list.removeFirst(), is(i / 2));
            assertThat("size", list.size(), is(i / 2 + 1));
        }

        for (int i = MAX; 0 < i; i -= 2) {
            assertThat("size", list.size(), is(i / 2));

            int a = MAX + (MAX - i) / 2;
            list.add(a);
            assertThat("get", list.get(i / 2), is(a));
            assertThat("size", list.size(), is(i / 2 + 1));

            assertThat("removeFirst", list.removeFirst(), is(MAX / 2 + (MAX - i)));
            assertThat("removeFirst", list.removeFirst(), is(MAX / 2 + (MAX - i) + 1));
            assertThat("size", list.size(), is(i / 2 - 1));
        }

        assertTrue("isEmpty", list.isEmpty());
    }

}
