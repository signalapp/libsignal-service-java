package org.whispersystems.signalservice.internal.push;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class StickerUploadAttributesResponse {
  @JsonProperty
  private String packId;

  @JsonProperty
  private StickerUploadAttributes manifest;

  @JsonProperty
  private List<StickerUploadAttributes> stickers;

  public String getPackId() {
    return packId;
  }

  public StickerUploadAttributes getManifest() {
    return manifest;
  }

  public List<StickerUploadAttributes> getStickers() {
    return stickers;
  }
}
