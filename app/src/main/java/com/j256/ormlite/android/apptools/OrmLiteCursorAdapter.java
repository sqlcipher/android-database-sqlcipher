package com.j256.ormlite.android.apptools;

import java.sql.SQLException;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.CursorAdapter;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.stmt.PreparedQuery;

/**
 * Cursor adapter base class.
 * 
 * @author emmby
 */
public abstract class OrmLiteCursorAdapter<T, ViewType extends View> extends CursorAdapter {

	protected PreparedQuery<T> preparedQuery;

	public OrmLiteCursorAdapter(Context context) {
		super(context, null, false);
	}

	/**
	 * Bind the view to a particular item.
	 */
	public abstract void bindView(ViewType itemView, Context context, T item);

	/**
	 * Final to prevent subclasses from accidentally overriding. Intentional overriding can be accomplished by
	 * overriding {@link #doBindView(View, Context, Cursor)}.
	 * 
	 * @see CursorAdapter#bindView(View, Context, Cursor)
	 */
	@Override
	public final void bindView(View itemView, Context context, Cursor cursor) {
		doBindView(itemView, context, cursor);
	}

	/**
	 * This is here to make sure that the user really wants to override it.
	 */
	protected void doBindView(View itemView, Context context, Cursor cursor) {
		try {
			@SuppressWarnings("unchecked")
			ViewType itemViewType = (ViewType) itemView;
			bindView(itemViewType, context, cursorToObject(cursor));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a T object at the current position.
	 */
	public T getTypedItem(int position) {
		try {
			return cursorToObject((Cursor) super.getItem(position));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Map a single row to our cursor object.
	 */
	protected T cursorToObject(Cursor cursor) throws SQLException {
		return preparedQuery.mapRow(new AndroidDatabaseResults(cursor, null, true));
	}

	/**
	 * Show not be used. Instead use {@link #changeCursor(Cursor, PreparedQuery)}
	 */
	@Override
	public final void changeCursor(Cursor cursor) {
		throw new UnsupportedOperationException(
				"Please use OrmLiteCursorAdapter.changeCursor(Cursor,PreparedQuery) instead");
	}

	/**
	 * Change the cursor associated with the prepared query.
	 */
	public void changeCursor(Cursor cursor, PreparedQuery<T> preparedQuery) {
		setPreparedQuery(preparedQuery);
		super.changeCursor(cursor);
	}

	public void setPreparedQuery(PreparedQuery<T> preparedQuery) {
		this.preparedQuery = preparedQuery;
	}
}
