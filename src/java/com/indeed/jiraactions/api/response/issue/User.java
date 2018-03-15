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
    public static final User INVALID_USER = ImmutableUser.builder()
            .displayName("No User")
            .name("")
            .key("")
            .build();

    public String getDisplayName();
    public String getName();
    public String getKey();
    @JsonDeserialize(using = GroupDeserializer.class)
    public List<String> getGroups();

    static class GroupDeserializer extends StdDeserializer<List<String>> {
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
