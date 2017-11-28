package com.j256.ormlite.dao;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.misc.IOUtils;
import com.j256.ormlite.support.DatabaseConnection;

/**
 * Collection that is set on a field that as been marked with the {@link ForeignCollectionField} annotation when an
 * object is refreshed or queried (i.e. not created). Most of the methods here require a pass through the database.
 * Operations such as size() therefore should most likely not be used because of their expense. Chances are you only
 * want to use the {@link #iterator()}, {@link #toArray()}, and {@link #toArray(Object[])} methods.
 * 
 * <p>
 * <b>WARNING:</b> Most likely for(;;) loops should not be used here since we need to be careful about closing the
 * iterator.
 * </p>
 * 
 * @author graywatson
 */
public class LazyForeignCollection<T, ID> extends BaseForeignCollection<T, ID> implements Serializable {

	private static final long serialVersionUID = -5460708106909626233L;

	private transient CloseableIterator<T> lastIterator;

	/**
	 * WARNING: The user should not be calling this constructor. You should be using the
	 * {@link Dao#assignEmptyForeignCollection(Object, String)} or {@link Dao#getEmptyForeignCollection(String)} methods
	 * instead.
	 */
	public LazyForeignCollection(Dao<T, ID> dao, Object parent, Object parentId, FieldType foreignFieldType,
			String orderColumn, boolean orderAscending) {
		super(dao, parent, parentId, foreignFieldType, orderColumn, orderAscending);
	}

	/**
	 * The iterator returned from a lazy collection keeps a connection open to the database as it iterates across the
	 * collection. You will need to call {@link CloseableIterator#close()} or go all the way through the loop to ensure
	 * that the connection has been closed. You can also call {@link #closeLastIterator()} on the collection itself
	 * which will close the last iterator returned. See the reentrant warning.
	 */
	@Override
	public CloseableIterator<T> iterator() {
		return closeableIterator(DatabaseConnection.DEFAULT_RESULT_FLAGS);
	}

	@Override
	public CloseableIterator<T> iterator(int flags) {
		return closeableIterator(flags);
	}

	@Override
	public CloseableIterator<T> closeableIterator() {
		return closeableIterator(DatabaseConnection.DEFAULT_RESULT_FLAGS);
	}

	@Override
	public CloseableIterator<T> closeableIterator(int flags) {
		try {
			return iteratorThrow(flags);
		} catch (SQLException e) {
			throw new IllegalStateException("Could not build lazy iterator for " + dao.getDataClass(), e);
		}
	}

	@Override
	public CloseableIterator<T> iteratorThrow() throws SQLException {
		return iteratorThrow(DatabaseConnection.DEFAULT_RESULT_FLAGS);
	}

	@Override
	public CloseableIterator<T> iteratorThrow(int flags) throws SQLException {
		lastIterator = seperateIteratorThrow(flags);
		return lastIterator;
	}

	@Override
	public CloseableWrappedIterable<T> getWrappedIterable() {
		return getWrappedIterable(DatabaseConnection.DEFAULT_RESULT_FLAGS);
	}

	@Override
	public CloseableWrappedIterable<T> getWrappedIterable(final int flags) {
		return new CloseableWrappedIterableImpl<T>(new CloseableIterable<T>() {
			@Override
			public CloseableIterator<T> iterator() {
				return closeableIterator();
			}
			@Override
			public CloseableIterator<T> closeableIterator() {
				try {
					return LazyForeignCollection.this.seperateIteratorThrow(flags);
				} catch (Exception e) {
					throw new IllegalStateException("Could not build lazy iterator for " + dao.getDataClass(), e);
				}
			}
		});
	}

	@Override
	public void closeLastIterator() throws IOException {
		if (lastIterator != null) {
			lastIterator.close();
			lastIterator = null;
		}
	}

	@Override
	public boolean isEager() {
		return false;
	}

	@Override
	public int size() {
		CloseableIterator<T> iterator = iterator();
		try {
			int sizeC;
			for (sizeC = 0; iterator.hasNext(); sizeC++) {
				// move to next without constructing the object
				iterator.moveToNext();
			}
			return sizeC;
		} finally {
			IOUtils.closeQuietly(iterator);
		}
	}

