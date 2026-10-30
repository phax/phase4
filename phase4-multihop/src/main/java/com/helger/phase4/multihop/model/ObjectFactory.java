/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.multihop.model;

import javax.xml.namespace.QName;

import org.jspecify.annotations.NonNull;

import com.helger.phase4.multihop.CAS4MultiHop;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

/**
 * JAXB object factory for the multi-hop model classes. This is needed so that a JAXBContext can be
 * created for this package.<br>
 * The classes are hand written on purpose - generating them from the OASIS schema would create a
 * duplicate set of all <code>Ebms3*</code> classes, because the refactored ebMS3 core schema is
 * imported there.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@XmlRegistry
public class ObjectFactory
{
  /** The QName of the RoutingInput root element */
  public static final QName _RoutingInput_QNAME = new QName (CAS4MultiHop.EBINT_NS, "RoutingInput");

  public ObjectFactory ()
  {}

  @NonNull
  public MultiHopRoutingInput createMultiHopRoutingInput ()
  {
    return new MultiHopRoutingInput ();
  }

  @NonNull
  public MultiHopRoutingUserMessage createMultiHopRoutingUserMessage ()
  {
    return new MultiHopRoutingUserMessage ();
  }

  @NonNull
  @XmlElementDecl (namespace = CAS4MultiHop.EBINT_NS, name = "RoutingInput")
  public JAXBElement <MultiHopRoutingInput> createRoutingInput (@NonNull final MultiHopRoutingInput aValue)
  {
    return new JAXBElement <> (_RoutingInput_QNAME, MultiHopRoutingInput.class, null, aValue);
  }
}
