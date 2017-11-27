package com.j256.ormlite.android.apptools;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import android.content.Context;
import android.content.res.Resources;
import net.sqlcipher.database.SQLiteOpenHelper;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;

/**
 * This helps organize and access database connections to optimize connection sharing. There are several schemes to
 * manage the database connections in an Android app, but as an app gets more complicated, there are many potential
 * places where database locks can occur. This class allows database connection sharing between multiple threads in a
 * single app.
 * 
 * This gets injected or called with the {@link OrmLiteSqliteOpenHelper} class that is used to manage the database
 * connection. The helper instance will be kept in a static field and only released once its internal usage count goes
 * to 0.
 * 
 * The {@link SQLiteOpenHelper} and database classes maintain one connection under the hood, and prevent locks in the
 * java code. Creating multiple connections can potentially be a source of trouble. This class shares the same
 * connection instance between multiple clients, which will allow multiple activities and services to run at the same
 * time.
 * 
 * Every time you use the helper, you should call {@link #getHelper(Context)} or {@link #getHelper(Context, Class)}.
 * When you are done with the helper you should call {@link #releaseHelper()}.
 * 
 * @author graywatson, kevingalligan
 */
public class OpenHelperManager {

	private static final String HELPER_CLASS_RESOURCE_NAME = "open_helper_classname";
	private static Logger logger = LoggerFactory.getLogger(OpenHelperManager.class);

	private static Class<? extends OrmLiteSqliteOpenHelper> helperClass = null;
	private static volatile OrmLiteSqliteOpenHelper helper = null;
	private static boolean wasClosed = false;
	private static int instanceCount = 0;

	/**
	 * If you are _not_ using the {@link OrmLiteBaseActivity} type classes then you will need to call this in a static
	 * method in your code.
	 */
	public static synchronized void setOpenHelperClass(Class<? extends OrmLiteSqliteOpenHelper> openHelperClass) {
		if (openHelperClass == null) {
			helperClass = null;
		} else {
			innerSetHelperClass(openHelperClass);
		}
	}

	/**
	 * Set the helper for the manager. This is most likely used for testing purposes and should only be called if you
	 * _really_ know what you are doing. If you do use it then it should be in a static {} initializing block to make
	 * sure you have one helper instance for your application.
	 */
	public static synchronized void setHelper(OrmLiteSqliteOpenHelper helper) {
		OpenHelperManager.helper = helper;
	}

	/**
	 * Create a static instance of our open helper from the helper class. This has a usage counter on it so make sure
	 * all calls to this method have an associated call to {@link #releaseHelper()}. This should be called during an
	 * onCreate() type of method when the application or service is starting. The caller should then keep the helper
	 * around until it is shutting down when {@link #releaseHelper()} should be called.
	 */
	public static synchronized <T extends OrmLiteSqliteOpenHelper> T getHelper(Context context, Class<T> openHelperClass) {
		if (openHelperClass == null) {
			throw new IllegalArgumentException("openHelperClass argument is null");
		}
		innerSetHelperClass(openHelperClass);
		return loadHelper(context, openHelperClass);
	}

	/**
	 * <p>
	 * Similar to {@link #getHelper(Context, Class)} (which is recommended) except we have to find the helper class
	 * through other means. This method requires that the Context be a class that extends one of ORMLite's Android base
	 * classes such as {@link OrmLiteBaseActivity}. Either that or the helper class needs to be set in the strings.xml.
	 * </p>
	 * 
	 * <p>
	 * To find the helper class, this does the following:
	 * </p>
	 * 
	 * <ol>
	 * <li>If the class has been set with a call to {@link #setOpenHelperClass(Class)}, it will be used to construct a
	 * helper.</li>
	 * <li>If the resource class name is configured in the strings.xml file it will be used.</li>
	 * <li>The context class hierarchy is walked looking at the generic parameters for a class extending
	 * OrmLiteSqliteOpenHelper. This is used by the {@link OrmLiteBaseActivity} and other base classes.</li>
	 * <li>An exception is thrown saying that it was not able to set the helper class.</li>
	 * </ol>
	 * 
	 * @deprecated Should use {@link #getHelper(Context, Class)}
	 */
	@Deprecated
	public static synchronized OrmLiteSqliteOpenHelper getHelper(Context context) {
		if (helperClass == null) {
			if (context == null) {
				throw new IllegalArgumentException("context argument is null");
			}
			Context appContext = context.getApplicationContext();
			innerSetHelperClass(lookupHelperClass(appContext, context.getClass()));
		}
		return loadHelper(context, helperClass);
	}

	/**
	 * Release the helper that was previously returned by a call {@link #getHelper(Context)} or
	 * {@link #getHelper(Context, Class)}. This will decrement the usage counter and close the helper if the counter is
	 * 0.
	 * 
	 * <p>
	 * <b> WARNING: </b> This should be called in an onDestroy() type of method when your application or service is
	 * terminating or if your code is no longer going to use the helper or derived DAOs in any way. _Don't_ call this
	 * method if you expect to call {@link #getHelper(Context)} again before the application terminates.
	 * </p>
	 */
	public static synchronized void releaseHelper() {
		instanceCount--;
		logger.trace("releasing helper {}, instance count = {}", helper, instanceCount);
		if (instanceCount <= 0) {
			if (helper != null) {
				logger.trace("zero instances, closing helper {}", helper);
				helper.close();
				helper = null;
				wasClosed = true;
			}
			if (instanceCount < 0) {
				logger.error("too many calls to release helper, instance count = {}", instanceCount);
			}
		}
	}

