/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.partner;

import java.util.Iterator;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import com.helger.as4lib.util.IStringMap;
import com.helger.as4lib.util.StringMap;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsMap;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.photon.basic.object.AbstractBaseObject;
import com.helger.photon.security.object.StubObject;

/**
 * This class represents a single partner. A partnership consists of 2 partners
 * - a sender and a receiver.
 *
 * @author Philip Helger
 * @since 2.2.0
 */
public class Partner extends AbstractBaseObject implements IPartner
{
  public static final ObjectType OT = new ObjectType ("as4.partner");
  public static final String ATTR_PARTNER_NAME = "name";
  public static final String ATTR_CERT = "certificate";

  private final StringMap m_aAttrs;

  public Partner (@Nonnull @Nonempty final String sID, @Nonnull final IStringMap aAttrs)
  {
    this (StubObject.createForCurrentUserAndID (sID), aAttrs);
  }

  Partner (@Nonnull final StubObject aStubObject, @Nonnull final IStringMap aAttrs)
  {
    super (aStubObject);
    m_aAttrs = new StringMap (aAttrs);
    if (!m_aAttrs.containsAttribute (ATTR_PARTNER_NAME))
      throw new IllegalArgumentException ("The provided attributes are missing the required '" +
                                          ATTR_PARTNER_NAME +
                                          "' attribute!");
  }

  @Nonnull
  public String getName ()
  {
    return m_aAttrs.getAttributeAsString (ATTR_PARTNER_NAME);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsMap <String, String> getAllAttributes ()
  {
    return m_aAttrs.getAllAttributes ();
  }

  public void setAllAttributes (final IStringMap aAttrs)
  {
    m_aAttrs.clear ();
    m_aAttrs.setAttributes (aAttrs);
  }

  @Nonnull
  public Iterator <Entry <String, String>> iterator ()
  {
    return m_aAttrs.iterator ();
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final Partner rhs = (Partner) o;
    return m_aAttrs.equals (rhs.m_aAttrs);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aAttrs).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Attrs", m_aAttrs).toString ();
  }

  public ObjectType getObjectType ()
  {
    return OT;
  }

}
