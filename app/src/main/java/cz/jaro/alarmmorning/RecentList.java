package cz.jaro.alarmmorning;

import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * This class implements a List interface that handles operations with time series data effectively.
 * <p>
 * Example of typical use case: The sensor generates several values per second. We have to store all the values in the past X seconds to be able to detect gestures. In practice, we doing the following operations:
 * <br>1. are adding new values at the end of list,
 * <br>2. purging the old values from the beginning of the list,
 * <br>3. iterating over the stored values to detect the gesture.
 * <p>
 * The time complexity of key operations "add value at the end" and "remove oldest value" is O(1).  This is rather amortized time complexity as the size of array which stores the values grows or shrinks according to the count of values (invariant: there is between 25% and 100% occupied  places in the array that stores the values).
 *
 * @param <T> Type of the stored elements.
 */
public final class RecentList<T> implements Iterable<T> { // TODO Extend AbstractList, support for time indexed data like purgeValuesOlderThan(Calendar calendar) or iterator over the values in past X seconds
    private final Class<T> cls;
    private T[] values;
    private int from = 0;
    private int to = 0;

    /*
                   0   1   2   3             Index used in the get/set functions
           0   1   2   3   4   5   6   7     Index in the values array
         ---------------------------------
values   |   |   | A | B | C | D |   |   |
         ---------------------------------
                   ^               ^
                   |               |
                   from            to

Invariant: There is always at least one null value in the values array.
    */

    public RecentList(Class<T> cls) {
        this(cls, 1);
    }

    @SuppressWarnings("unchecked")
    public RecentList(Class<T> cls, int size) {
        // Use Array native method to create array of a type only known at run time
        values = (T[]) Array.newInstance(cls, size);
        this.cls = cls;
    }

    public final void add(T element) {
        if (size() == values.length - 1) {
            expandArray();
        }

        int to2 = to < values.length - 1 ? to + 1 : 0;
        values[to] = element;
        to = to2;
    }

    public final T removeFirst() {
        if (isEmpty()) {
            throw new IllegalStateException("The list is empty");
        } else {
            if (size() < values.length / 4) {
                shrinkArray();
            }

            int from2 = from < values.length - 1 ? from + 1 : 0;
            T res = values[from];
            values[from] = null;
            from = from2;

            return res;
        }
    }

    public final void clear() {
        if (from <= to) {
            for (int i = from; i <= to; ++i) {
                values[i] = null;
            }
        } else {
            for (int i = 0; i <= to; ++i) {
                values[i] = null;
            }
            for (int i = from; i < values.length; ++i) {
                values[i] = null;
            }
        }

        from = 0;
        to = 0;
    }

    public final T get(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index " + index + " may not be negative");
        } else if (size() <= index) {
            throw new IndexOutOfBoundsException("Index " + index + " is bigger than the array size " + size());
        } else {
            int i = from + index;
            if (values.length <= i) {
                i -= values.length;
            }

            return values[i];
        }
    }

    public final void set(int index, T element) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index " + index + " may not be negative");
        } else if (size() <= index) {
            throw new IndexOutOfBoundsException("Index " + index + " is bigger than the array size " + size());
        } else {
            int i = from + index;
            if (values.length < i) {
                i -= values.length;
            }

            values[i] = element;
        }
    }

    public final boolean isEmpty() {
        return to == from;
    }

    public final int size() {
        int size = to - from;
        return 0 <= size ? size : size + values.length;
    }

    @SuppressWarnings("unchecked")
    private void expandArray() {
        T[] values2 = (T[]) Array.newInstance(cls, 2 * values.length);
        copyAndUse(values2);
    }

    @SuppressWarnings("unchecked")
    private void shrinkArray() {
        T[] values2 = (T[]) Array.newInstance(cls, values.length / 2);
        copyAndUse(values2);
    }

    private void copyAndUse(T[] values2) {
        int j = 0;

        if (from <= to) {
            for (int i = from; i <= to; ++i) {
                values2[j++] = values[i];
            }
        } else {
            for (int i = from; i < values.length; ++i) {
                values2[j++] = values[i];
            }
            for (int i = 0; i <= to; ++i) {
                values2[j++] = values[i];
            }
        }

        to = size();
        from = 0;
        values = values2;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iter();
    }

    private class Iter implements Iterator<T> {
        private int pos = from; // position of the next element

        @Override
        public boolean hasNext() {
            return from <= to
                    ? pos < to
                    : from <= pos || pos < to;
        }

        @Override
        public T next() {
            int posOld = pos;
            pos = pos < values.length - 1 ? pos + 1 : 0;
            return values[posOld];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove not supported");
        }
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("[");
        int i = 0;

        for (int var3 = values.length; i < var3; ++i) {
            if (0 < i) {
                str.append(", ");
            }

            if (i == from) {
                str.append(">>> ");
            }

            if (i == to) {
                str.append(" <<<");
            }

            str.append(values[i]);
        }

        str.append("]");
        return str.toString();
    }

}
