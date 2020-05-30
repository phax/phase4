package com.helger.phase4.sender;

import java.time.Duration;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import com.helger.commons.lang.TimeValue;
import com.helger.commons.state.ESuccess;
import com.helger.commons.traits.IGenericImplTrait;
import com.helger.phase4.crypto.AS4CryptoFactoryPropertiesFile;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.soap.ESoapVersion;
import com.helger.phase4.util.Phase4Exception;

/**
 * Abstract builder base class with the requirements for all message types.
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        The implementation type
 * @since 0.10.0
 */
public abstract class AbstractAS4MessageBuilder <IMPLTYPE extends AbstractAS4MessageBuilder <IMPLTYPE>> implements
                                                IGenericImplTrait <IMPLTYPE>
{
  protected IAS4CryptoFactory m_aCryptoFactory;
  protected String m_sMessageID;
  protected ESoapVersion m_eSoapVersion;
  protected int m_nMaxRetries = -1;
  protected long m_nRetryIntervalMS = -1;
  protected Locale m_aLocale = Locale.US;

  /**
   * Create a new builder, with the following fields already set:<br>
   * {@link #cryptoFactory(IAS4CryptoFactory)}<br>
   * {@link #soapVersion(ESoapVersion)}
   */
  public AbstractAS4MessageBuilder ()
  {
    // Set default values
    try
    {
      cryptoFactory (AS4CryptoFactoryPropertiesFile.getDefaultInstance ());
      soapVersion (ESoapVersion.SOAP_12);
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to init AbstractAS4MessageBuilder", ex);
    }
  }

  /**
   * @return The currently set {@link IAS4CryptoFactory}. May be
   *         <code>null</code>.
   */
  @Nullable
  public final IAS4CryptoFactory cryptoFactory ()
  {
    return m_aCryptoFactory;
  }

  /**
   * Set the crypto factory to be used. The default crypto factory uses the
   * properties from the file "crypto.properties".
   *
   * @param aCryptoFactory
   *        The crypto factory to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE cryptoFactory (@Nullable final IAS4CryptoFactory aCryptoFactory)
  {
    m_aCryptoFactory = aCryptoFactory;
    return thisAsT ();
  }

  /**
   * Set the optional AS4 message ID. If this field is not set, a random message
   * ID is created.
   *
   * @param sMessageID
   *        The optional AS4 message ID to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE messageID (@Nullable final String sMessageID)
  {
    m_sMessageID = sMessageID;
    return thisAsT ();
  }

  /**
   * @return The SOAP version to be used. May be <code>null</code>.
   */
  @Nullable
  public final ESoapVersion soapVersion ()
  {
    return m_eSoapVersion;
  }

  /**
   * Set the SOAP version to be used. Default is SOAP 1.2
   *
   * @param eSoapVersion
   *        The SOAP version to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE soapVersion (@Nullable final ESoapVersion eSoapVersion)
  {
    m_eSoapVersion = eSoapVersion;
    return thisAsT ();
  }

  @Nonnull
  public final IMPLTYPE maxRetries (final int n)
  {
    m_nMaxRetries = n;
    return thisAsT ();
  }

  @Nonnull
  public final IMPLTYPE retryInterval (final TimeValue a)
  {
    return retryIntervalMilliseconds (a == null ? null : a.getAsMillis ());
  }

  @Nonnull
  public final IMPLTYPE retryInterval (final Duration a)
  {
    return retryIntervalMilliseconds (a == null ? null : a.toMillis ());
  }

  @Nonnull
  public final IMPLTYPE retryIntervalMilliseconds (final long n)
  {
    m_nRetryIntervalMS = n;
    return thisAsT ();
  }

  @Nonnull
  public final IMPLTYPE locale (final Locale a)
  {
    m_aLocale = a;
    return thisAsT ();
  }

  @OverridingMethodsMustInvokeSuper
  public boolean isEveryRequiredFieldSet ()
  {
    // m_aCryptoFactory may be null
    // m_sMessageID is optional
    if (m_eSoapVersion == null)
      return false;

    // m_nMaxRetries doesn't matter
    // m_nRetryIntervalMS doesn't matter
    if (m_aLocale == null)
      return false;

    return true;
  }

  /**
   * Synchronously send the AS4 message. Before sending,
   * {@link #isEveryRequiredFieldSet()} is called to check that the mandatory
   * elements are set.
   *
   * @return {@link ESuccess#FAILURE} if not all mandatory parameters are set or
   *         if sending failed, {@link ESuccess#SUCCESS} upon success. Never
   *         <code>null</code>.
   * @throws Phase4Exception
   *         In case of any error
   * @see #isEveryRequiredFieldSet()
   */
  @Nonnull
  public abstract ESuccess sendMessage () throws Phase4Exception;
}
