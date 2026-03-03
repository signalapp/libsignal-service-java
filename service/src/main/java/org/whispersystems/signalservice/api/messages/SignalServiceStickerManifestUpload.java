package org.whispersystems.signalservice.api.messages;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SignalServiceStickerManifestUpload {

  private final Optional<String>      title;
  private final Optional<String>      author;
  private final Optional<StickerInfo> cover;
  private final List<StickerInfo>     stickers;

  public SignalServiceStickerManifestUpload(String title, String author, StickerInfo cover, List<StickerInfo> stickers) {
    this.title    = Optional.ofNullable(title);
    this.author   = Optional.ofNullable(author);
    this.cover    = Optional.ofNullable(cover);
    this.stickers = (stickers == null) ? Collections.<StickerInfo>emptyList() : new ArrayList<>(stickers);
  }

  public Optional<String> getTitle() {
    return title;
  }

  public Optional<String> getAuthor() {
    return author;
  }

  public Optional<StickerInfo> getCover() {
    return cover;
  }

  public List<StickerInfo> getStickers() {
    return stickers;
  }

  public SignalServiceStickerManifest toManifest() {
    List<SignalServiceStickerManifest.StickerInfo> stickers = new ArrayList<>();

    int i = 0;
    for (StickerInfo sticker : this.stickers) {
      stickers.add(new SignalServiceStickerManifest.StickerInfo(i, sticker.emoji, sticker.contentType));
      i++;
    }

    SignalServiceStickerManifest.StickerInfo cover = this.cover.isPresent()
                                                     ? new SignalServiceStickerManifest.StickerInfo(stickers.size(), this.cover.get().emoji, this.cover.get().contentType)
                                                     : stickers.get(0);

    return new SignalServiceStickerManifest(title.orElse(null), author.orElse(null), cover, stickers);
  }

  public static final class StickerInfo {
    private final InputStream inputStream;
    private final long        length;
    private final String      emoji;
    private final String      contentType;

    public StickerInfo(InputStream inputStream, long length, String emoji, String contentType) {
      this.inputStream = inputStream;
      this.length      = length;
      this.emoji       = emoji;
      this.contentType = contentType;
    }

    public InputStream getInputStream() {
      return inputStream;
    }

    public long getLength() {
      return length;
    }

    public String getEmoji() {
      return emoji;
    }

    public String getContentType() {
      return contentType;
    }
  }
}
