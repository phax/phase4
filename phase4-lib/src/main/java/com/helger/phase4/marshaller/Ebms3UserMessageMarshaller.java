/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.marshaller;

import javax.xml.namespace.QName;

import com.helger.jaxb.GenericJAXBMarshaller;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.ebms3header.ObjectFactory;

/**
 * Marshaller for {@link Ebms3UserMessage} objects. This should only simplify
 * the logging of user messages but it is in itself not a valid Ebms3 message.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public class Ebms3UserMessageMarshaller extends GenericJAXBMarshaller <Ebms3UserMessage>
{
  public static final QName ROOT_ELEMENT_QNAME = new QName (ObjectFactory._Messaging_QNAME.getNamespaceURI (),
                                                            "UserMessage");

  public Ebms3UserMessageMarshaller ()
  {
    // No XSD, because the "UserMessage" is not a dedicated element
    super (Ebms3UserMessage.class, null, createSimpleJAXBElement (ROOT_ELEMENT_QNAME, Ebms3UserMessage.class));
    setNamespaceContext (Ebms3NamespaceHandler.getInstance ());
  }
}
