// SPDX-FileCopyrightText: Copyright 2025-2026 Diridium Technologies Inc.
// SPDX-License-Identifier: MPL-2.0

package com.diridium;

/**
 * DTO representing a deleted channel or code template snapshot.
 */
public class DeletedItemInfo {
    public static final String TYPE_CHANNEL = "Channel";
    public static final String TYPE_CODE_TEMPLATE = "Code Template";

    private long id;
    private String itemId;
    private String name;
    private String type;
    private String deletedBy;
    private long dateDeleted;

    public DeletedItemInfo() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    public long getDateDeleted() {
        return dateDeleted;
    }

    public void setDateDeleted(long dateDeleted) {
        this.dateDeleted = dateDeleted;
    }
}
