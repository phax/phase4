package com.helger.as4lib.attachment;

import java.io.File;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.AttachmentUtils;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.ext.CommonsHashMap;
import com.helger.commons.collection.ext.ICommonsMap;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import com.helger.http.CHTTPHeader;

/**
 * File based attachment.
 *
 * @author Philip Helger
 */
public class AS4FileAttachment extends AbstractAS4Attachment
{
  private final File m_aFile;
  private Crypto m_aCrypto = null;
  private Attachment m_aEncryptedAttachment = null;

  public AS4FileAttachment (@Nonnull final File aFile, @Nonnull final IMimeType aMimeType) throws WSSecurityException
  {
    super (aMimeType, (EAS4CompressionMode) null);
    ValueEnforcer.notNull (aFile, "File");
    m_aFile = aFile;
    m_aCrypto = CryptoFactory.getInstance ("test.properties");
  }

  public void addToMimeMultipart (@Nonnull final MimeMultipart aMimeMultipart,
                                  final boolean bEncrypt) throws MessagingException
  {
    ValueEnforcer.notNull (aMimeMultipart, "MimeMultipart");

    final MimeBodyPart aMimeBodyPart = new MimeBodyPart ();
    aMimeBodyPart.setDataHandler (new DataHandler (new FileDataSource (m_aFile)));

    aMimeBodyPart.setHeader (CHTTPHeader.CONTENT_TRANSFER_ENCODING, getContentTransferEncoding ().getID ());
    aMimeBodyPart.setHeader (AttachmentUtils.MIME_HEADER_CONTENT_ID, getID ());

    if (bEncrypt)
    {
      aMimeBodyPart.removeHeader (AttachmentUtils.MIME_HEADER_CONTENT_TYPE);
      aMimeBodyPart.setHeader (AttachmentUtils.MIME_HEADER_CONTENT_TYPE,
                               CMimeType.APPLICATION_OCTET_STREAM.getAsString ());
      aMimeMultipart.addBodyPart (aMimeBodyPart);
    }

    // {
    // final CryptoType aCryptoType = new CryptoType (CryptoType.TYPE.ALIAS);
    // aCryptoType.setAlias (CryptoConfigBuilder.CF.getAsString
    // ("encrypt.alias"));
    // X509Certificate [] aCertList;
    //
    // aCertList = m_aCrypto.getX509Certificates (aCryptoType);

    else
    {
      aMimeBodyPart.setHeader (AttachmentUtils.MIME_HEADER_CONTENT_TYPE, getMimeType ().getAsString ());
      aMimeMultipart.addBodyPart (aMimeBodyPart);
    }
  }

  @Nonnull
  public Attachment getAsWSS4JAttachment ()
  {
    if (m_aEncryptedAttachment != null)
    {
      return m_aEncryptedAttachment;
    }
    else
    {
      final ICommonsMap <String, String> aHeaders = new CommonsHashMap<> ();
      aHeaders.put (AttachmentUtils.MIME_HEADER_CONTENT_DESCRIPTION, "Attachment");
      aHeaders.put (AttachmentUtils.MIME_HEADER_CONTENT_DISPOSITION,
                    "attachment; filename=\"" + FilenameHelper.getWithoutPath (m_aFile) + "\"");
      aHeaders.put (AttachmentUtils.MIME_HEADER_CONTENT_ID, "<attachment=" + getID () + ">");
      aHeaders.put (AttachmentUtils.MIME_HEADER_CONTENT_TYPE, getMimeType ().getAsString ());

      final Attachment aAttachment = new Attachment ();
      aAttachment.setMimeType (getMimeType ().getAsString ());
      aAttachment.addHeaders (aHeaders);
      aAttachment.setId (getID ());
      aAttachment.setSourceStream (FileHelper.getInputStream (m_aFile));
      return aAttachment;
    }
  }

  public void setEncryptedAttachment (final Attachment aEncryptedAttachment)
  {
    m_aEncryptedAttachment = aEncryptedAttachment;
  }
}
