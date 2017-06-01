package com.indeed.jiraactions;

import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.util.core.nullsafety.ReturnValuesAreNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;

@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public class FriendlyUserLookupService implements UserLookupService {
    @Override
    public User getUser(final String key) throws IOException {
        final User user = new User();
        user.displayName = key;
        user.name = key;
        return user;
    }
}
