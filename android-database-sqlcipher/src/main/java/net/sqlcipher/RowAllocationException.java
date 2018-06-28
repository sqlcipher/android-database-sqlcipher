package net.sqlcipher;

/**
 * An exception that indicates there was an error attempting to allocate a row
 * for the CursorWindow.
 */
public class RowAllocationException extends RuntimeException
{
    public RowAllocationException() {}

    public RowAllocationException(String error)
    {
        super(error);
    }
}
