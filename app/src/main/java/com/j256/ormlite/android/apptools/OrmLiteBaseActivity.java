package com.j256.ormlite.android.apptools;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.j256.ormlite.support.ConnectionSource;

/**
 * Base class to use for activities in Android.
 * 
 * You can simply call {@link #getHelper()} to get your helper class, or {@link #getConnectionSource()} to get a
 * {@link ConnectionSource}.
 * 
 * The method {@link #getHelper()} assumes you are using the default helper factory -- see {@link OpenHelperManager}. If
 * not, you'll need to provide your own helper instances which will need to implement a reference counting scheme. This
 * method will only be called if you use the database, and only called once for this activity's life-cycle. 'close' will
 * also be called once for each call to createInstance.
 * 
 * @author graywatson, kevingalligan
 */
public abstract class OrmLiteBaseActivity<H extends OrmLiteSqliteOpenHelper> extends Activity {

	private volatile H helper;
	private volatile boolean created = false;
	private volatile boolean destroyed = false;
	private static Logger logger = LoggerFactory.getLogger(OrmLiteBaseActivity.class);

	/**
	 * Get a helper for this action.
	 */
	public H getHelper() {
		if (helper == null) {
			if (!created) {
				throw new IllegalStateException("A call has not been made to onCreate() yet so the helper is null");
			} else if (destroyed) {
				throw new IllegalStateException(
						"A call to onDestroy has already been made and the helper cannot be used after that point");
			} else {
				throw new IllegalStateException("Helper is null for some unknown reason");
			}
		} else {
			return helper;
		}
	}

	/**
	 * Get a connection source for this action.
	 */
	public ConnectionSource getConnectionSource() {
		return getHelper().getConnectionSource();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (helper == null) {
			helper = getHelperInternal(this);
			created = true;
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		releaseHelper(helper);
		destroyed = true;
	}

	/**
	 * This is called internally by the class to populate the helper object instance. This should not be called directly
	 * by client code unless you know what you are doing. Use {@link #getHelper()} to get a helper instance. If you are
	 * managing your own helper creation, override this method to supply this activity with a helper instance.
	 * 
	 * <p>
	 * <b> NOTE: </b> If you override this method, you most likely will need to override the
	 * {@link #releaseHelper(OrmLiteSqliteOpenHelper)} method as well.
	 * </p>
	 */
	protected H getHelperInternal(Context context) {
		@SuppressWarnings({ "unchecked", "deprecation" })
		H newHelper = (H) OpenHelperManager.getHelper(context);
		logger.trace("{}: got new helper {} from OpenHelperManager", this, newHelper);
		return newHelper;
	}

	/**
	 * Release the helper instance created in {@link #getHelperInternal(Context)}. You most likely will not need to call
	 * this directly since {@link #onDestroy()} does it for you.
	 * 
	 * <p>
	 * <b> NOTE: </b> If you override this method, you most likely will need to override the
	 * {@link #getHelperInternal(Context)} method as well.
	 * </p>
	 */
	protected void releaseHelper(H helper) {
		OpenHelperManager.releaseHelper();
		logger.trace("{}: helper {} was released, set to null", this, helper);
		this.helper = null;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + Integer.toHexString(super.hashCode());
	}
}
