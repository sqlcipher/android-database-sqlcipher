package com.j256.ormlite.dao;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.support.DatabaseResults;

/**
 * Collection that is set on a field that as been marked with the {@link ForeignCollectionField} annotation when an
 * object is refreshed or queried (i.e. not created).
 * 
 * @author graywatson
 */
public class EagerForeignCollection<T, ID> extends BaseForeignCollection<T, ID>
		implements CloseableWrappedIterable<T>, Serializable {

	private static final long serialVersionUID = -2523335606983317721L;

	private List<T> results;

	/**
	 * WARNING: The user should not be calling this constructor. You should be using the
	 * {@link Dao#assignEmptyForeignCollection(Object, String)} or {@link Dao#getEmptyForeignCollection(String)} methods
	 * instead.
	 */
	public EagerForeignCollection(Dao<T, ID> dao, Object parent, Object parentId, FieldType foreignFieldType,
			String orderColumn, boolean orderAscending) throws SQLException {
		super(dao, parent, parentId, foreignFieldType, orderColumn, orderAscending);
		if (parentId == null) {
			/*
			 * If we have no field value then just create an empty list. This is for when we need to create an empty
			 * eager collection.
			 */
			results = new ArrayList<T>();
		} else {
			// go ahead and do the query if eager
			results = dao.query(getPreparedQuery());
		}
	}

	@Override
	public CloseableIterator<T> iterator() {
		return iteratorThrow(DatabaseConnection.DEFAULT_RESULT_FLAGS);
	}

	@Override
	public CloseableIterator<T> iterator(int flags) {
		return iteratorThrow(flags);
	}

	@Override
	public CloseableIterator<T> closeableIterator() {
		return iteratorThrow(DatabaseConnection.DEFAULT_RESULT_FLAGS);
	}

	@Override
	public CloseableIterator<T> closeableIterator(int flags) {
		return iteratorThrow(DatabaseConnection.DEFAULT_RESULT_FLAGS);
	}

	@Override
	public CloseableIterator<T> iteratorThrow() {
		return iteratorThrow(DatabaseConnection.DEFAULT_RESULT_FLAGS);
	}

	@Override
	public CloseableIterator<T> iteratorThrow(int flags) {
		// we have to wrap the iterator since we are returning the List's iterator
		return new CloseableIterator<T>() {
			private int offset = -1;

			@Override
			public boolean hasNext() {
				return (offset + 1 < results.size());
			}

			@Override
			public T first() {
				offset = 0;
				if (offset >= results.size()) {
					return null;
				} else {
					return results.get(0);
				}
			}

			@Override
			public T next() {
				offset++;
				// this should throw if OOB
				return results.get(offset);
			}

			@Override
			public T nextThrow() {
				offset++;
				if (offset >= results.size()) {
					return null;
				} else {
					return results.get(offset);
				}
			}

			@Override
			public T current() {
				if (offset < 0) {
					offset = 0;
				}
				if (offset >= results.size()) {
					return null;
				} else {
					return results.get(offset);
				}
			}

			@Override
			public T previous() {
				offset--;
				if (offset < 0 || offset >= results.size()) {
					return null;
				} else {
					return results.get(offset);
				}
			}

			@Override
			public T moveRelative(int relativeOffset) {
				offset += relativeOffset;
				if (offset < 0 || offset >= results.size()) {
					return null;
				} else {
					return results.get(offset);
				}
			}

			@Override
			public T moveAbsolute(int position) {
				offset = position;
				if (offset < 0 || offset >= results.size()) {
					return null;
				} else {
					return results.get(offset);
				}
			}

			@Override
			public void remove() {
				if (offset < 0) {
					throw new IllegalStateException("next() must be called before remove()");
				}
				if (offset >= results.size()) {
					throw new IllegalStateException("current results position (" + offset + ") is out of bounds");
				}
				T removed = results.remove(offset);
				offset--;
				if (dao != null) {
					try {
						dao.delete(removed);
					} catch (SQLException e) {
						// have to demote this to be runtime
						throw new RuntimeException(e);
					}
				}
			}

			@Override
			public void close() {
				// noop
			}

			@Override
			public void closeQuietly() {
				// noop
			}

			@Override
			public DatabaseResults getRawResults() {
				// no results object
				return null;
			}

			@Override
			public void moveToNext() {
				offset++;
			}
		};
	}

	@Override
	public CloseableWrappedIterable<T> getWrappedIterable() {
		// since the iterators don't have any connections, the collection can be a wrapped iterable
		return this;
	}

	@Override
	public CloseableWrappedIterable<T> getWrappedIterable(int flags) {
		return this;
	}

	@Override
	public void close() {
		// noop since the iterators aren't holding open a connection
	}

	@Override
	public void closeLastIterator() {
		// noop since the iterators aren't holding open a connection
	}

	@Override
	public boolean isEager() {
		return true;
	}

	@Override
	public int size() {
		return results.size();
	}

	@Override
	public boolean isEmpty() {
		return results.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return results.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return results.containsAll(c);
	}

	@Override
	public Object[] toArray() {
		return results.toArray();
	}

	@Override
	public <E> E[] toArray(E[] array) {
		return results.toArray(array);
	}

	@Override
	public boolean add(T data) {
		if (results.add(data)) {
			return super.add(data);
		} else {
			return false;
		}
	}

	@Override
	public boolean addAll(Collection<? extends T> collection) {
		if (results.addAll(collection)) {
			return super.addAll(collection);
		} else {
			return false;
		}
	}

	@Override
	public boolean remove(Object data) {
		if (!results.remove(data) || dao == null) {
			return false;
		}

		@SuppressWarnings("unchecked")
		T castData = (T) data;
		try {
			return (dao.delete(castData) == 1);
		} catch (SQLException e) {
			throw new IllegalStateException("Could not delete data element from dao", e);
		}
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		boolean changed = false;
		for (Object data : collection) {
			if (remove(data)) {
				changed = true;
			}
		}
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		// delete from the iterate removes from the eager list and dao
		return super.retainAll(collection);
	}

	@Override
	public int updateAll() throws SQLException {
		int updatedC = 0;
		for (T data : results) {
			updatedC += dao.update(data);
		}
		return updatedC;
	}

	@Override
	public int refreshAll() throws SQLException {
		int updatedC = 0;
		for (T data : results) {
			updatedC += dao.refresh(data);
		}
		return updatedC;
	}

	@Override
	public int refreshCollection() throws SQLException {
		results = dao.query(getPreparedQuery());
		return results.size();
	}

	/**
	 * This is just a call to the equals method of the internal results list.
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof EagerForeignCollection)) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		EagerForeignCollection other = (EagerForeignCollection) obj;
		return results.equals(other.results);
	}

	/**
	 * This is just a call to the hashcode method of the internal results list.
	 */
	@Override
	public int hashCode() {
		return results.hashCode();
	}
}
