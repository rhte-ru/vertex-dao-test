package com.redhat.dsevosty;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

import java.util.UUID;

public class AbstractFataObjectTest {

    @Test
    public void testAbstractDataObjectToString() {
        AbstractDataObject dto = new AbstractDataObject() {
            @SuppressWarnings("unused")
            private static final long serialVersionUID = 0;
            private UUID id = UUID.fromString("1");
            private String name = "name 1";

            @SuppressWarnings("unused")
            public String getName() {
                return name;
            }

            // @SuppressWarnings("unused")
            @Override
            public UUID getId() {
                return id;
            }

            @Override
			public JsonObject toJson() {
                JsonObject json = new JsonObject();
                json.put("id", getId());
                json.put("name", getName());
				return json;
			}

        };
        System.out.println(dto.toStringAbstract());
        assertThat(dto.toStringAbstract()).contains("id: 1");
        assertThat(dto.toStringAbstract()).contains("name: \"name 1\"");
    }
}
