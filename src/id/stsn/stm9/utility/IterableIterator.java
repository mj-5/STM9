package id.stsn.stm9.utility;

import java.util.Iterator;

public class IterableIterator<T> implements Iterable<T> {
    private Iterator<T> mIter;

    public IterableIterator(Iterator<T> iter) {
        mIter = iter;
    }

    public Iterator<T> iterator() {
        return mIter;
    }
}
