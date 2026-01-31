/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.model.message;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.time.OffsetDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;

import javax.xml.namespace.QName;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.base.trait.IGenericImplTrait;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.phase4.CAS4;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.ebms3header.Ebms3Messaging;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.marshaller.Ebms3MessagingMarshaller;
import com.helger.phase4.marshaller.Soap11EnvelopeMarshaller;
import com.helger.phase4.marshaller.Soap12EnvelopeMarshaller;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.soap11.Soap11Body;
import com.helger.phase4.soap11.Soap11Envelope;
import com.helger.phase4.soap11.Soap11Header;
import com.helger.phase4.soap12.Soap12Body;
import com.helger.phase4.soap12.Soap12Envelope;
import com.helger.phase4.soap12.Soap12Header;
import com.helger.xml.XMLHelper;

/**
 * Abstract AS4 message implementation
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        Real implementation type.
 */
public abstract class AbstractAS4Message <IMPLTYPE extends AbstractAS4Message <IMPLTYPE>> implements
                                         IAS4Message,
                                         IGenericImplTrait <IMPLTYPE>
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (AbstractAS4Message.class);
  private static final QName QNAME_WSU_ID = new QName (CAS4.WSU_NS, "Id");

  private final ESoapVersion m_eSoapVersion;
  private final EAS4MessageType m_eMsgType;
  private final String m_sMessagingID;
  protected final Ebms3Messaging m_aMessaging = new Ebms3Messaging ();

  public AbstractAS4Message (@NonNull final ESoapVersion eSoapVersion, @NonNull final EAS4MessageType eMsgType)
  {
    m_eSoapVersion = ValueEnforcer.notNull (eSoapVersion, "SoapVersion");
    m_eMsgType = ValueEnforcer.notNull (eMsgType, "MessageType");
    m_sMessagingID = MessageHelperMethods.createRandomMessagingID ();

    // Must be a "wsu:Id" for WSSec to be found
    m_aMessaging.getOtherAttributes ().put (QNAME_WSU_ID, m_sMessagingID);

    // Assume all EbmsMessaging parts are always "mustUnderstand"
    setMustUnderstand (true);
  }

  @NonNull
  public final ESoapVersion getSoapVersion ()
  {
    return m_eSoapVersion;
  }

  @NonNull
  public final EAS4MessageType getMessageType ()
  {
    return m_eMsgType;
  }

  @NonNull
  @Nonempty
  public final String getMessagingID ()
  {
    return m_sMessagingID;
  }

  @NonNull
  public final IMPLTYPE setMustUnderstand (final boolean bMustUnderstand)
  {
    switch (m_eSoapVersion)
    {
      case SOAP_11:
        m_aMessaging.setS11MustUnderstand (Boolean.valueOf (bMustUnderstand));
        break;
      case SOAP_12:
        m_aMessaging.setS12MustUnderstand (Boolean.valueOf (bMustUnderstand));
        break;
      default:
        throw new IllegalStateException ("Unsupported SOAP version " + m_eSoapVersion);
    }
    return thisAsT ();
  }

  public static final DateTimeFormatter DOMIBUS_XSD_DATE_TIME;

  static
  {
    // Same as PDTWebDateHelper.XSD_TIME except the milliseconds
    final DateTimeFormatter aTimeFormatter = new DateTimeFormatterBuilder ().parseCaseInsensitive ()
                                                                            .appendValue (HOUR_OF_DAY, 2)
                                                                            .appendLiteral (':')
                                                                            .appendValue (MINUTE_OF_HOUR, 2)
                                                                            .optionalStart ()
                                                                            .appendLiteral (':')
                                                                            .appendValue (SECOND_OF_MINUTE, 2)
                                                                            .optionalStart ()
                                                                            /*
                                                                             * This is different
                                                                             * compared to
                                                                             * PDTWebDateHelper. We
                                                                             * use exactly 3 here.
                                                                             */
                                                                            .appendFraction (MILLI_OF_SECOND,
                                                                                             3,
                                                                                             3,
                                                                                             true)
                                                                            .optionalEnd ()
                                                                            /*
                                                                             * Timezone can occur
                                                                             * without milliseconds
                                                                             */
                                                                            .optionalStart ()
                                                                            .appendOffsetId ()
                                                                            .optionalStart ()
                                                                            .appendLiteral ('[')
                                                                            .parseCaseSensitive ()
                                                                            .appendZoneRegionId ()
                                                                            .appendLiteral (']')
                                                                            .toFormatter (Locale.getDefault (Locale.Category.FORMAT))
                                                                            .withResolverStyle (ResolverStyle.STRICT)
                                                                            .withChronology (IsoChronology.INSTANCE);
    DOMIBUS_XSD_DATE_TIME = new DateTimeFormatterBuilder ().parseCaseInsensitive ()
                                                           .append (DateTimeFormatter.ISO_LOCAL_DATE)
                                                           .appendLiteral ('T')
                                                           .append (aTimeFormatter)
                                                           .toFormatter (Locale.getDefault (Locale.Category.FORMAT))
                                                           .withResolverStyle (ResolverStyle.STRICT)
                                                           .withChronology (IsoChronology.INSTANCE);
  }

  @NonNull
  public final Document getAsSoapDocument (@Nullable final Node aSoapBodyPayload)
  {
    // Convert to DOM Node
    final Element aEbms3Element = new Ebms3MessagingMarshaller ().getAsElement (m_aMessaging);
    if (aEbms3Element == null)
      throw new IllegalStateException ("Failed to write EBMS3 Messaging to XML");

    if (AS4Configuration.isCompatibilityModeDomibus ())
    {
      // Do some timestamp post processing. See #335
      Element aEbms3AnyMessage = XMLHelper.getChildElementIteratorNS (aEbms3Element, CAS4.EBMS_NS).next ();
      if (aEbms3AnyMessage != null)
      {
        Element aEbms3MessageInfo = XMLHelper.getFirstChildElementOfName (aEbms3AnyMessage,
                                                                          CAS4.EBMS_NS,
                                                                          "MessageInfo");
        if (aEbms3MessageInfo != null)
        {
          Element aEbms3Timestamp = XMLHelper.getFirstChildElementOfName (aEbms3MessageInfo, CAS4.EBMS_NS, "Timestamp");
          if (aEbms3Timestamp != null)
          {
            final String sValue = XMLHelper.getFirstChildText (aEbms3Timestamp);
            final OffsetDateTime aODT = PDTWebDateHelper.getOffsetDateTimeFromXSD (sValue);
            if ((aODT.get (ChronoField.MILLI_OF_SECOND) % 10) == 0)
            {
              String sNewValue = DOMIBUS_XSD_DATE_TIME.format (aODT);
              LOGGER.info ("Changing MessageInfo/Timestamp from '" + sValue + "' to '" + sNewValue + "' for Domibus");

              // Replace in DOM
              XMLHelper.removeAllChildElements (aEbms3Timestamp);
              aEbms3Timestamp.appendChild (aEbms3Timestamp.getOwnerDocument ().createTextNode (sNewValue));
            }
          }
        }
      }
    }

    final Node aRealSoapBodyPayload = aSoapBodyPayload instanceof Document d ? d.getDocumentElement ()
                                                                             : aSoapBodyPayload;

    switch (m_eSoapVersion)
    {
      case SOAP_11:
      {
        // Creating SOAP 11 Envelope
        final Soap11Envelope aSoapEnv = new Soap11Envelope ();

        aSoapEnv.setHeader (new Soap11Header ());
        aSoapEnv.getHeader ().addAny (aEbms3Element);

        aSoapEnv.setBody (new Soap11Body ());
        if (aRealSoapBodyPayload != null)
          aSoapEnv.getBody ().addAny (aRealSoapBodyPayload);

        final Document ret = new Soap11EnvelopeMarshaller ().getAsDocument (aSoapEnv);
        if (ret == null)
          throw new IllegalStateException ("Failed to serialize SOAP 1.1 document");
        return ret;
      }
      case SOAP_12:
      {
        // Creating SOAP 12 Envelope
        final Soap12Envelope aSoapEnv = new Soap12Envelope ();

        aSoapEnv.setHeader (new Soap12Header ());
        aSoapEnv.getHeader ().addAny (aEbms3Element);

        aSoapEnv.setBody (new Soap12Body ());
        if (aRealSoapBodyPayload != null)
          aSoapEnv.getBody ().addAny (aRealSoapBodyPayload);

        final Document ret = new Soap12EnvelopeMarshaller ().getAsDocument (aSoapEnv);
        if (ret == null)
          throw new IllegalStateException ("Failed to serialize SOAP 1.2 document");
        return ret;
      }
      default:
        throw new IllegalStateException ("Unsupported SOAP version " + m_eSoapVersion);
    }
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("SOAPVersion", m_eSoapVersion)
                                       .append ("MsgType", m_eMsgType)
                                       .append ("MessagingID", m_sMessagingID)
                                       .append ("Messaging", m_aMessaging)
                                       .getToString ();
  }
}
