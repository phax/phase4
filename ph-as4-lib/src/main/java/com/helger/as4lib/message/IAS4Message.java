package com.helger.as4lib.message;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4lib.soap.ESOAPVersion;

/**
 * Base interface for an AS4 message.
 *
 * @author Philip Helger
 */
public interface IAS4Message extends Serializable
{
  /**
   * @return The SOAP version to use. May not be <code>null</code>.
   */
  @Nonnull
  ESOAPVersion getSOAPVersion ();

  /**
   * Set the "mustUnderstand" value depending on the used SOAP version.
   *
   * @param bMustUnderstand
   *        <code>true</code> for must understand, <code>false</code> otherwise.
   * @return this for chaining
   */
  @Nonnull
  IAS4Message setMustUnderstand (boolean bMustUnderstand);

  @Nonnull
  default Document getAsSOAPDocument ()
  {
    return getAsSOAPDocument ((Node) null);
  }

  @Nonnull
  Document getAsSOAPDocument (@Nullable Node aPayload);
}
