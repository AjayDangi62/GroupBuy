package com.groupbuy.common.exception;

/**
 * Exception thrown when a user attempts to authenticate but their account is suspended or deleted.
 * Resolves to HTTP 403 Forbidden.
 */
public class AccountSuspendedException extends BusinessException {

    public AccountSuspendedException(String message) {
        super(message, "ACCOUNT_SUSPENDED");
    }

    public AccountSuspendedException() {
        super("User account is suspended or inactive.", "ACCOUNT_SUSPENDED");
    }
}
