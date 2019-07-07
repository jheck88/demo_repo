package com.tyrannos.data.collector.json;

import java.util.List;

public class MappingsModel {

    public List<DispacherServletModel> getDispacherServlet() {
        return dispacherServlet;
    }

    public void setDispacherServlet(List<DispacherServletModel> dispacherServlet) {
        this.dispacherServlet = dispacherServlet;
    }

    List<DispacherServletModel> dispacherServlet = null;
}
