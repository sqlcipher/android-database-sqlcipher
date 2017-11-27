package com.j256.ormlite.android.apptools;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.Loader;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;

/**
 * A {@link Loader} implementation that queries specified {@link Dao} using a {@link PreparedQuery}.
 * 
 * @author Egorand
 */
public class OrmLitePreparedQueryLoader<T, ID> extends BaseOrmLiteLoader<T, ID> {

	private PreparedQuery<T> preparedQuery;

	public OrmLitePreparedQueryLoader(Context context) {
		super(context);
	}

	public OrmLitePreparedQueryLoader(Context context, Dao<T, ID> dao, PreparedQuery<T> preparedQuery) {
		super(context, dao);
		this.preparedQuery = preparedQuery;
	}

	@Override
	public List<T> loadInBackground() {
		if (dao == null) {
			throw new IllegalStateException("Dao is not initialized.");
		}
		if (preparedQuery == null) {
			throw new IllegalStateException("PreparedQuery is not initialized.");
		}
		try {
			return dao.query(preparedQuery);
		} catch (SQLException e) {
			// XXX: is this really the right thing to do? Maybe throw RuntimeException?
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	public void setPreparedQuery(PreparedQuery<T> preparedQuery) {
		this.preparedQuery = preparedQuery;
	}

	public PreparedQuery<T> getPreparedQuery() {
		return preparedQuery;
	}
}
