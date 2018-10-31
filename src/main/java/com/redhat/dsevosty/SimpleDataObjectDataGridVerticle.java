package com.redhat.dsevosty;

import io.vertx.core.json.JsonObject;

public class SimpleDataObjectDataGridVerticle extends DataGridVerticle {

  @Override
  protected SimpleDataObject abstractObjectFromJson(JsonObject json) {
    return new SimpleDataObject(json);
  }
}
