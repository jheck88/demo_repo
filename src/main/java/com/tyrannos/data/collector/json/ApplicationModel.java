package com.tyrannos.data.collector.json;

public class ApplicationModel {

    public MappingsModel getMappings() {
        return mappings;
    }

    public void setMappings(MappingsModel mappings) {
        this.mappings = mappings;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    MappingsModel mappings = null;
    String parentId = null;
}

