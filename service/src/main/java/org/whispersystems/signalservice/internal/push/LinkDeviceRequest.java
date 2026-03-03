package org.whispersystems.signalservice.internal.push;

import org.whispersystems.signalservice.api.account.AccountAttributes;
import org.whispersystems.signalservice.api.push.SignedPreKeyEntity;

public record LinkDeviceRequest(String verificationCode,
                                AccountAttributes accountAttributes,
                                SignedPreKeyEntity aciSignedPreKey,
                                SignedPreKeyEntity pniSignedPreKey,
                                KyberPreKeyEntity aciPqLastResortPreKey,
                                KyberPreKeyEntity pniPqLastResortPreKey
) {
}
