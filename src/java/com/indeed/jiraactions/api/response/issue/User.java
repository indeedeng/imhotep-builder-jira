package com.indeed.jiraactions.api.response.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import org.immutables.value.Value;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author soono
 */

@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonDeserialize(as = ImmutableUser.class)
public interface User {
    User INVALID_USER = ImmutableUser.builder()
            .displayName("No User")
            .name("nouser")
            .key("nouser")
            .build();

    User NOBODY = ImmutableUser.builder()
            .displayName("")
            .name("")
            .key("")
            .build();

    static User getFallbackUser(final String key) {
        return ImmutableUser.builder()
                .displayName("Unknown User " + key)
                .key(key)
                .name(key)
                .build();
    }

    String getDisplayName();
    String getName();
    String getKey();
    @JsonDeserialize(using = GroupDeserializer.class)
    List<String> getGroups();

    class GroupDeserializer extends StdDeserializer<List<String>> {
        GroupDeserializer() {
            super(List.class);
        }

        public List<String> deserialize(
                final JsonParser jsonParser,
                final DeserializationContext context
        ) throws IOException {
            final ObjectMapper objectMapper = new ObjectMapper();
            final JsonNode groupsNode = objectMapper.readTree(jsonParser);
            final JsonNode itemsNode = groupsNode.get("items");
            return StreamSupport.stream(itemsNode.spliterator(), false)
                    .map(itemNode -> itemNode.get("name").asText())
                    .collect(Collectors.toList());
        }
    }
}
