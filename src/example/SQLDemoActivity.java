package example;

import info.guardianproject.database.Cursor;
import info.guardianproject.database.sqlcipher.SQLiteDatabase;
import android.app.Activity;
import android.content.ContentValues;
import android.os.Bundle;
import android.util.Log;

public class SQLDemoActivity extends Activity {
  EventDataSQLHelper eventsData;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    System.loadLibrary("nativehelper");
    System.loadLibrary("android_runtime");

    System.loadLibrary("crypto");
    System.loadLibrary("ssl");


    System.loadLibrary("icudata");
    System.loadLibrary("icui18n");
    System.loadLibrary("icuuc");

    System.loadLibrary("stlport_shared");

    System.loadLibrary("sqlcipher");

    System.loadLibrary("sqlcipher_android");
    System.loadLibrary("database_sqlcipher");



    eventsData = new EventDataSQLHelper(this);
    addEvent("Hello Android Event");
    Cursor cursor = getEvents();
    showEvents(cursor);
  }
  
  @Override
  public void onDestroy() {
    eventsData.close();
  }

  private void addEvent(String title) {
    SQLiteDatabase db = eventsData.getWritableDatabase();
    
    
    ContentValues values = new ContentValues();
    values.put(EventDataSQLHelper.TIME, System.currentTimeMillis());
    values.put(EventDataSQLHelper.TITLE, title);
    db.insert(EventDataSQLHelper.TABLE, null, values);

  }

  private Cursor getEvents() {
    SQLiteDatabase db = eventsData.getReadableDatabase();
    Cursor cursor = db.query(EventDataSQLHelper.TABLE, null, null, null, null,
        null, null);
    
    startManagingCursor(cursor);
    return cursor;
  }

  private void showEvents(Cursor cursor) {
    StringBuilder ret = new StringBuilder("Saved Events:\n\n");
    while (cursor.moveToNext()) {
      long id = cursor.getLong(0);
      long time = cursor.getLong(1);
      String title = cursor.getString(2);
      ret.append(id + ": " + time + ": " + title + "\n");
    }
    
    Log.i("sqldemo",ret.toString());
  }
}