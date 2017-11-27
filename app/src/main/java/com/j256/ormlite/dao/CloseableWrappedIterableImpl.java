package com.j256.ormlite.dao;

import java.io.IOException;

import com.j256.ormlite.misc.IOUtils;

/**
 * Class which is used to help folks use for loops but still close at the end. This is a wrapper to allow multiple
 * threads to iterate across the same dao or the same lazy collection at the same time. See
 * {@link Dao#getWrappedIterable()} or {@link ForeignCollection#getWrappedIterable()}.
 * 
 * @author graywatson
 */
public class CloseableWrappedIterableImpl<T> implements CloseableWrappedIterable<T> {

	private final CloseableIterable<T> iterable;
	private CloseableIterator<T> iterator;

	public CloseableWrappedIterableImpl(CloseableIterable<T> iterable) {
		this.iterable = iterable;
	}

	@Override
	public CloseableIterator<T> iterator() {
		return closeableIterator();
	}

	@Override
	public CloseableIterator<T> closeableIterator() {
		IOUtils.closeQuietly(this);
		iterator = iterable.closeableIterator();
		return iterator;
	}

	@Override
	public void close() throws IOException {
		if (iterator != null) {
			iterator.close();
			iterator = null;
		}
	}
}
