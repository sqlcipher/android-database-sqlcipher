package net.sqlcipher.database;

public class SQLiteQueryStats {
  long totalQueryResultSize = 0L;
  long largestIndividualRowSize = 0L;

  public SQLiteQueryStats(long totalQueryResultSize,
                          long largestIndividualRowSize) {
    this.totalQueryResultSize = totalQueryResultSize;
    this.largestIndividualRowSize = largestIndividualRowSize;
  }

  public long getTotalQueryResultSize(){
    return totalQueryResultSize;
  }

  public long getLargestIndividualRowSize(){
    return largestIndividualRowSize;
  }
}
