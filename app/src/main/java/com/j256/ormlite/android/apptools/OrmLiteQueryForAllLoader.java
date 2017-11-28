package com.j256.ormlite.android.apptools;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import android.content.Context;

import com.j256.ormlite.dao.Dao;

/**
 * A <code>Loader</code> implementation that queries specified {@link com.j256.ormlite.dao.Dao} for all data, using the
 * <code>Dao.queryForAll()</code> call.
 * 
 * @author EgorAnd
 */
public class OrmLiteQueryForAllLoader<T, ID> extends BaseOrmLiteLoader<T, ID> {

	public OrmLiteQueryForAllLoader(Context context) {
		super(context);
	}

	public OrmLiteQueryForAllLoader(Context context, Dao<T, ID> dao) {
		super(context, dao);
	}

	@Override
	public List<T> loadInBackground() {
		if (dao == null) {
			throw new IllegalStateException("Dao is not initialized.");
		}
		try {
			return dao.queryForAll();
		} catch (SQLException e) {
			// XXX: is this really the right thing to do? Maybe throw RuntimeException?
			e.printStackTrace();
			return Collections.emptyList();
		}
	}
}
