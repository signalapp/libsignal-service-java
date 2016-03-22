/**
 * Copyright (C) 2014 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.whispersystems.signalservice.api.push;

import org.whispersystems.libsignal.util.guava.Optional;

/**
 * A class representing a message destination or origin.
 */
public class SignalServiceAddress {

  public static final int DEFAULT_DEVICE_ID = 1;

  private final String e164number;
  private final Optional<String> relay;

  /**
   * Construct a PushAddress.
   *
   * @param e164number The Signal Service username of this destination (eg e164 representation of a phone number).
   * @param relay The Signal SErvicefederated server this user is registered with (if not your own server).
   */
  public SignalServiceAddress(String e164number, Optional<String> relay) {
    this.e164number  = e164number;
    this.relay       = relay;
  }

  public SignalServiceAddress(String e164number) {
    this(e164number, Optional.<String>absent());
  }

  public String getNumber() {
    return e164number;
  }

  public Optional<String> getRelay() {
    return relay;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof SignalServiceAddress)) return false;

    SignalServiceAddress that = (SignalServiceAddress)other;

    return equals(this.e164number, that.e164number) &&
           equals(this.relay, that.relay);
  }

  @Override
  public int hashCode() {
    int hashCode = 0;

    if (this.e164number != null) hashCode ^= this.e164number.hashCode();
    if (this.relay.isPresent())  hashCode ^= this.relay.get().hashCode();

    return hashCode;
  }

  private boolean equals(String one, String two) {
    if (one == null) return two == null;
    return one.equals(two);
  }

  private boolean equals(Optional<String> one, Optional<String> two) {
    if (one.isPresent()) return two.isPresent() && one.get().equals(two.get());
    else                 return !two.isPresent();
  }
}
