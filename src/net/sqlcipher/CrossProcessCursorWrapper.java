package net.sqlcipher;

import android.database.Cursor;
import android.database.CrossProcessCursor;
import android.database.CursorWindow;
import android.database.CursorWrapper;

public class CrossProcessCursorWrapper extends CursorWrapper implements CrossProcessCursor {

    public CrossProcessCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    @Override
    public CursorWindow getWindow() {
        return null;
    }

    @Override
    public void fillWindow(int position, CursorWindow window) {
        if (position < 0 || position > getCount()) {
            return;
        }
        window.acquireReference();
        try {
            moveToPosition(position - 1);
            window.clear();
            window.setStartPosition(position);
            int columnNum = getColumnCount();
            window.setNumColumns(columnNum);
            while (moveToNext() && window.allocRow()) {
                for (int i = 0; i < columnNum; i++) {
                    String field = getString(i);
                    if (field != null) {
                        if (!window.putString(field, getPosition(), i)) {
                            window.freeLastRow();
                            break;
                        }
                    } else {
                        if (!window.putNull(getPosition(), i)) {
                            window.freeLastRow();
                            break;
                        }
                    }
                }
            }
        } catch (IllegalStateException e) {
            // simply ignore it
        } finally {
            window.releaseReference();
        }
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        return true;
    }
}
