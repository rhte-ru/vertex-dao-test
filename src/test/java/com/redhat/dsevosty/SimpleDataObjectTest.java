package com.redhat.dsevosty;

import io.vertx.core.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

public class SimpleDataObjectTest {
  @Test
  public void simpleDataObjecttoJson() {
    SimpleDataObject dto = new SimpleDataObject("1", "name 1", "1");
    JsonObject json = dto.toJson();
    System.out.println(json.toString());
    Assert.assertEquals(dto.getId(), "1");
    Assert.assertEquals(dto.getName(), "name 1");
    Assert.assertEquals(dto.getOtherReference(), "1");
  }
}
