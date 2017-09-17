package com.github.felixoldenburg;

/**
 * Created by f.oldenburg on 9/17/15.
 */
public class ZooKeeperUnavailableException extends RuntimeException
{
    public ZooKeeperUnavailableException(String message)
    {
        super(message);
    }

    public ZooKeeperUnavailableException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
