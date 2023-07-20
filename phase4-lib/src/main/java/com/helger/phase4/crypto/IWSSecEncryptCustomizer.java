package com.helger.phase4.crypto;

import javax.annotation.Nonnull;

import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;

/**
 * Customize the {@link WSSecEncrypt} object additional to what is possible via
 * the {@link AS4CryptParams} class.
 *
 * @author Philip Helger
 * @since 2.1.4
 */
public interface IWSSecEncryptCustomizer
{
  /**
   * Create an overloaded version of WSSecEncrypt
   *
   * @param aSecHeader
   *        The security header to start with.
   * @return Never <code>null</code>.
   */
  @Nonnull
  default WSSecEncrypt createWSSecEncrypt (@Nonnull final WSSecHeader aSecHeader)
  {
    return new WSSecEncrypt (aSecHeader);
  }

  /**
   * The customization happens AFTER all the default properties are applied. So
   * be sure you know what to do when overwriting stuff.
   *
   * @param aWSSecEncrypt
   *        The object to modify. May not be <code>null</code>.
   */
  default void customize (@Nonnull final WSSecEncrypt aWSSecEncrypt)
  {}
}
