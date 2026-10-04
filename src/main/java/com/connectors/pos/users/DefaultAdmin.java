package com.connectors.pos.users;

/**
 * Credentials of the account created on the first launch of a local (offline) install.
 * The account is flagged mustChangePassword, so these values only work until the
 * customer picks their own password.
 */
public final class DefaultAdmin {
    public static final String NAME = "admin";
    public static final String EMAIL = "admin@pos.local";
    public static final String PASSWORD = "Admin@12345";

    private DefaultAdmin() {}
}
