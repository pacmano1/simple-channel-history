package com.diridium;

/*
   Copyright [2025-2026] [Diridium Technologies Inc.]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

public class DecomposedComponent {

    public enum Category {
        CHANNEL_SCRIPT,
        CONNECTOR_CONFIGURATION,
        CONNECTOR_SCRIPT,
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
