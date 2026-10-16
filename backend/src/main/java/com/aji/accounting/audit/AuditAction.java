package com.aji.accounting.audit;

public final class AuditAction {
    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";
    public static final String POST = "POST";
    public static final String VOID = "VOID";
    public static final String REVERSE = "REVERSE";
    public static final String APPROVE = "APPROVE";
    public static final String CLOSE = "CLOSE";
    public static final String REOPEN = "REOPEN";
    public static final String LOGIN = "LOGIN";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";
    public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";

    private AuditAction() {
    }
}
