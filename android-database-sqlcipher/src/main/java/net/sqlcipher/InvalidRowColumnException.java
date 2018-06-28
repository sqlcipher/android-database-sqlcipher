package net.sqlcipher;

/**
 * An exception that indicates there was an error accessing a specific row/column.
 */
public class InvalidRowColumnException extends RuntimeException
{
    public InvalidRowColumnException() {}

    public InvalidRowColumnException(String error)
    {
        super(error);
    }
}
