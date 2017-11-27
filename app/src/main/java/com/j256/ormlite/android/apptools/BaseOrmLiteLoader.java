package com.j256.ormlite.android.apptools;

import java.util.List;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.Dao.DaoObserver;

/**
 * An abstract superclass for the ORMLite Loader classes, which closely resembles to the Android's
 * <code>CursorLoader</code>. Implements basic loading and synchronization logic.
 * 
 * @author EgorAnd
 */
public abstract class BaseOrmLiteLoader<T, ID> extends AsyncTaskLoader<List<T>> implements DaoObserver {

	/**
	 * A Dao which will be queried for the data.
	 */
	protected Dao<T, ID> dao;
	private List<T> cachedResults;

	public BaseOrmLiteLoader(Context context) {
		super(context);
	}

	public BaseOrmLiteLoader(Context context, Dao<T, ID> dao) {
		super(context);
		this.dao = dao;
	}

	@Override
	public void deliverResult(List<T> results) {
		if (!isReset() && isStarted()) {
			super.deliverResult(results);
		}
	}

	/**
	 * Starts an asynchronous load of the data. When the result is ready the callbacks will be called on the UI thread.
	 * If a previous load has been completed and is still valid the result may be passed to the callbacks immediately.
	 * 
	 * <p>
	 * Must be called from the UI thread.
	 * </p>
	 */
	@Override
	protected void onStartLoading() {
		// XXX: do we really return the cached results _before_ checking if the content has changed?
		if (cachedResults != null) {
			deliverResult(cachedResults);
		}
		if (takeContentChanged() || cachedResults == null) {
			forceLoad();
		}
		// watch for data changes
		dao.registerObserver(this);
	}

	/**
	 * Must be called from the UI thread
	 */
	@Override
	protected void onStopLoading() {
		// attempt to cancel the current load task if possible.
		cancelLoad();
	}

	@Override
	protected void onReset() {
		super.onReset();

		// ensure the loader is stopped
		onStopLoading();
		if (cachedResults != null) {
			cachedResults.clear();
			cachedResults = null;
		}

		// stop watching for changes
		dao.unregisterObserver(this);
	}

	@Override
	public void onChange() {
		onContentChanged();
	}

	public void setDao(Dao<T, ID> dao) {
		this.dao = dao;
	}
}
