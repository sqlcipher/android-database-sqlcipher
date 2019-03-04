package net.sqlcipher;

import net.sqlcipher.CursorWindowAllocation;

public class CustomCursorWindowAllocation implements CursorWindowAllocation {

  private long initialAllocationSize = 0L;
  private long growthPaddingSize = 0L;
  private long maxAllocationSize = 0L;

  public CustomCursorWindowAllocation(long initialSize,
                                     long growthPaddingSize,
                                     long maxAllocationSize){
    this.initialAllocationSize = initialSize;
    this.growthPaddingSize = growthPaddingSize;
    this.maxAllocationSize = maxAllocationSize;
  }

  public long getInitialAllocationSize() {
    return initialAllocationSize;
  }

  public long getGrowthPaddingSize() {
    return growthPaddingSize;
  }

  public long getMaxAllocationSize() {
    return maxAllocationSize;
  }
}
