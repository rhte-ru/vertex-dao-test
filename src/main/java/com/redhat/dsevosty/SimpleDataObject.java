package com.redhat.dsevosty;

import java.util.Objects;
import java.util.UUID;
import io.vertx.core.json.JsonObject;

public class SimpleDataObject implements AbstractDataObject {

    @SuppressWarnings("unused")
    private static final long serialVersionUID = 1;

    private UUID id;
    private String name;
    private UUID otherReference;

    public SimpleDataObject() {
        this.id = defaultId();
        this.name = "default name for: " + id.toString();
        this.otherReference = null;
    }

    public SimpleDataObject(SimpleDataObject other) {
        this.id = other.id;
        this.name = other.name;
        this.otherReference = other.otherReference;
    }

    public SimpleDataObject(UUID id, String name, UUID ref) {
        this.id = id;
        this.name = name;
        this.otherReference = ref;
    }

    public SimpleDataObject(JsonObject json) {
        String val;
        val = json.getString("id");
        if (val != null) {
            id = UUID.fromString(val);
        }
        val = json.getString("name");
        if (val != null) {
            name = val;
        }
        val = json.getString("otherReference");
        if (val != null) {
            otherReference = UUID.fromString(val);
        }
    }

    public static UUID defaultId() {
        return UUID.randomUUID();
    }

    @Override
    public UUID getId() {
        if (id == null) {
            id = defaultId();
        }
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

    public void setOtherReference(UUID otherReference) {
        this.otherReference = otherReference;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (id != null) {
            json.put("id", id.toString());
        }
        if (name != null) {
            json.put("name", name);
        }
        if (otherReference != null) {
            json.put("otherReference", otherReference.toString());
        }
        return json;
    }

    public String toString() {
        return toStringAbstract();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getOtherReference());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof SimpleDataObject)) {
            return false;
        }
        SimpleDataObject other = (SimpleDataObject) o;
        return Objects.equals(getId(), other.getId()) && Objects.equals(getName(), other.getName())
                && Objects.equals(getOtherReference(), other.getOtherReference());
    }
}
