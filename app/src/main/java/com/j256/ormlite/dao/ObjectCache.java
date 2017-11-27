package com.j256.ormlite.dao;

/**
 * Definition of an object cache that can be injected into the Dao with the {@link Dao#setObjectCache(ObjectCache)}.
 * 
 * <p>
 * <b>NOTE:</b> Most of the below methods take a Class argument but your cache can be for a single cache. If this is the
 * case then you should protect against storing different classes in the cache.
 * </p>
 * 
 * @author graywatson
 */
public interface ObjectCache {

	/**
	 * Register a class for use with this class. This will be called before any other method for the particular class is
	 * called.
	 */
	public <T> void registerClass(Class<T> clazz);

	/**
	 * Lookup in the cache for an object of a certain class that has a certain id.
	 * 
	 * @return The found object or null if none.
	 */
	public <T, ID> T get(Class<T> clazz, ID id);

	/**
	 * Put an object in the cache that has a certain class and id.
	 */
	public <T, ID> void put(Class<T> clazz, ID id, T data);

	/**
	 * Delete from the cache an object of a certain class that has a certain id.
	 */
	public <T, ID> void remove(Class<T> clazz, ID id);

	/**
	 * Change the id in the cache for an object of a certain class from an old-id to a new-id.
	 */
	public <T, ID> T updateId(Class<T> clazz, ID oldId, ID newId);

	/**
	 * Remove all entries from the cache of a certain class.
	 */
	public <T> void clear(Class<T> clazz);

	/**
	 * Remove all entries from the cache of all classes.
	 */
	public void clearAll();

	/**
	 * Return the number of elements in the cache.
	 */
	public <T> int size(Class<T> clazz);

	/**
	 * Return the number of elements in all of the caches.
	 */
	public int sizeAll();
}
