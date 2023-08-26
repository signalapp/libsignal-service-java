package org.whispersystems.signalservice.internal.serialize

import org.whispersystems.signalservice.api.push.ServiceId.Companion.parseOrThrow
import org.whispersystems.signalservice.api.push.SignalServiceAddress
import org.whispersystems.signalservice.internal.serialize.protos.AddressProto
import java.util.Optional

object SignalServiceAddressProtobufSerializer {
  @JvmStatic
  fun toProtobuf(signalServiceAddress: SignalServiceAddress): AddressProto {
    val builder = AddressProto.Builder()
    builder.uuid(signalServiceAddress.serviceId.toByteString())
    if (signalServiceAddress.number.isPresent) {
      builder.e164(signalServiceAddress.number.get())
    }
    return builder.build()
  }

  @JvmStatic
  fun fromProtobuf(addressProto: AddressProto): SignalServiceAddress {
    if (addressProto.uuid == null) {
      throw IllegalArgumentException("Missing ServiceId!")
    }
    val serviceId = parseOrThrow(
      addressProto.uuid.toByteArray()
    )
    val number = Optional.ofNullable(addressProto.e164)
    return SignalServiceAddress(serviceId, number)
  }
}
