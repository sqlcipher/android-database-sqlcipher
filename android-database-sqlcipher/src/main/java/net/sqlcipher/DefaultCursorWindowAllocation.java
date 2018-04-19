package net.sqlcipher;

import net.sqlcipher.CursorWindowAllocation;

public class DefaultCursorWindowAllocation implements CursorWindowAllocation {
  public long getAllocationSize(){
    return 0;
  }
}
