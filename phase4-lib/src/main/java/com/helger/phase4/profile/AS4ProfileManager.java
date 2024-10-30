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
package com.helger.phase4.profile;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsTreeMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
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
@ThreadSafe
public class AS4ProfileManager implements IAS4ProfileManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4ProfileManager.class);

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private final ICommonsMap <String, IAS4Profile> m_aProfiles = new CommonsTreeMap <> ();

  private void _registerAll ()
  {
    m_aRWLock.writeLocked ( () -> { m_aProfiles.clear (); });
    for (final IAS4ProfileRegistrarSPI aSPI : ServiceLoaderHelper.getAllSPIImplementations (IAS4ProfileRegistrarSPI.class))
      aSPI.registerAS4Profile (this);

    final int nCount = getProfileCount ();
    if (nCount == 0)
      LOGGER.warn ("No AS4 profile is registered. This is most likely a configuration problem. Please make sure that at least one of the 'phase4-profile-*' modules is on the classpath.");
    else
      LOGGER.info ((nCount == 1 ? "1 AS4 profile is registered" : nCount + " AS4 profiles are registered") +
                   ": " +
                   m_aProfiles.keySet ());
  }

  public AS4ProfileManager ()
  {
    _registerAll ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IAS4Profile> getAllProfiles ()
  {
    return m_aRWLock.readLockedGet (m_aProfiles::copyOfValues);
  }

  @Nonnegative
  public final int getProfileCount ()
  {
    return m_aRWLock.readLockedInt (m_aProfiles::size);
  }

  @Nullable
  public IAS4Profile getProfileOfID (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return null;

    return m_aRWLock.readLockedGet ( () -> m_aProfiles.get (sID));
  }

  public void registerProfile (@Nonnull final IAS4Profile aAS4Profile)
  {
    ValueEnforcer.notNull (aAS4Profile, "AS4Profile");

    final String sID = aAS4Profile.getID ();
    m_aRWLock.writeLocked ( () -> {
      if (m_aProfiles.containsKey (sID))
        throw new IllegalStateException ("An AS4 profile with ID '" + sID + "' is already registered!");
      m_aProfiles.put (sID, aAS4Profile);
    });

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Registered" + (aAS4Profile.isDeprecated () ? " deprecated" : "") + " AS4 profile '" + sID + "'");
  }

  public void reloadAll ()
  {
    _registerAll ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Profiles", m_aProfiles).getToString ();
  }
}
