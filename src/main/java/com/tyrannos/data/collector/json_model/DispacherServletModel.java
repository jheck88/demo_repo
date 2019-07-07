package com.tyrannos.data.collector.json_model;

public class DispacherServletModel {
    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public String getPredicate() {
        return predicate;
    }

    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public RequestMappingConditionsModel getRequestMappingConditions() {
        return requestMappingConditions;
    }

    public void setRequestMappingConditions(RequestMappingConditionsModel requestMappingConditions) {
        this.requestMappingConditions = requestMappingConditions;
    }

    String handler = null;
    String predicate = null;
    String details = null;
    RequestMappingConditionsModel requestMappingConditions = null;
}
