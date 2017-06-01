package com.indeed.jiraactions;

import com.indeed.jiraactions.api.response.issue.User;
import com.indeed.util.core.nullsafety.ReturnValuesAreNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;

@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface UserLookupService {
    User getUser(final String key) throws IOException;
}
