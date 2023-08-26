package org.whispersystems.signalservice.internal.serialize

import okio.ByteString
import org.whispersystems.signalservice.api.messages.SignalServiceMetadata
import org.whispersystems.signalservice.internal.serialize.SignalServiceAddressProtobufSerializer.fromProtobuf
import org.whispersystems.signalservice.internal.serialize.SignalServiceAddressProtobufSerializer.toProtobuf
import org.whispersystems.signalservice.internal.serialize.protos.MetadataProto
import java.util.Optional

object SignalServiceMetadataProtobufSerializer {
  fun toProtobuf(metadata: SignalServiceMetadata): MetadataProto {
    val builder = MetadataProto.Builder()
      .address(toProtobuf(metadata.sender))
      .senderDevice(metadata.senderDevice)
      .needsReceipt(metadata.isNeedsReceipt)
      .timestamp(metadata.timestamp)
      .serverReceivedTimestamp(metadata.serverReceivedTimestamp)
      .serverDeliveredTimestamp(metadata.serverDeliveredTimestamp)
      .serverGuid(metadata.serverGuid)
      .destinationUuid(metadata.destinationUuid)
    if (metadata.groupId.isPresent) {
      builder.groupId(ByteString.of(*metadata.groupId.get()))
    }
    return builder.build()
  }

  fun fromProtobuf(metadata: MetadataProto): SignalServiceMetadata {
    if (metadata.address == null) {
      throw IllegalArgumentException("Missing address in metadata")
    }
    return SignalServiceMetadata(
      fromProtobuf(metadata.address),
      metadata.senderDevice ?: 0,
      metadata.timestamp ?: 0,
      metadata.serverReceivedTimestamp ?: 0,
      metadata.serverDeliveredTimestamp ?: 0,
      metadata.needsReceipt ?: false,
      metadata.serverGuid,
      Optional.ofNullable(metadata.groupId?.toByteArray()),
      metadata.destinationUuid
    )
  }
}
