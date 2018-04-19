package net.sqlcipher;

import net.sqlcipher.CursorWindowAllocation;

public class FixedCursorWindowAllocation implements CursorWindowAllocation {

  private long allocationSize = 0L;

  public FixedCursorWindowAllocation(long size){
    allocationSize = size;
  }

  public long getAllocationSize(){
    return allocationSize;
  }
}
