package com.helger.as4.messaging.mime;

import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import com.helger.commons.string.ToStringGenerator;

public class AS4MimeMessage extends MimeMessage
{
  private boolean m_bIsRepeatable = false;

  public AS4MimeMessage (@Nullable final Session aSession)
  {
    super (aSession);
  }

  public AS4MimeMessage (@Nullable final Session aSession, @Nonnull final InputStream aIS) throws MessagingException
  {
    super (aSession, aIS);
  }

  public boolean isRepeatable ()
  {
    return m_bIsRepeatable;
  }

  @Nonnull
  public AS4MimeMessage setRepeatable (final boolean bIsRepeatable)
  {
    m_bIsRepeatable = bIsRepeatable;
    return this;
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("IsRepeatable", m_bIsRepeatable).getToString ();
  }
}
