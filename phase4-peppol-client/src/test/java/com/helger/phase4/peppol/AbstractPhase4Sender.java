package com.helger.phase4.peppol;

import java.security.cert.X509Certificate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.security.certificate.CertificateDecodeHelper;

/**
 * Base class to add a single sanity method
 *
 * @author Philip Helger
 */
public abstract class AbstractPhase4Sender
{
  @Nullable
  protected static X509Certificate pem2cert (@NonNull final String sCert)
  {
    return new CertificateDecodeHelper ().source (sCert).pemEncoded (true).getDecodedOrNull ();
  }
}
