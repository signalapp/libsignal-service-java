package org.whispersystems.signalservice.api.util;

import org.whispersystems.libsignal.util.guava.Optional;

import java.util.UUID;
import java.util.regex.Pattern;

public final class UuidUtil {

  private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", Pattern.CASE_INSENSITIVE);

  private UuidUtil() { }

  public static Optional<UUID> parse(String uuid) {
    return Optional.fromNullable(parseOrNull(uuid));
  }

  public static UUID parseOrNull(String uuid) {
    return isUuid(uuid) ? parseOrThrow(uuid) : null;
  }

  public static UUID parseOrThrow(String uuid) {
    return UUID.fromString(uuid);
  }

  public static boolean isUuid(String uuid) {
    return uuid != null && UUID_PATTERN.matcher(uuid).matches();
  }
}