	@Override
	public boolean isEmpty() {
		CloseableIterator<T> iterator = iterator();
		try {
			return !iterator.hasNext();
		} finally {
			IOUtils.closeQuietly(iterator);
		}
	}

	@Override
	public boolean contains(Object obj) {
		CloseableIterator<T> iterator = iterator();
		try {
			while (iterator.hasNext()) {
				if (iterator.next().equals(obj)) {
					return true;
				}
			}
			return false;
		} finally {
			IOUtils.closeQuietly(iterator);
		}
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		Set<Object> leftOvers = new HashSet<Object>(collection);
		CloseableIterator<T> iterator = iterator();
		try {
			while (iterator.hasNext()) {
				leftOvers.remove(iterator.next());
			}
			return leftOvers.isEmpty();
		} finally {
			IOUtils.closeQuietly(iterator);
		}
	}

	@Override
	public boolean remove(Object data) {
		CloseableIterator<T> iterator = iterator();
		try {
			while (iterator.hasNext()) {
				if (iterator.next().equals(data)) {
					iterator.remove();
					return true;
				}
			}
			return false;
		} finally {
			IOUtils.closeQuietly(iterator);
		}
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		boolean changed = false;
		CloseableIterator<T> iterator = iterator();
		try {
			while (iterator.hasNext()) {
				if (collection.contains(iterator.next())) {
					iterator.remove();
					changed = true;
				}
			}
			return changed;
		} finally {
			IOUtils.closeQuietly(iterator);
		}
	}

	@Override
	public Object[] toArray() {
		List<T> items = new ArrayList<T>();
		CloseableIterator<T> iterator = iterator();
		try {
			while (iterator.hasNext()) {
				items.add(iterator.next());
			}
			return items.toArray();
		} finally {
			IOUtils.closeQuietly(iterator);
		}
	}

	@Override
	public <E> E[] toArray(E[] array) {
		List<E> items = null;
		int itemC = 0;
		CloseableIterator<T> iterator = iterator();
		try {
			while (iterator.hasNext()) {
				@SuppressWarnings("unchecked")
				E castData = (E) iterator.next();
				// are we exceeding our capacity in the array?
				if (itemC >= array.length) {
					if (items == null) {
						items = new ArrayList<E>();
						for (E arrayData : array) {
							items.add(arrayData);
						}
					}
					items.add(castData);
				} else {
					array[itemC] = castData;
				}
				itemC++;
			}
		} finally {
			IOUtils.closeQuietly(iterator);
		}
		if (items == null) {
			if (itemC < array.length - 1) {
				array[itemC] = null;
			}
			return array;
		} else {
			return items.toArray(array);
		}
	}

	@Override
	public int updateAll() {
		throw new UnsupportedOperationException("Cannot call updateAll() on a lazy collection.");
	}

	@Override
	public int refreshAll() {
		throw new UnsupportedOperationException("Cannot call updateAll() on a lazy collection.");
	}

	@Override
	public int refreshCollection() {
		// no-op for lazy collections
		return 0;
	}

	/**
	 * This is just a call to {@link Object#equals(Object)}.
	 * 
	 * <p>
	 * NOTE: This method is here for documentation purposes because {@link EagerForeignCollection#equals(Object)} is
	 * defined.
	 * </p>
	 */
	@Override
	public boolean equals(Object other) {
		return super.equals(other);
	}

	/**
	 * This is just a call to {@link Object#hashCode()}.
	 * 
	 * <p>
	 * NOTE: This method is here for documentation purposes because {@link EagerForeignCollection#equals(Object)} is
	 * defined.
	 * </p>
	 */
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	/**
	 * NOTE: package perms to removed synthetic accessor
	 */
	CloseableIterator<T> seperateIteratorThrow(int flags) throws SQLException {
		// check state to make sure we have a DAO in case we have a deserialized collection
		if (dao == null) {
			throw new IllegalStateException(
					"Internal DAO object is null.  Maybe the collection was deserialized or otherwise constructed wrongly.  "
							+ "Use dao.assignEmptyForeignCollection(...) or dao.getEmptyForeignCollection(...) instead");
		} else {
			return dao.iterator(getPreparedQuery(), flags);
		}
	}
}
