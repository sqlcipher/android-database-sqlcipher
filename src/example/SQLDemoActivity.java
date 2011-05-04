package example;

import android.database.Cursor;
import info.guardianproject.database.sqlcipher.SQLiteDatabase;
import android.app.Activity;
import android.content.ContentValues;
import android.os.Bundle;
import android.util.Log;

public class SQLDemoActivity extends Activity {
  EventDataSQLHelper eventsData;

  /*
 0x00000001 (NEEDED)                     Shared library: [libstlport_shared.so]
 0x00000001 (NEEDED)                     Shared library: [libc.so]
 0x00000001 (NEEDED)                     Shared library: [libstdc++.so]
 0x00000001 (NEEDED)                     Shared library: [libm.so]
 0x00000001 (NEEDED)                     Shared library: [libsqlcipher.so]
 0x00000001 (NEEDED)                     Shared library: [liblog.so]
 0x00000001 (NEEDED)                     Shared library: [libdl.s	  
   */

  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

 //   System.loadLibrary("nativehelper");
 //   System.loadLibrary("android_runtime");

  //  System.loadLibrary("crypto");
  //  System.loadLibrary("ssl");

    /*
    System.loadLibrary("icudata");
    System.loadLibrary("icui18n");
    System.loadLibrary("icuuc"); 
    */
 
   
    /*
    System.loadLibrary("lib/libstlport_shared.so");
    System.loadLibrary("lib/libsqlcipher.so");
    System.loadLibrary("lib/libdatabase_sqlcipher.so");
    System.loadLibrary("lib/libsqlcipher_android.so");
*/

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