/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
