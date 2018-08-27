package com.redhat.dsevosty;

import java.util.UUID;
import io.vertx.core.json.JsonObject;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.ModuleGen;

@DataObject(generateConverter = true)
public class SimpleDataObject {

    private String id;
    private String name;
    private String otherReference;

    public SimpleDataObject() {
        this.id = getDefaultId();
        this.name = "default name for: " + id.toString();
        this.otherReference = null;
    }

    public SimpleDataObject(SimpleDataObject other) {
        this.id = other.id;
        this.name = other.name;
        this.otherReference = other.otherReference;
    }

    public SimpleDataObject(JsonObject json) {
        SimpleDataObjectConverter.fromJson(json, this);
    }

    protected String getDefaultId() {
        return UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOtherReference() {
        return otherReference;
    }

    public void setOtherReference(String otherReference) {
        this.otherReference = otherReference;
    }
}
