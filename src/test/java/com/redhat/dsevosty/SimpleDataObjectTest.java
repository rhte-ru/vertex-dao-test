package com.redhat.dsevosty;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class SimpleDataObjectTest {
  @Test
  public void simpleDataObjecttoJson() {
    UUID id = UUID.randomUUID();
    SimpleDataObject dto = new SimpleDataObject(id, "name 1", id);
    assertThat(dto.getId()).isEqualTo(id);
    assertThat(dto.getName()).isEqualTo("name 1");
    assertThat(dto.getOtherReference()).isEqualTo(id);
  }
}
