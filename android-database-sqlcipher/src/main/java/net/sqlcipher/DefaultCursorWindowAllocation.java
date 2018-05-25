package net.sqlcipher;

import net.sqlcipher.CursorWindowAllocation;

public class DefaultCursorWindowAllocation implements CursorWindowAllocation {

  private long initialAllocationSize = 1024 * 1024;
  private long WindowAllocationUnbounded = 0;

  public long getInitialAllocationSize() {
    return initialAllocationSize;
  }

  public long getGrowthPaddingSize() {
    return initialAllocationSize;
  }

  public long getMaxAllocationSize() {
    return WindowAllocationUnbounded;
  }
}
