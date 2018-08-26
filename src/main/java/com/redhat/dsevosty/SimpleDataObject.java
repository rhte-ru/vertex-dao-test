package com.redhat.dsevosty;

import java.util.UUID;
import io.vertx.core.json.JsonObject;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.ModuleGen;

@DataObject(generateConverter = true)
public class SimpleDataObject {

    private UUID   id;
    private String name;
    private UUID   otherReference;

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
        SimpleDataObjectConverter(json, this);
    }

    private UUID getDefaultId() {
        return UUID.randomUUID();
    }
    
    public UUID getId() {
        return id;
    }
     public void setId(UUID id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public UUID getOtherReference() {
        return otherReference;
    }
}