	/**
	 * Set the helper class and make sure we aren't changing it to another class.
	 */
	private static void innerSetHelperClass(Class<? extends OrmLiteSqliteOpenHelper> openHelperClass) {
		// make sure if that there are not 2 helper classes in an application
		if (openHelperClass == null) {
			throw new IllegalStateException("Helper class was trying to be reset to null");
		} else if (helperClass == null) {
			helperClass = openHelperClass;
		} else if (helperClass != openHelperClass) {
			throw new IllegalStateException("Helper class was " + helperClass + " but is trying to be reset to "
					+ openHelperClass);
		}
	}

	private static <T extends OrmLiteSqliteOpenHelper> T loadHelper(Context context, Class<T> openHelperClass) {
		if (helper == null) {
			if (wasClosed) {
				// this can happen if you are calling get/release and then get again
				logger.info("helper was already closed and is being re-opened");
			}
			if (context == null) {
				throw new IllegalArgumentException("context argument is null");
			}
			Context appContext = context.getApplicationContext();
			helper = constructHelper(appContext, openHelperClass);
			logger.trace("zero instances, created helper {}", helper);
			/*
			 * Filipe Leandro and I worked on this bug for like 10 hours straight. It's a doosey.
			 * 
			 * Each ForeignCollection has internal DAO objects that are holding a ConnectionSource. Each Android
			 * ConnectionSource is tied to a particular database connection. What Filipe was seeing was that when all of
			 * his views we closed (onDestroy), but his application WAS NOT FULLY KILLED, the first View.onCreate()
			 * method would open a new connection to the database. Fine. But because he application was still in memory,
			 * the static BaseDaoImpl default cache had not been cleared and was containing cached objects with
			 * ForeignCollections. The ForeignCollections still had references to the DAOs that had been opened with old
			 * ConnectionSource objects and therefore the old database connection. Using those cached collections would
			 * cause exceptions saying that you were trying to work with a database that had already been close.
			 * 
			 * Now, whenever we create a new helper object, we must make sure that the internal object caches have been
			 * fully cleared. This is a good lesson for anyone that is holding objects around after they have closed
			 * connections to the database or re-created the DAOs on a different connection somehow.
			 */
			BaseDaoImpl.clearAllInternalObjectCaches();
			/*
			 * Might as well do this also since if the helper changes then the ConnectionSource will change so no one is
			 * going to have a cache hit on the old DAOs anyway. All they are doing is holding memory.
			 * 
			 * NOTE: we don't want to clear the config map.
			 */
			DaoManager.clearDaoCache();
			instanceCount = 0;
		}

		instanceCount++;
		logger.trace("returning helper {}, instance count = {} ", helper, instanceCount);
		@SuppressWarnings("unchecked")
		T castHelper = (T) helper;
		return castHelper;
	}

	/**
	 * Call the constructor on our helper class.
	 */
	private static OrmLiteSqliteOpenHelper constructHelper(Context context,
			Class<? extends OrmLiteSqliteOpenHelper> openHelperClass) {
		Constructor<?> constructor;
		try {
			constructor = openHelperClass.getConstructor(Context.class);
		} catch (Exception e) {
			throw new IllegalStateException(
					"Could not find public constructor that has a single (Context) argument for helper class "
							+ openHelperClass, e);
		}
		try {
			return (OrmLiteSqliteOpenHelper) constructor.newInstance(context);
		} catch (Exception e) {
			throw new IllegalStateException("Could not construct instance of helper class " + openHelperClass, e);
		}
	}

	/**
	 * Lookup the helper class either from the resource string or by looking for a generic parameter.
	 */
	private static Class<? extends OrmLiteSqliteOpenHelper> lookupHelperClass(Context context, Class<?> componentClass) {

		// see if we have the magic resource class name set
		Resources resources = context.getResources();
		int resourceId = resources.getIdentifier(HELPER_CLASS_RESOURCE_NAME, "string", context.getPackageName());
		if (resourceId != 0) {
			String className = resources.getString(resourceId);
			try {
				@SuppressWarnings("unchecked")
				Class<? extends OrmLiteSqliteOpenHelper> castClass =
						(Class<? extends OrmLiteSqliteOpenHelper>) Class.forName(className);
				return castClass;
			} catch (Exception e) {
				throw new IllegalStateException("Could not create helper instance for class " + className, e);
			}
		}

		// try walking the context class to see if we can get the OrmLiteSqliteOpenHelper from a generic parameter
		for (Class<?> componentClassWalk = componentClass; componentClassWalk != null; componentClassWalk =
				componentClassWalk.getSuperclass()) {
			Type superType = componentClassWalk.getGenericSuperclass();
			if (superType == null || !(superType instanceof ParameterizedType)) {
				continue;
			}
			// get the generic type arguments
			Type[] types = ((ParameterizedType) superType).getActualTypeArguments();
			// defense
			if (types == null || types.length == 0) {
				continue;
			}
			for (Type type : types) {
				// defense
				if (!(type instanceof Class)) {
					continue;
				}
				Class<?> clazz = (Class<?>) type;
				if (OrmLiteSqliteOpenHelper.class.isAssignableFrom(clazz)) {
					@SuppressWarnings("unchecked")
					Class<? extends OrmLiteSqliteOpenHelper> castOpenHelperClass =
							(Class<? extends OrmLiteSqliteOpenHelper>) clazz;
					return castOpenHelperClass;
				}
			}
		}
		throw new IllegalStateException(
				"Could not find OpenHelperClass because none of the generic parameters of class " + componentClass
						+ " extends OrmLiteSqliteOpenHelper.  You should use getHelper(Context, Class) instead.");
	}
}
