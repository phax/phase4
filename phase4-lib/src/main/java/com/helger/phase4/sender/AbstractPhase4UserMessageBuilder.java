package com.helger.phase4.sender;

import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.traits.IGenericImplTrait;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientSettings;
import com.helger.phase4.attachment.IIncomingAttachmentFactory;
import com.helger.phase4.attachment.Phase4OutgoingAttachment;
import com.helger.phase4.client.AS4ClientUserMessage;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.client.IAS4RawResponseConsumer;
import com.helger.phase4.client.IAS4RetryCallback;
import com.helger.phase4.client.IAS4SignalMessageConsumer;
import com.helger.phase4.crypto.AS4CryptoFactoryPropertiesFile;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.IAS4IncomingDumper;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.ebms3header.Ebms3Property;
import com.helger.phase4.messaging.domain.MessageHelperMethods;
import com.helger.phase4.model.MessageProperty;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.model.pmode.resolve.DefaultPModeResolver;
import com.helger.phase4.model.pmode.resolve.IPModeResolver;
import com.helger.phase4.soap.ESoapVersion;
import com.helger.phase4.util.Phase4Exception;

/**
 * Abstract builder base class with the minimum requirements configuration
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        The implementation type
 * @since 0.10.0
 */
public abstract class AbstractPhase4UserMessageBuilder <IMPLTYPE extends AbstractPhase4UserMessageBuilder <IMPLTYPE>> implements
                                                       IGenericImplTrait <IMPLTYPE>
{
  private HttpClientFactory m_aHttpClientFactory;
  protected IAS4CryptoFactory m_aCryptoFactory;
  protected IPModeResolver m_aPModeResolver;
  protected IIncomingAttachmentFactory m_aIAF;
  private IPMode m_aPMode;
  private ESoapVersion m_eSoapVersion;

  private String m_sServiceType;
  private String m_sService;
  private String m_sAction;
  private String m_sAgreementRef;
  private String m_sPModeID;

  private String m_sFromPartyIDType;
  private String m_sFromPartyID;
  private String m_sFromRole;

  private String m_sToPartyIDType;
  private String m_sToPartyID;
  private String m_sToRole;

  private String m_sMessageID;
  private String m_sConversationID;

  private final ICommonsList <MessageProperty> m_aMessageProperties = new CommonsArrayList <> ();

  private X509Certificate m_aReceiverCertificate;
  protected String m_sEndointURL;

  protected final ICommonsList <Phase4OutgoingAttachment> m_aAttachments = new CommonsArrayList <> ();

  protected IAS4ClientBuildMessageCallback m_aBuildMessageCallback;
  protected IAS4OutgoingDumper m_aOutgoingDumper;
  protected IAS4IncomingDumper m_aIncomingDumper;
  protected IAS4RetryCallback m_aRetryCallback;
  protected IAS4RawResponseConsumer m_aResponseConsumer;
  protected IAS4SignalMessageConsumer m_aSignalMsgConsumer;

  /**
   * Create a new builder, with the following fields already set:<br>
   * {@link #httpClientFactory(HttpClientFactory)}<br>
   * {@link #hryptoFactory(IAS4CryptoFactory)}<br>
   * {@link #pmodeResolver(IPModeResolver)}<br>
   * {@link #incomingAttachmentFactory(IIncomingAttachmentFactory)}<br>
   * {@link #pmode(IPMode)}<br>
   * {@link #soapVersion(ESoapVersion)}
   */
  public AbstractPhase4UserMessageBuilder ()
  {
    // Set default values
    try
    {
      httpClientFactory (new HttpClientFactory ());
      cryptoFactory (AS4CryptoFactoryPropertiesFile.getDefaultInstance ());
      final IPModeResolver aPModeResolver = DefaultPModeResolver.DEFAULT_PMODE_RESOLVER;
      pmodeResolver (aPModeResolver);
      incomingAttachmentFactory (IIncomingAttachmentFactory.DEFAULT_INSTANCE);
      pmode (aPModeResolver.getPModeOfID (null, "s", "a", "i", "r", null));
      soapVersion (ESoapVersion.SOAP_12);
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to init AS4 Client builder", ex);
    }
  }

  /**
   * @return The currently set {@link HttpClientFactory}. May be
   *         <code>null</code>.
   */
  @Nullable
  public final HttpClientFactory httpClientFactory ()
  {
    return m_aHttpClientFactory;
  }

  /**
   * Set the HTTP client factory to be used. If the passed settings are
   * provided, a new {@link HttpClientFactory} is created with them.
   *
   * @param aHttpClientSettings
   *        The new HTTP client settings to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE httpClientFactory (@Nullable final HttpClientSettings aHttpClientSettings)
  {
    return httpClientFactory (aHttpClientSettings == null ? null : new HttpClientFactory (aHttpClientSettings));
  }

  /**
   * Set the HTTP client factory to be used. By default an instance of
   * {@link HttpClientFactory} is used and there is no need to invoke this
   * method.
   *
   * @param aHttpClientFactory
   *        The new HTTP client factory to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE httpClientFactory (@Nullable final HttpClientFactory aHttpClientFactory)
  {
    m_aHttpClientFactory = aHttpClientFactory;
    return thisAsT ();
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
   * @return The currently set {@link IPModeResolver}. May be <code>null</code>.
   */
  @Nullable
  public final IPModeResolver pmodeResolver ()
  {
    return m_aPModeResolver;
  }

  /**
   * Set the PMode resolver to be used.
   *
   * @param aPModeResolver
   *        The PMode resolver to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE pmodeResolver (@Nullable final IPModeResolver aPModeResolver)
  {
    m_aPModeResolver = aPModeResolver;
    return thisAsT ();
  }

  /**
   * @return The currently set {@link IIncomingAttachmentFactory}. May be
   *         <code>null</code>.
   */
  @Nullable
  public final IIncomingAttachmentFactory incomingAttachmentFactory ()
  {
    return m_aIAF;
  }

  /**
   * Set the incoming attachment factory to be used.
   *
   * @param aIAF
   *        The incoming attachment factory to be used. May be
   *        <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE incomingAttachmentFactory (@Nullable final IIncomingAttachmentFactory aIAF)
  {
    m_aIAF = aIAF;
    return thisAsT ();
  }

  /**
   * @return The currently set P-Mode. May be <code>null</code>.
   */
  @Nullable
  public final IPMode pmode ()
  {
    return m_aPMode;
  }

  /**
   * Set the PMode to be used. By default a generic PMode is used.
   *
   * @param aPMode
   *        The PMode to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE pmode (@Nullable final IPMode aPMode)
  {
    m_aPMode = aPMode;
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

  /**
   * Set the "Service" value only, leaving the type <code>null</code>.
   *
   * @param sServiceValue
   *        Service value. May be <code>null</code>.
   * @return this for chaining.
   */
  @Nonnull
  public final IMPLTYPE service (@Nullable final String sServiceValue)
  {
    return service (null, sServiceValue);
  }

  /**
   * Set the "Service" value consisting of type and value. It's optional. If the
   * "Service" value is not set, it the "service type" defaults to the "process
   * identifier scheme" and the "service value" defaults to the "process
   * identifier value".
   *
   * @param sServiceType
   *        Service type. May be <code>null</code>.
   * @param sServiceValue
   *        Service value. May be <code>null</code>.
   * @return this for chaining.
   */
  @Nonnull
  public final IMPLTYPE service (@Nullable final String sServiceType, @Nullable final String sServiceValue)
  {
    m_sServiceType = sServiceType;
    m_sService = sServiceValue;
    return thisAsT ();
  }

  /**
   * Set the "Action" value. It's optional. If the "Action" value is not set, it
   * defaults to the "document type identifier value" (URI encoded).
   *
   * @param sAction
   *        Action value. May be <code>null</code>.
   * @return this for chaining.
   */
  @Nonnull
  public final IMPLTYPE action (@Nullable final String sAction)
  {
    m_sAction = sAction;
    return thisAsT ();
  }

  /**
   * Set the "AgreementRef" value. It's optional.
   *
   * @param sAgreementRef
   *        Agreement reference. May be <code>null</code>.
   * @return this for chaining.
   */
  @Nonnull
  public final IMPLTYPE areementRef (@Nullable final String sAgreementRef)
  {
    m_sAgreementRef = sAgreementRef;
    return thisAsT ();
  }

  /**
   * Set the optional PMode ID for packaging in the user message.
   *
   * @param s
   *        Pmode ID. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE pmodeID (@Nullable final String s)
  {
    m_sPModeID = s;
    return thisAsT ();
  }

  /**
   * Set the "from party ID type".
   *
   * @param sFromPartyIDType
   *        The from party ID.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE fromPartyIDType (@Nullable final String sFromPartyIDType)
  {
    m_sFromPartyIDType = sFromPartyIDType;
    return thisAsT ();
  }

  /**
   * Set the "from party ID".
   *
   * @param sFromPartyID
   *        The from party ID.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE fromPartyID (@Nullable final String sFromPartyID)
  {
    m_sFromPartyID = sFromPartyID;
    return thisAsT ();
  }

  /**
   * Set the "from party role". This is optional
   *
   * @param sFromRole
   *        The from role. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE fromRole (@Nullable final String sFromRole)
  {
    m_sFromRole = sFromRole;
    return thisAsT ();
  }

  /**
   * Set the "to party ID type".
   *
   * @param sToPartyIDType
   *        The to party ID.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE toPartyIDType (@Nullable final String sToPartyIDType)
  {
    m_sToPartyIDType = sToPartyIDType;
    return thisAsT ();
  }

  /**
   * Set the "to party ID".
   *
   * @param sToPartyID
   *        The to party ID.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE toPartyID (@Nullable final String sToPartyID)
  {
    m_sToPartyID = sToPartyID;
    return thisAsT ();
  }

  /**
   * Set the "to party role". This is optional
   *
   * @param sToRole
   *        The to role. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE toRole (@Nullable final String sToRole)
  {
    m_sToRole = sToRole;
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
   * Set the optional AS4 conversation ID. If this field is not set, a random
   * conversation ID is created.
   *
   * @param sConversationID
   *        The optional AS4 conversation ID to be used. May be
   *        <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE conversationID (@Nullable final String sConversationID)
  {
    m_sConversationID = sConversationID;
    return thisAsT ();
  }

  @Nonnull
  public final IMPLTYPE addMessageProperty (@Nullable final Ebms3Property a)
  {
    return addMessageProperty (a == null ? null : MessageProperty.builder (a));
  }

  @Nonnull
  public final IMPLTYPE addMessageProperty (@Nullable final MessageProperty.Builder a)
  {
    return addMessageProperty (a == null ? null : a.build ());
  }

  @Nonnull
  public final IMPLTYPE addMessageProperty (@Nullable final MessageProperty a)
  {
    if (a != null)
      m_aMessageProperties.add (a);
    return thisAsT ();
  }

  @Nonnull
  public final IMPLTYPE messageProperty (@Nullable final Ebms3Property a)
  {
    return messageProperty (a == null ? null : MessageProperty.builder (a));
  }

  @Nonnull
  public final IMPLTYPE messageProperty (@Nullable final MessageProperty.Builder a)
  {
    return messageProperty (a == null ? null : a.build ());
  }

  @Nonnull
  public final IMPLTYPE messageProperty (@Nullable final MessageProperty a)
  {
    if (a == null)
      m_aMessageProperties.clear ();
    else
      m_aMessageProperties.set (a);
    return thisAsT ();
  }

  @Nonnull
  public final IMPLTYPE messageProperties (@Nullable final MessageProperty... a)
  {
    m_aMessageProperties.setAll (a);
    return thisAsT ();
  }

  @Nonnull
  public final IMPLTYPE messageProperties (@Nullable final Iterable <? extends MessageProperty> a)
  {
    m_aMessageProperties.setAll (a);
    return thisAsT ();
  }

  /**
   * Set the receiver certificate.
   *
   * @param aCertificate
   *        The certificate of the receiver to be used. May be
   *        <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE receiverCertificate (@Nullable final X509Certificate aCertificate)
  {
    m_aReceiverCertificate = aCertificate;
    return thisAsT ();
  }

  /**
   * Set an receiver AS4 endpoint URL, independent of its usability.
   *
   * @param sEndointURL
   *        The endpoint URL to be used. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE endpointURL (@Nullable final String sEndointURL)
  {
    m_sEndointURL = sEndointURL;
    return thisAsT ();
  }

  /**
   * Add an optional attachment
   *
   * @param a
   *        The attachment to be added. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE addAttachment (@Nullable final Phase4OutgoingAttachment.Builder a)
  {
    return addAttachment (a == null ? null : a.build ());
  }

  /**
   * Add an optional attachment
   *
   * @param a
   *        The attachment to be added. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE addAttachment (@Nullable final Phase4OutgoingAttachment a)
  {
    if (a != null)
      m_aAttachments.add (a);
    return thisAsT ();
  }

  /**
   * Set optional attachment. All existing attachments are overridden.
   *
   * @param a
   *        The attachment to be set. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE attachment (@Nullable final Phase4OutgoingAttachment.Builder a)
  {
    return attachment (a == null ? null : a.build ());
  }

  /**
   * Set optional attachment. All existing attachments are overridden.
   *
   * @param a
   *        The attachment to be set. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE attachment (@Nullable final Phase4OutgoingAttachment a)
  {
    if (a == null)
      m_aAttachments.clear ();
    else
      m_aAttachments.set (a);
    return thisAsT ();
  }

  /**
   * Set optional attachments. All existing attachments are overridden.
   *
   * @param a
   *        The attachment to be set. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE attachments (@Nullable final Phase4OutgoingAttachment... a)
  {
    m_aAttachments.setAll (a);
    return thisAsT ();
  }

  /**
   * Set optional attachments. All existing attachments are overridden.
   *
   * @param a
   *        The attachment to be set. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE attachments (@Nullable final Iterable <? extends Phase4OutgoingAttachment> a)
  {
    m_aAttachments.setAll (a);
    return thisAsT ();
  }

  /**
   * Set a internal message callback. Usually this method is NOT needed. Use
   * only when you know what you are doing.
   *
   * @param aBuildMessageCallback
   *        An internal to be used for the created message. May be
   *        <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE buildMessageCallback (@Nullable final IAS4ClientBuildMessageCallback aBuildMessageCallback)
  {
    m_aBuildMessageCallback = aBuildMessageCallback;
    return thisAsT ();
  }

  /**
   * Set a specific outgoing dumper for this builder.
   *
   * @param aOutgoingDumper
   *        An outgoing dumper to be used. Maybe <code>null</code>. If
   *        <code>null</code> the global outgoing dumper is used.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE outgoingDumper (@Nullable final IAS4OutgoingDumper aOutgoingDumper)
  {
    m_aOutgoingDumper = aOutgoingDumper;
    return thisAsT ();
  }

  /**
   * Set a specific incoming dumper for this builder.
   *
   * @param aIncomingDumper
   *        An incoming dumper to be used. Maybe <code>null</code>. If
   *        <code>null</code> the global incoming dumper is used.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE incomingDumper (@Nullable final IAS4IncomingDumper aIncomingDumper)
  {
    m_aIncomingDumper = aIncomingDumper;
    return thisAsT ();
  }

  /**
   * Set an optional handler that is notified if an http sending will be
   * retried. This method is optional and must not be called prior to sending.
   *
   * @param aRetryCallback
   *        The optional retry callback. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE retryCallback (@Nullable final IAS4RetryCallback aRetryCallback)
  {
    m_aRetryCallback = aRetryCallback;
    return thisAsT ();
  }

  /**
   * Set an optional handler for the synchronous result message received from
   * the other side. This method is optional and must not be called prior to
   * sending.
   *
   * @param aResponseConsumer
   *        The optional response consumer. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE rawResponseConsumer (@Nullable final IAS4RawResponseConsumer aResponseConsumer)
  {
    m_aResponseConsumer = aResponseConsumer;
    return thisAsT ();
  }

  /**
   * Set an optional Ebms3 Signal Message Consumer. If this consumer is set, the
   * response is trying to be parsed as a Signal Message. This method is
   * optional and must not be called prior to sending.
   *
   * @param aSignalMsgConsumer
   *        The optional signal message consumer. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public final IMPLTYPE signalMsgConsumer (@Nullable final IAS4SignalMessageConsumer aSignalMsgConsumer)
  {
    m_aSignalMsgConsumer = aSignalMsgConsumer;
    return thisAsT ();
  }

  @OverridingMethodsMustInvokeSuper
  public boolean isEveryRequiredFieldSet ()
  {
    if (m_aHttpClientFactory == null)
      return false;
    // m_aCryptoFactory may be null
    // m_aPModeResolver may be null
    // IIncomingAttachmentFactory may be null
    if (m_aPMode == null)
      return false;
    if (m_eSoapVersion == null)
      return false;

    // m_sServiceType may be null
    // m_sService may be null
    // m_sAction may be null
    // m_sAgreementRef may be null
    // m_sPModeID may be null

    // m_sFromPartyIDType may be null
    // m_sFromPartyID may be null
    // m_sFromRole may be null

    // m_sToPartyIDType may be null
    // m_sToPartyID may be null
    // m_sToRole may be null

    // m_sMessageID is optional
    // m_sConversationID is optional

    // m_aMessageProperties is final

    // m_aReceiverCertificate is optional
    if (StringHelper.hasNoText (m_sEndointURL))
      return false;

    // m_aBuildMessageCallback may be null
    // m_aOutgoingDumper may be null
    // m_aIncomingDumper may be null
    // m_aResponseConsumer may be null
    // m_aSignalMsgConsumer may be null

    return true;
  }

  /**
   * This method applies all builder parameters onto the user message, except
   * the attachments.
   *
   * @param aUserMsg
   *        The user message the parameters should be applied to. May not be
   *        <code>null</code>.
   */
  protected final void applyToUserMessage (@Nonnull final AS4ClientUserMessage aUserMsg)
  {
    aUserMsg.setHttpClientFactory (m_aHttpClientFactory);

    // Otherwise Oxalis dies
    aUserMsg.setQuoteHttpHeaders (false);
    aUserMsg.setSoapVersion (m_eSoapVersion);
    // Set the keystore/truststore parameters
    aUserMsg.setAS4CryptoFactory (m_aCryptoFactory);
    aUserMsg.setPMode (m_aPMode, true);

    // Set after PMode
    if (m_aReceiverCertificate != null)
      aUserMsg.cryptParams ().setCertificate (m_aReceiverCertificate);

    aUserMsg.setAgreementRefValue (m_sAgreementRef);
    if (StringHelper.hasText (m_sPModeID))
      aUserMsg.setPModeID (m_sPModeID);
    else
      aUserMsg.setPModeIDFactory (x -> null);
    aUserMsg.setServiceType (m_sServiceType);
    aUserMsg.setServiceValue (m_sService);
    aUserMsg.setAction (m_sAction);
    if (StringHelper.hasText (m_sMessageID))
      aUserMsg.setMessageID (m_sMessageID);
    aUserMsg.setConversationID (StringHelper.hasText (m_sConversationID) ? m_sConversationID
                                                                         : MessageHelperMethods.createRandomConversationID ());

    aUserMsg.setFromPartyIDType (m_sFromPartyIDType);
    aUserMsg.setFromPartyID (m_sFromPartyID);
    aUserMsg.setFromRole (m_sFromRole);

    aUserMsg.setToPartyIDType (m_sToPartyIDType);
    aUserMsg.setToPartyID (m_sToPartyID);
    aUserMsg.setToRole (m_sToRole);

    for (final MessageProperty aItem : m_aMessageProperties)
      aUserMsg.ebms3Properties ().add (aItem.getAsEbms3Property ());
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
