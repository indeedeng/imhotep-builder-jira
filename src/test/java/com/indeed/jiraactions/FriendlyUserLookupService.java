package com.indeed.jiraactions;

import com.google.common.collect.ImmutableList;
import com.indeed.jiraactions.api.response.issue.ImmutableUser;
import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.util.core.nullsafety.ReturnValuesAreNonnullByDefault;
import org.junit.Assert;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public class FriendlyUserLookupService implements UserLookupService {
    @Override
    public User getUser(@Nullable final String key) {
        if (key == null) {
            return User.INVALID_USER;
        }
        return ImmutableUser.builder()
                .displayName("Display Name: " + key)
                .name("Name: " + key)
                .key("Key: " + key)
                .groups(ImmutableList.of("groups", key))
                .build();
    }

    void assertCreatedUser(final User user, final String key) {
        Assert.assertEquals("Display Name: " + key, user.getDisplayName());
        Assert.assertEquals("Name: " + key, user.getName());
        Assert.assertEquals("Key: " + key, user.getKey());
        Assert.assertEquals(ImmutableList.of("groups", key), user.getGroups());
    }
}
