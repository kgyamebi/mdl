package com.mdl.platform.authorization.projection;

public final class UserAuthProfile {

    private UserAuthProfile() {
    }

    public interface BusinessProfile {
        Long getBusinessId();

        String getBusinessCode();

        String getBusinessName();

        String getCurrencyCode();
    }
}
