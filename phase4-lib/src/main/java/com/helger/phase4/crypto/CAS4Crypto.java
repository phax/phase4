package com.helger.phase4.crypto;

import javax.annotation.concurrent.Immutable;

import com.helger.security.keystore.EKeyStoreType;

/**
 * Constant values for the AS4 cryptography.
 *
 * @author Philip Helger
 */
@Immutable
public final class CAS4Crypto
{
  public static final EKeyStoreType DEFAULT_KEY_STORE_TYPE = EKeyStoreType.JKS;
  public static final EKeyStoreType DEFAULT_TRUST_STORE_TYPE = EKeyStoreType.JKS;

  private CAS4Crypto ()
  {}
}
