package com.helger.as4.messaging.mime;

import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import com.helger.commons.string.ToStringGenerator;

/**
 * Special wrapper around a {@link MimeMessage} with an indicator if the message
 * can be written more than once.
 * 
 * @author Philip Helger
 */
public class AS4MimeMessage extends MimeMessage
{
  private final boolean m_bIsRepeatable;

  public AS4MimeMessage (@Nullable final Session aSession, final boolean bIsRepeatable)
  {
    super (aSession);
    m_bIsRepeatable = bIsRepeatable;
  }

  public AS4MimeMessage (@Nullable final Session aSession, @Nonnull final InputStream aIS) throws MessagingException
  {
    super (aSession, aIS);
    m_bIsRepeatable = false;
  }

  public final boolean isRepeatable ()
  {
    return m_bIsRepeatable;
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("IsRepeatable", m_bIsRepeatable).getToString ();
  }
}
