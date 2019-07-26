package com.helger.as4.client;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeMessage;

import org.w3c.dom.Document;

import com.helger.as4.messaging.domain.AbstractAS4Message;

/**
 * Callback interface for AS4 client message creation.
 *
 * @author Philip Helger
 */
public interface IAS4ClientBuildMessageCallback extends Serializable
{
  /**
   * @param aMsg
   *        The created message
   */
  default void onAS4Message (@Nonnull final AbstractAS4Message <?> aMsg)
  {}

  /**
   * @param aDoc
   *        The created SOAP document
   */
  default void onSOAPDocument (@Nonnull final Document aDoc)
  {}

  /**
   * @param aDoc
   *        The signed SOAP document
   */
  default void onSignedSOAPDocument (@Nonnull final Document aDoc)
  {}

  /**
   * This method is only called if encryption is enabled and no attachments are
   * present. Only called for user messages.
   *
   * @param aDoc
   *        The encrypted SOAP document
   */
  default void onEncryptedSOAPDocument (@Nonnull final Document aDoc)
  {}

  /**
   * This method is only called if encryption is enabled and at least one
   * attachments is present. Only called for user messages.
   *
   * @param aMimeMsg
   *        The encrypted MIME message
   */
  default void onEncryptedMimeMessage (@Nonnull final MimeMessage aMimeMsg)
  {}
}
