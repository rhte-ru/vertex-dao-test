package com.redhat.dsevosty;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

// import io.vertx.core.json.JsonObject;

public class SimpleDataObjectTest {
  @Test
  public void simpleDataObjecttoJson() {
    SimpleDataObject dto = new SimpleDataObject("1", "name 1", "1");
    // JsonObject json = dto.toJson();
    // System.out.println(json.toString());
    assertThat(dto.getId()).isEqualTo("1");
    assertThat(dto.getName()).isEqualTo("name 1");
    assertThat(dto.getOtherReference()).isEqualTo("1");
  }
}
