package com.j256.ormlite.dao;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;

import com.j256.ormlite.field.ForeignCollectionField;

/**
 * <p>
 * Collection that is set on a field that as been marked with the {@link ForeignCollectionField} annotation when an
 * object is refreshed or queried (i.e. not created).
 * </p>
 * 
 * <pre>
 * &#064;ForeignCollectionField(eager = false)
 * private ForeignCollection&lt;Order&gt; orders;
 * </pre>
 * 
 * <p>
 * <b>NOTE:</b> If the collection has been marked as being "lazy" then just about all methods in this class result in a
 * pass through the database using the {@link #iterator()}. Even {@link #size()} and other seemingly simple calls can
 * cause a lot of database I/O. Most likely just the {@link #iterator()}, {@link #toArray()}, and
 * {@link #toArray(Object[])} methods should be used if you are using a lazy collection. Any other methods have no
 * guarantee to be at all efficient. Take a look at the source if you have any question.
 * </p>
 * 
 * <p>
 * <b>NOTE:</b> It is also important to remember that lazy iterators hold a connection open to the database which needs
 * to be closed. See {@link LazyForeignCollection#iterator()}.
 * </p>
 * 
 * @author graywatson
 */
public interface ForeignCollection<T> extends Collection<T>, CloseableIterable<T> {

	/**
	 * Like {@link Collection#iterator()} but while specifying flags for the results. This is necessary with certain
	 * database types. The resultFlags could be something like ResultSet.TYPE_SCROLL_INSENSITIVE or other values.
	 */
	public CloseableIterator<T> iterator(int flags);

	/**
	 * Same as {@link #iterator(int)}.
	 */
	public CloseableIterator<T> closeableIterator(int flags);

	/**
	 * Like {@link Collection#iterator()} but returns a closeable iterator instead and can throw a SQLException.
	 */
	public CloseableIterator<T> iteratorThrow() throws SQLException;

	/**
	 * Like {@link #iteratorThrow()} but while specifying flags for the results. This is necessary with certain database
	 * types. The resultFlags could be something like ResultSet.TYPE_SCROLL_INSENSITIVE or other values.
	 */
	public CloseableIterator<T> iteratorThrow(int flags) throws SQLException;

	/**
	 * This makes a one time use iterable class that can be closed afterwards. The ForeignCollection itself is
	 * {@link CloseableWrappedIterable} but multiple threads can each call this to get their own closeable iterable.
	 */
	public CloseableWrappedIterable<T> getWrappedIterable();

	/**
	 * Like {@link #getWrappedIterable()} but while specifying flags for the results. This is necessary with certain
	 * database types. The resultFlags could be something like ResultSet.TYPE_SCROLL_INSENSITIVE or other values.
	 */
	public CloseableWrappedIterable<T> getWrappedIterable(int flags);

	/**
	 * This will close the last iterator returned by the {@link #iterator()} method.
	 * 
	 * <p>
	 * <b>NOTE:</b> For lazy collections, this is not reentrant. If multiple threads are getting iterators from a lazy
	 * collection from the same object then you should use {@link #getWrappedIterable()} to get a reentrant wrapped
	 * iterable for each thread instead.
	 * </p>
	 */
	public void closeLastIterator() throws IOException;

	/**
	 * Returns true if this an eager collection otherwise false.
	 */
	public boolean isEager();

	/**
	 * This is a call through to {@link Dao#update(Object)} using the internal collection DAO. Objects inside of the
	 * collection are not updated if the parent object is refreshed so you will need to so that by hand.
	 */
	public int update(T obj) throws SQLException;

	/**
	 * Update all of the items <i>currently</i> in the collection with the database. This is only applicable for eager
	 * collections.
	 * 
	 * @return The number of rows updated.
	 */
	public int updateAll() throws SQLException;

	/**
	 * This is a call through to {@link Dao#refresh(Object)} using the internal collection DAO. Objects inside of the
	 * collection are not refreshed if the parent object is refreshed so you will need to so that by hand.
	 */
	public int refresh(T obj) throws SQLException;

	/**
	 * Call to refresh on all of the items <i>currently</i> in the collection with the database. This is only applicable
	 * for eager collections. If you want to see new objects in the collection then you should use
	 * {@link #refreshCollection()}.
	 * 
	 * @return The number of rows refreshed.
	 */
	public int refreshAll() throws SQLException;

	/**
	 * This re-issues the query that initially built the collection replacing any underlying result collection with a
	 * new one build from the database. This is only applicable for eager collections and is a no-op for lazy
	 * collections.
	 * 
	 * @return The number of objects loaded into the new collection.
	 */
	public int refreshCollection() throws SQLException;

	/**
	 * Adds the object to the collection. This will also add it to the database by calling through to [@link
	 * {@link Dao#create(Object)}. If the object has already been created in the database then you just need to set the
	 * foreign field on the object and call {@link Dao#update(Object)}. If you add it here the DAO will try to create it
	 * in the database again which will most likely cause an error.
	 * 
	 * @see Collection#add(Object)
	 */
	@Override
	public boolean add(T obj);

	/**
	 * Return the DAO object associated with this foreign collection. For usage for those who know what they are doing.
	 */
	public Dao<T, ?> getDao();
}
