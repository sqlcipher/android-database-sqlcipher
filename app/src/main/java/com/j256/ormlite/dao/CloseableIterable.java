package com.j256.ormlite.dao;

/**
 * Extension to Iterable to provide a iterator() method that returns a {@link CloseableIterator}.
 * 
 * @author graywatson
 */
public interface CloseableIterable<T> extends Iterable<T> {

	/**
	 * Returns an iterator over a set of elements of type T which can be closed.
	 */
	public CloseableIterator<T> closeableIterator();
}
