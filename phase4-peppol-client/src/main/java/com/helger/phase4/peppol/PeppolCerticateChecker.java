/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phase4.peppol;

import java.security.GeneralSecurityException;
import java.security.Security;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.security.auth.x500.X500Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.cache.MappedCache;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.timing.StopWatch;
import com.helger.peppol.utils.PeppolKeyStoreHelper;

/**
 * The Peppol certificate checker
 *
 * @author Philip Helger
 */
public final class PeppolCerticateChecker
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolCerticateChecker.class);

  /** Sorted list with all issuers we're accepting. Never empty. */
  private static final ICommonsList <X509Certificate> PEPPOL_AP_CA_CERTS = new CommonsArrayList <> ();
  private static final ICommonsList <X500Principal> PEPPOL_AP_CA_ISSUERS;
  private static final ICommonsSet <TrustAnchor> PEPPOL_AP_TRUST_ANCHORS;

  static
  {
    // PKI v3
    PEPPOL_AP_CA_CERTS.add (PeppolKeyStoreHelper.Config2018.CERTIFICATE_PILOT_AP);
    PEPPOL_AP_CA_CERTS.add (PeppolKeyStoreHelper.Config2018.CERTIFICATE_PRODUCTION_AP);
    // PKI v2 after v3 because lower precedence
    PEPPOL_AP_CA_CERTS.add (PeppolKeyStoreHelper.Config2010.CERTIFICATE_PILOT_AP);
    PEPPOL_AP_CA_CERTS.add (PeppolKeyStoreHelper.Config2010.CERTIFICATE_PRODUCTION_AP);

    // all issuers
    PEPPOL_AP_CA_ISSUERS = new CommonsArrayList <> (PEPPOL_AP_CA_CERTS, X509Certificate::getSubjectX500Principal);

    // Certificate -> trust anchors; name constraints MUST be null
    PEPPOL_AP_TRUST_ANCHORS = new CommonsHashSet <> (PEPPOL_AP_CA_CERTS, x -> new TrustAnchor (x, null));
  }

  private static final AtomicBoolean CACHE_OCSP_RESULTS = new AtomicBoolean (true);
  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("s_aRWLock")
  private static Consumer <? super GeneralSecurityException> s_aExceptionHdl = ex -> LOGGER.warn ("Certificate is revoked",
                                                                                                  ex);
  private static final MappedCache <X509Certificate, String, Boolean> OCSP_CACHE = new MappedCache <> (aCert -> aCert.getSubjectX500Principal ()
                                                                                                                     .getName () +
                                                                                                                "-" +
                                                                                                                aCert.getSerialNumber ()
                                                                                                                     .toString (),
                                                                                                       aCert -> Boolean.valueOf (isPeppolAPCertificateRevoked (aCert,
                                                                                                                                                               null,
                                                                                                                                                               getExceptionHdl ())),
                                                                                                       1000,
                                                                                                       "peppol-oscp-cache",
                                                                                                       false);

  public PeppolCerticateChecker ()
  {}

  public static boolean isCacheOCSPResults ()
  {
    return CACHE_OCSP_RESULTS.get ();
  }

  public static void setCacheOCSPResults (final boolean bCache)
  {
    CACHE_OCSP_RESULTS.set (bCache);
  }

  @Nonnull
  public static Consumer <? super GeneralSecurityException> getExceptionHdl ()
  {
    return s_aRWLock.readLocked ( () -> s_aExceptionHdl);
  }

  public static void setExceptionHdl (@Nonnull final Consumer <? super GeneralSecurityException> aExceptionHdl)
  {
    ValueEnforcer.notNull (aExceptionHdl, "ExceptionHdl");
    s_aRWLock.writeLocked ( () -> s_aExceptionHdl = aExceptionHdl);
  }

  /**
   * This is the unconditional
   *
   * @param aCert
   *        The certificate to be check. May not be <code>null</code>.
   * @param aCheckDT
   *        The check date time. May be <code>null</code>.
   * @param aExceptionHdl
   *        The exception handler to be used. May not be <code>null</code>.
   * @return <code>true</code> if it is revoked, <code>false</code> if not.
   */
  public static boolean isPeppolAPCertificateRevoked (@Nonnull final X509Certificate aCert,
                                                      @Nullable final LocalDateTime aCheckDT,
                                                      @Nonnull final Consumer <? super GeneralSecurityException> aExceptionHdl)
  {
    ValueEnforcer.notNull (aCert, "Cert");
    ValueEnforcer.notNull (aExceptionHdl, "ExceptionHdl");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Performing certificate revocation check on certificate '" +
                    aCert.getSubjectX500Principal ().getName () +
                    "'" +
                    (aCheckDT != null ? " for datetime " + aCheckDT : ""));

    // check OCSP and CLR
    final StopWatch aSW = StopWatch.createdStarted ();
    try
    {
      final X509CertSelector aSelector = new X509CertSelector ();
      aSelector.setCertificate (aCert);
      final PKIXBuilderParameters aPKIXParams = new PKIXBuilderParameters (PEPPOL_AP_TRUST_ANCHORS.getClone (),
                                                                           aSelector);

      aPKIXParams.setRevocationEnabled (true);

      // Enable On-Line Certificate Status Protocol (OCSP) support
      final boolean bEnableOCSP = true;
      Security.setProperty ("ocsp.enable", Boolean.toString (bEnableOCSP));

      if (aCheckDT != null)
      {
        // Check at what date?
        final Date aCheckDate = PDTFactory.createDate (aCheckDT);
        aPKIXParams.setDate (aCheckDate);
      }

      // Specify a list of intermediate certificates ("Collection" is a key in
      // the "SUN" security provider)
      final CertStore aIntermediateCertStore = CertStore.getInstance ("Collection",
                                                                      new CollectionCertStoreParameters (PEPPOL_AP_CA_CERTS));
      aPKIXParams.addCertStore (aIntermediateCertStore);

      // Throws an exception in case of an error
      final CertPathBuilder aCPB = CertPathBuilder.getInstance ("PKIX");
      final PKIXCertPathBuilderResult aBuilderResult = (PKIXCertPathBuilderResult) aCPB.build (aPKIXParams);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("OCSP/CLR builder result = " + aBuilderResult);

      final CertPathValidator aCPV = CertPathValidator.getInstance ("PKIX");
      final PKIXCertPathValidatorResult aValidateResult = (PKIXCertPathValidatorResult) aCPV.validate (aBuilderResult.getCertPath (),
                                                                                                       aPKIXParams);
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("OCSP/CLR validation result = " + aValidateResult);

      return false;
    }
    catch (final GeneralSecurityException ex)
    {
      aExceptionHdl.accept (ex);
      return true;
    }
    finally
    {
      final long nMillis = aSW.stopAndGetMillis ();
      if (nMillis > 500)
        LOGGER.warn ("OCSP/CLR revocation check took " + nMillis + " milliseconds which is too long");
      else
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("OCSP/CLR revocation check took " + nMillis + " milliseconds");
    }
  }

  /**
   * Check if the provided certificate is a valid Peppol AP certificate.
   *
   * @param aCert
   *        The certificate to be checked. May be <code>null</code>.
   * @param aCheckDT
   *        The check date and time to use. May not be <code>null</code>.
   * @return {@link EPeppolCertificateCheckResult} and never <code>null</code>.
   */
  @Nonnull
  public static EPeppolCertificateCheckResult checkPeppolAPCertificate (@Nullable final X509Certificate aCert,
                                                                        @Nonnull final LocalDateTime aCheckDT)
  {
    if (aCert == null)
      return EPeppolCertificateCheckResult.NO_CERTIFICATE_PROVIDED;

    // Check date valid
    final Date aCheckDate = PDTFactory.createDate (aCheckDT);
    try
    {
      aCert.checkValidity (aCheckDate);
    }
    catch (final CertificateNotYetValidException ex)
    {
      return EPeppolCertificateCheckResult.NOT_YET_VALID;
    }
    catch (final CertificateExpiredException ex)
    {
      return EPeppolCertificateCheckResult.EXPIRED;
    }

    // Check if issuer is known
    final X500Principal aIssuer = aCert.getIssuerX500Principal ();
    if (!PEPPOL_AP_CA_ISSUERS.contains (aIssuer))
    {
      // Not a PEPPOL AP certificate
      return EPeppolCertificateCheckResult.UNSUPPORTED_ISSUER;
    }

    // Check OCSP/CLR
    if (isCacheOCSPResults ())
    {
      final boolean bRevoked = OCSP_CACHE.getFromCache (aCert).booleanValue ();
      if (bRevoked)
        return EPeppolCertificateCheckResult.REVOKED;
    }
    else
    {
      if (isPeppolAPCertificateRevoked (aCert, aCheckDT, getExceptionHdl ()))
        return EPeppolCertificateCheckResult.REVOKED;
    }

    return EPeppolCertificateCheckResult.VALID;
  }
}
