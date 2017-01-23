/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.profile;

import java.io.Serializable;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsHashMap;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsMap;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.lang.ServiceLoaderHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * AS4 profile manager. All profiles are registered by SPI -
 * {@link IAS4ProfileRegistrarSPI}.
 *
 * @author Philip Helger
 */
public class AS4ProfileManager implements IAS4ProfileRegistrar, Serializable
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS4ProfileManager.class);

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  private final ICommonsMap <String, IAS4Profile> m_aMap = new CommonsHashMap <> ();
  private IAS4Profile m_aDefaultProfile;

  private void _registerAll ()
  {
    m_aRWLock.writeLocked ( () -> m_aMap.clear ());
    for (final IAS4ProfileRegistrarSPI aSPI : ServiceLoaderHelper.getAllSPIImplementations (IAS4ProfileRegistrarSPI.class))
      aSPI.registerAS4Profile (this);

    final int nCount = getProfileCount ();
    s_aLogger.info ((nCount == 1 ? "1 AS4 profile is registered " : nCount + " AS4 profiles are registered"));
  }

  public AS4ProfileManager ()
  {
    _registerAll ();
  }

  public void registerProfile (@Nonnull final IAS4Profile aAS4Profile)
  {
    ValueEnforcer.notNull (aAS4Profile, "AS4Profile");

    final String sID = aAS4Profile.getID ();
    m_aRWLock.writeLocked ( () -> {
      if (m_aMap.containsKey (sID))
        throw new IllegalStateException ("An AS4 profile with ID '" + sID + "' is already registered!");
      m_aMap.put (sID, aAS4Profile);
    });
    s_aLogger.info ("Registered AS4 profile '" + sID + "'");
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IAS4Profile> getAllProfiles ()
  {
    return m_aRWLock.readLocked ( () -> m_aMap.copyOfValues ());
  }

  @Nonnegative
  public final int getProfileCount ()
  {
    return m_aRWLock.readLocked ( () -> m_aMap.size ());
  }

  @Nullable
  public IAS4Profile getProfileOfID (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return null;

    return m_aRWLock.readLocked ( () -> m_aMap.get (sID));
  }

  /**
   * Set the default profile to be used.
   *
   * @param sDefaultProfileID
   *        The ID of the default profile. May be <code>null</code>.
   * @return <code>null</code> if no such profile is registered, the resolve
   *         profile otherwise.
   */
  @Nullable
  public IAS4Profile setDefaultProfile (@Nullable final String sDefaultProfileID)
  {
    final IAS4Profile aDefault = getProfileOfID (sDefaultProfileID);
    m_aRWLock.writeLocked ( () -> m_aDefaultProfile = aDefault);
    return aDefault;
  }

  /**
   * @return The default profile. If none is set, and exactly one profile is
   *         present, it is used. If no default profile is present and more than
   *         one profile is present an Exception is thrown.
   * @throws IllegalStateException
   *         If no default is present and more than one profile is registered
   */
  @Nonnull
  public IAS4Profile getDefaultProfile ()
  {
    return m_aRWLock.readLocked ( () -> {
      IAS4Profile ret = m_aDefaultProfile;
      if (ret == null)
      {
        if (m_aMap.size () == 1)
          ret = m_aMap.getFirstValue ();
        else
          if (m_aMap.isEmpty ())
            throw new IllegalStateException ("No AS4 profile is present, so no default profile can be determined!");
          else
            throw new IllegalStateException (m_aMap.size () +
                                             " AS4 profiles are present, but none is declared default!");
      }
      return ret;
    });
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Map", m_aMap).append ("DefaultProfile", m_aDefaultProfile).toString ();
  }
}
