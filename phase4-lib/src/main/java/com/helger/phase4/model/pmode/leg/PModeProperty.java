/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.model.pmode.leg;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.name.IHasName;
import com.helger.commons.state.EMandatory;
import com.helger.commons.state.IMandatoryIndicator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.text.IHasDescription;

/**
 * A property is a data structure that consists of four values: the property
 * name, which can be used as an identifier of the property (e.g. a required
 * property named "messagetype" can be noted as:
 * <code>Properties[messagetype].required="true"</code>); the property
 * description; the property data type; and a Boolean value, indicating whether
 * the property is expected or optional, within the User message. This parameter
 * controls the contents of the element
 * <code>eb:Messaging/eb:UserMessage/eb:MessageProperties.</code>
 *
 * @author Philip Helger
 */
@Immutable
@MustImplementEqualsAndHashcode
public class PModeProperty implements IHasName, IHasDescription, IMandatoryIndicator, Serializable
{
  public static final String DATA_TYPE_STRING = "string";
  public static final boolean DEFAULT_MANDATORY = false;

  private static final Logger LOGGER = LoggerFactory.getLogger (PModeProperty.class);

  private final String m_sName;
  private final String m_sDescription;
  private final String m_sDataType;
  private final EMandatory m_eMandatory;

  private static void _checkDataType (@Nonnull final String sDataType)
  {
    if (!DATA_TYPE_STRING.equals (sDataType))
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn ("A non-standard data type (everything besides 'string') is used: " + sDataType);
  }

  public PModeProperty (@Nonnull @Nonempty final String sName,
                        @Nullable final String sDescription,
                        @Nonnull @Nonempty final String sDataType,
                        @Nonnull final EMandatory eMandatory)
  {
    m_sName = ValueEnforcer.notEmpty (sName, "Name");
    m_sDescription = sDescription;
    m_sDataType = ValueEnforcer.notEmpty (sDataType, "DataType");
    m_eMandatory = ValueEnforcer.notNull (eMandatory, "Mandatory");
    _checkDataType (sDataType);
  }

  /**
   * The PMode property name.
   */
  @Nonnull
  @Nonempty
  public final String getName ()
  {
    return m_sName;
  }

  /**
   * The PMode property description.
   */
  @Nullable
  public final String getDescription ()
  {
    return m_sDescription;
  }

  /**
   * @return The PMode property data type. May neither be <code>null</code> nor
   *         empty.
   */
  @Nonnull
  @Nonempty
  public final String getDataType ()
  {
    return m_sDataType;
  }

  /**
   * @return <code>true</code> if the PMode property is mandatory,
   *         <code>false</code> if it is optional.
   */
  public final boolean isMandatory ()
  {
    return m_eMandatory.isMandatory ();
  }

  /**
   * @return <code>true</code> if the PMode property is optional,
   *         <code>false</code> if it is mandatory.
   */
  @Override
  public final boolean isOptional ()
  {
    return m_eMandatory.isOptional ();
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PModeProperty rhs = (PModeProperty) o;
    return m_sName.equals (rhs.m_sName) &&
           EqualsHelper.equals (m_sDescription, rhs.m_sDescription) &&
           m_sDataType.equals (rhs.m_sDataType) &&
           m_eMandatory.equals (rhs.m_eMandatory);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sName)
                                       .append (m_sDescription)
                                       .append (m_sDataType)
                                       .append (m_eMandatory)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Name", m_sName)
                                       .append ("Description", m_sDescription)
                                       .append ("DataType", m_sDataType)
                                       .append ("Mandatory", m_eMandatory)
                                       .getToString ();
  }
}
