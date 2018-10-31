package com.redhat.dsevosty;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

import java.util.UUID;

public class AbstractDataObjectTest {

    @Test
    public void testAbstractDataObjectToString() {
        UUID myid = UUID.randomUUID();
        AbstractDataObject dto = new AbstractDataObject() {
            @SuppressWarnings("unused")
            private static final long serialVersionUID = 0;
            private UUID id = myid;
            private String name = "name 1";

            public String getName() {
                return name;
            }

            public UUID getId() {
                return id;
            }

            @Override
			public JsonObject toJson() {
                JsonObject json = new JsonObject();
                json.put("id", getId().toString());
                json.put("name", getName());
				return json;
			}

        };
        System.out.println(dto.toStringAbstract());
        assertThat(dto.toJson().toString()).contains("\"id\":\"" + myid + "\"");
        assertThat(dto.toJson().toString()).contains("\"name\":\"name 1\"");
    }
}
