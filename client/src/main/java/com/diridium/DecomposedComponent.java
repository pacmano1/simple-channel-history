// SPDX-FileCopyrightText: Copyright 2025-2026 Diridium Technologies Inc.
// SPDX-License-Identifier: MPL-2.0

package com.diridium;

public class DecomposedComponent {

    public enum Category {
        CHANNEL_SCRIPT,
        CONNECTOR_CONFIGURATION,
        CONNECTOR_SCRIPT,
        CONNECTOR_PLUGIN,
        FILTER,
        TRANSFORMER,
        RESPONSE_TRANSFORMER,
        CHANNEL_PROPERTIES
    }

    private final String key;
    private final String displayName;
    private final String content;
    private final Category category;
    private final String parentGroup;

    public DecomposedComponent(String key, String displayName, String content, Category category, String parentGroup) {
        this.key = key;
        this.displayName = displayName;
        this.content = content;
        this.category = category;
        this.parentGroup = parentGroup;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getContent() {
        return content;
    }

    public Category getCategory() {
        return category;
    }

    public String getParentGroup() {
        return parentGroup;
    }
}
