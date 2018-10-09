package com.redhat.dsevosty;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class AbstractFataObjectTest {

   @Test
    public void testAbstractDataObjectToString() {
        AbstractDataObject dto = new AbstractDataObject() {
            private int    id   = 1;
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
        Assert.assertThat(dto.toStringAbstract(), CoreMatchers.containsString("id: 1"));
        Assert.assertThat(dto.toStringAbstract(), CoreMatchers.containsString("name: \"name 1\""));
    }
}
