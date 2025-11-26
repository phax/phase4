/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.error;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.ebms3header.Ebms3Error;

/**
 * Specific list of {@link AS4Error} objects
 *
 * @author Philip Helger
 * @since 4.2.0
 */
public class AS4ErrorList implements Iterable <AS4Error>
{
  private final ICommonsList <AS4Error> m_aList = new CommonsArrayList <> ();

  public AS4ErrorList ()
  {}

  /**
   * Add an EBMS Error to the list. It is internally wrapped into an {@link AS4Error}. No special
   * HTTP Status code can be provided with this method.
   *
   * @param aError
   *        The error to be added. May not be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public AS4ErrorList add (@NonNull final Ebms3Error aError)
  {
    ValueEnforcer.notNull (aError, "Error");
    return add (AS4Error.builder ().ebmsError (aError).build ());
  }

  /**
   * Add an error to the list.
   *
   * @param aError
   *        The error to be added. May not be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public AS4ErrorList add (@NonNull final AS4Error aError)
  {
    ValueEnforcer.notNull (aError, "Error");
    m_aList.add (aError);
    return this;
  }

  /**
   * Add another error list to this list
   *
   * @param aOther
   *        the error list to be added. May not be <code>null</code>.
   * @return this for chaining
   */
  @NonNull
  public AS4ErrorList addAll (@NonNull final AS4ErrorList aOther)
  {
    ValueEnforcer.notNull (aOther, "Other");
    m_aList.addAll (aOther.m_aList);
    return this;
  }

  /**
   * Get a mapped list of all contained errors.
   *
   * @param <T>
   *        The destination type.
   * @param aFunc
   *        The function to be invoked for each error in the list. May not be <code>null</code>.
   * @return A non-<code>null</code> but maybe empty list of mapped elements.
   */
  @NonNull
  @ReturnsMutableCopy
  public <T> ICommonsList <T> getAllMapped (@NonNull final Function <? super AS4Error, T> aFunc)
  {
    ValueEnforcer.notNull (aFunc, "Function");
    return m_aList.getAllMapped (aFunc);
  }

  /**
   * Iterate over each contained error with the provided consumer.
   */
  public void forEach (@NonNull final Consumer <? super AS4Error> aConsumer)
  {
    ValueEnforcer.notNull (aConsumer, "Consumer");
    m_aList.forEach (aConsumer);
  }

  /**
   * @return <code>true</code> if the list is empty, <code>false</code> if not.
   * @see #isNotEmpty()
   */
  public boolean isEmpty ()
  {
    return m_aList.isEmpty ();
  }

  /**
   * @return <code>true</code> if the list is not empty, <code>false</code> if it is.
   * @see #isEmpty()
   */
  public boolean isNotEmpty ()
  {
    return m_aList.isNotEmpty ();
  }

  /**
   * @return The number of errors contained. Must be &ge; 0.
   */
  @Nonnegative
  public int size ()
  {
    return m_aList.size ();
  }

  /**
   * Get an iterator over all contained errors.
   */
  @NonNull
  public Iterator <AS4Error> iterator ()
  {
    return m_aList.iterator ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("List", m_aList).getToString ();
  }
}
