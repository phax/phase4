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
package com.helger.phase4.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.builder.IBuilder;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.ebms3header.Ebms3Property;

/**
 * Represents a single message property.
 *
 * @author Philip Helger
 * @since 0.9.18
 */
public class MessageProperty
{
  private final String m_sName;
  private final String m_sType;
  private final String m_sValue;

  public MessageProperty (@Nonnull @Nonempty final String sName,
                          @Nullable final String sType,
                          @Nonnull @Nonempty final String sValue)
  {
    ValueEnforcer.notEmpty (sName, "Name");
    ValueEnforcer.notEmpty (sValue, "Value");
    m_sName = sName;
    m_sType = sType;
    m_sValue = sValue;
  }

  @Nonnull
  @Nonempty
  public final String getName ()
  {
    return m_sName;
  }

  @Nullable
  public final String getType ()
  {
    return m_sType;
  }

  @Nonnull
  @Nonempty
  public final String getValue ()
  {
    return m_sValue;
  }

  @Nonnull
  @ReturnsMutableCopy
  public Ebms3Property getAsEbms3Property ()
  {
    final Ebms3Property ret = new Ebms3Property ();
    ret.setName (m_sName);
    ret.setType (m_sType);
    ret.setValue (m_sValue);
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final MessageProperty rhs = (MessageProperty) o;
    return EqualsHelper.equals (m_sName, rhs.m_sName) &&
           EqualsHelper.equals (m_sType, rhs.m_sType) &&
           EqualsHelper.equals (m_sValue, rhs.m_sValue);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sName).append (m_sType).append (m_sValue).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("m_sName", m_sName)
                                       .append ("m_sType", m_sType)
                                       .append ("m_sValue", m_sValue)
                                       .getToString ();
  }

  @Nonnull
  public static Builder builder ()
  {
    return new Builder ();
  }

  @Nonnull
  public static Builder builder (@Nullable final Ebms3Property a)
  {
    final Builder ret = new Builder ();
    if (a != null)
      ret.name (a.getName ()).type (a.getType ()).value (a.getValue ());
    return ret;
  }

  public static class Builder implements IBuilder <MessageProperty>
  {
    private String m_sName;
    private String m_sType;
    private String m_sValue;

    protected Builder ()
    {}

    @Nonnull
    public Builder name (@Nullable final String s)
    {
      m_sName = s;
      return this;
    }

    @Nonnull
    public Builder type (@Nullable final String s)
    {
      m_sType = s;
      return this;
    }

    @Nonnull
    public Builder value (@Nullable final String s)
    {
      m_sValue = s;
      return this;
    }

    @OverridingMethodsMustInvokeSuper
    public void checkConsistency ()
    {
      if (StringHelper.hasNoText (m_sName))
        throw new IllegalStateException ("Name MUST be present");
      if (StringHelper.hasNoText (m_sValue))
        throw new IllegalStateException ("Value MUST be present");
    }

    @Nonnull
    public MessageProperty build ()
    {
      checkConsistency ();
      return new MessageProperty (m_sName, m_sType, m_sValue);
    }
  }
}
