package net.sqlcipher;

/**
 * An exception that indicates an unknown type was returned.
 */
public class UnknownTypeException extends RuntimeException
{
    public UnknownTypeException() {}

    public UnknownTypeException(String error)
    {
        super(error);
    }
}
