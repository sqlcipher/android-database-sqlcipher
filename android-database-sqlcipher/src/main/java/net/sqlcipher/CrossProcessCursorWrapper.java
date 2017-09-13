package net.sqlcipher;

import android.database.CrossProcessCursor;
import android.database.CursorWindow;

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
        DatabaseUtils.cursorFillWindow(this, position, window);
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        return true;
    }
}
