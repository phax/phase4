package com.helger.as4lib.messaging;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.as4lib.ebms3header.Ebms3Messaging;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.traits.IGenericImplTrait;

public abstract class AbstractAS4Message <IMPLTYPE extends AbstractAS4Message <IMPLTYPE>>
                                         implements IAS4Message, IGenericImplTrait <IMPLTYPE>
{
  private final ESOAPVersion m_eSOAPVersion;
  protected final Ebms3Messaging m_aMessaging = new Ebms3Messaging ();

  public AbstractAS4Message (@Nonnull final ESOAPVersion eSOAPVersion)
  {
    m_eSOAPVersion = ValueEnforcer.notNull (eSOAPVersion, "SOAPVersion");
  }

  @Nonnull
  public final ESOAPVersion getSOAPVersion ()
  {
    return m_eSOAPVersion;
  }

  @Nonnull
  public final IMPLTYPE setMustUnderstand (final boolean bMustUnderstand)
  {
    switch (m_eSOAPVersion)
    {
      case SOAP_11:
        m_aMessaging.setS11MustUnderstand (Boolean.valueOf (bMustUnderstand));
        break;
      case SOAP_12:
        m_aMessaging.setS12MustUnderstand (Boolean.valueOf (bMustUnderstand));
        break;
      default:
        throw new IllegalStateException ("Unsupported SOAP version");
    }
    return thisAsT ();
  }

  @Nonnull
  public final Document getAsSOAPDocument (@Nullable final Element aPayload)
  {
    return MessagingHandler.createSOAPEnvelopeAsDocument (m_eSOAPVersion, m_aMessaging, aPayload);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("SOAPVersion", m_eSOAPVersion).toString ();
  }
}
