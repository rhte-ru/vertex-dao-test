package com.redhat.dsevosty;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

public class AbstractFataObjectTest {

    @Test
    public void testAbstractDataObjectToString() {
        AbstractDataObject dto = new AbstractDataObject() {
            @SuppressWarnings("unused")
            private static final long serialVersionUID = 0;
            private int id = 1;
            private String name = "name 1";

            @SuppressWarnings("unused")
            public String getName() {
                return name;
            }

            @SuppressWarnings("unused")
            public int getId() {
                return id;
            }

        };
        System.out.println(dto.toStringAbstract());
        assertThat(dto.toStringAbstract()).contains("id: 1");
        assertThat(dto.toStringAbstract()).contains("name: \"name 1\"");
    }
}
