package net.sqlcipher;

public interface CursorWindowAllocation {
  long getInitialAllocationSize();
  long getGrowthPaddingSize();
  long getMaxAllocationSize();
}
