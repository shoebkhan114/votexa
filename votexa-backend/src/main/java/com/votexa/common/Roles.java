package com.votexa.common;

/** SpEL expressions for @PreAuthorize (least privilege per role). */
public final class Roles {
    private Roles() {}
    public static final String STAFF = "hasAnyRole('BLO','ERO','POLLING_OFFICER','AUDITOR','SUPER_ADMIN')";
    public static final String CITIZEN = "hasRole('CITIZEN')";
    public static final String ADMIN = "hasRole('SUPER_ADMIN')";
    public static final String ERO_ADMIN = "hasAnyRole('ERO','SUPER_ADMIN')";
    public static final String BLO_ERO_ADMIN = "hasAnyRole('BLO','ERO','SUPER_ADMIN')";
    public static final String VERIFIERS = "hasAnyRole('BLO','ERO','POLLING_OFFICER','SUPER_ADMIN')";
    public static final String POLLING = "hasAnyRole('POLLING_OFFICER','SUPER_ADMIN')";
    public static final String POLLING_READ = "hasAnyRole('POLLING_OFFICER','ERO','AUDITOR','SUPER_ADMIN')";
    public static final String AUDIT_READ = "hasAnyRole('AUDITOR','ERO','SUPER_ADMIN')";
    public static final String ANY_USER = "isAuthenticated()";
}
