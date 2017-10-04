package com.indeed.jiraactions;

import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.util.core.nullsafety.ReturnValuesAreNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public class FriendlyUserLookupService implements UserLookupService {
    @Override
    public User getUser(@Nullable final String key) {
        final User user = new User();
        user.displayName = key;
        user.name = key;
        return user;
    }
}
