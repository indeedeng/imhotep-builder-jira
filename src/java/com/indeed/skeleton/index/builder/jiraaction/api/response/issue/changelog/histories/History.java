package com.indeed.skeleton.index.builder.jiraaction.api.response.issue.changelog.histories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.indeed.common.datastruct.StringArray;
import com.indeed.common.util.StringUtils;
import com.indeed.skeleton.index.builder.jiraaction.api.response.issue.User;
import org.apache.lucene.analysis.kr.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by soono on 8/25/16.
 */

@JsonIgnoreProperties(ignoreUnknown=true)

public class History {
    public User author;
    public String created;
    public Item[] items;

    public String getChangedFields() {
        StringBuilder fieldsChanged = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i != 0) fieldsChanged.append(" ");
            fieldsChanged.append(items[i].field);
        }
        return fieldsChanged.toString();
    }

    public boolean itemExist(String field) {
        for (Item item : items) {
            if (item.field.equals(field)) return true;
        }
        return false;
    }

    public String getItemLastValue(String field) {
        for (Item item : items) {
            if (item.field.equals(field)) {
                String toString = item.toString;
                return toString == null ? "" : toString;
            }
        }
        return "";
    }
}
