package com.indeed.jiraactions.api.links;

import com.indeed.jiraactions.api.response.issue.changelog.histories.Item;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkFactory {
    private static final Pattern LINK_PATTERN = Pattern.compile("This issue ([A-Za-z ]+) ([A-Z0-9]+-[0-9]+)");

    protected Link makeLink(@Nonnull final String input) {
        final Matcher matcher = LINK_PATTERN.matcher(input);
        if(!matcher.matches()) {
            throw new IllegalArgumentException("Failed to make link from " + input);
        }
        if(matcher.groupCount() != 2) {
            throw new IllegalArgumentException("Incorrect number of groups " + matcher.groupCount() + " from " + input);
        }

        final String type = matcher.group(1);
        final String target = matcher.group(2);
        return ImmutableLink.builder()
                .targetKey(target)
                .description(type)
                .build();
    }

    public Set<Link> mergeLinks(@Nonnull final Set<Link> source, final Collection<Item> changes) {
        final Set<Link> output = new HashSet<>(source);
        for(final Item item : changes) {
            if(!StringUtils.isEmpty(item.fromString)) {
                final Link link = makeLink(item.fromString);
                output.remove(link);
            }
            if(!StringUtils.isEmpty(item.toString)) {
                final Link link = makeLink(item.toString);
                output.add(link);
            }
        }

        return output;
    }
}
