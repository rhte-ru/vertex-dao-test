package com.redhat.dsevosty;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class SimpleDataObjectTest {
  @Test
  public void simpleDataObjecttoJson() {
    SimpleDataObject dto = new SimpleDataObject(UUID.fromSt4ring("1"), "name 1", "1");
    assertThat(dto.getId()).isEqualTo("1");
    assertThat(dto.getName()).isEqualTo("name 1");
    assertThat(dto.getOtherReference()).isEqualTo("1");
  }
}
