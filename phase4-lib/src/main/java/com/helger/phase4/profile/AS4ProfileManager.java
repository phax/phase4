/*
 * Copyright (C) 2015-2022 Philip Helger (www.helger.com)
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

import java.io.Serializable;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.lang.ServiceLoaderHelper;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * AS4 profile manager. All profiles are registered by SPI -
 * {@link IAS4ProfileRegistrarSPI}.
 *
 * @author Philip Helger
 */
@ThreadSafe
public class AS4ProfileManager implements IAS4ProfileManager, Serializable
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4ProfileManager.class);

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private final ICommonsMap <String, IAS4Profile> m_aMap = new CommonsHashMap <> ();
  @GuardedBy ("m_aRWLock")
  private IAS4Profile m_aDefaultProfile;

  private void _registerAll ()
  {
    m_aRWLock.writeLocked ( () -> {
      m_aMap.clear ();
      m_aDefaultProfile = null;
    });
    for (final IAS4ProfileRegistrarSPI aSPI : ServiceLoaderHelper.getAllSPIImplementations (IAS4ProfileRegistrarSPI.class))
      aSPI.registerAS4Profile (this);

    final int nCount = getProfileCount ();
    LOGGER.info ((nCount == 1 ? "1 AS4 profile is registered " : nCount + " AS4 profiles are registered"));
  }

  public AS4ProfileManager ()
  {
    _registerAll ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IAS4Profile> getAllProfiles ()
  {
    return m_aRWLock.readLockedGet (m_aMap::copyOfValues);
  }

  @Nonnegative
  public final int getProfileCount ()
  {
    return m_aRWLock.readLockedInt (m_aMap::size);
  }

  @Nullable
  public IAS4Profile getProfileOfID (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return null;

    return m_aRWLock.readLockedGet ( () -> m_aMap.get (sID));
  }

  public void registerProfile (@Nonnull final IAS4Profile aAS4Profile)
  {
    ValueEnforcer.notNull (aAS4Profile, "AS4Profile");

    final String sID = aAS4Profile.getID ();
    m_aRWLock.writeLocked ( () -> {
      if (m_aMap.containsKey (sID))
        throw new IllegalStateException ("An AS4 profile with ID '" + sID + "' is already registered!");
      m_aMap.put (sID, aAS4Profile);

      // Make the first the default as fallback
      if (m_aMap.size () == 1)
        m_aDefaultProfile = aAS4Profile;
    });

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Registered" + (aAS4Profile.isDeprecated () ? " deprecated" : "") + " AS4 profile '" + sID + "'");
  }

  public boolean hasDefaultProfile ()
  {
    return m_aRWLock.readLockedBoolean ( () -> m_aDefaultProfile != null);
  }

  @Nullable
  public IAS4Profile getDefaultProfileOrNull ()
  {
    return m_aRWLock.readLockedGet ( () -> {
      IAS4Profile ret = m_aDefaultProfile;
      if (ret == null)
      {
        final int nCount = m_aMap.size ();
        if (nCount == 1)
          ret = m_aMap.getFirstValue ();
      }
      return ret;
    });
  }

  @Nonnull
  public IAS4Profile getDefaultProfile ()
  {
    return m_aRWLock.readLockedGet ( () -> {
      IAS4Profile ret = m_aDefaultProfile;
      if (ret == null)
      {
        final int nCount = m_aMap.size ();
        if (nCount == 1)
          ret = m_aMap.getFirstValue ();
        else
        {
          if (nCount == 0)
            throw new IllegalStateException ("No AS4 profile is present, so no default profile can be determined!");
          throw new IllegalStateException (nCount + " AS4 profiles are present, but none is declared default!");
        }
      }
      return ret;
    });
  }

  public void setDefaultProfile (@Nullable final IAS4Profile aAS4Profile)
  {
    final EChange eChanged = m_aRWLock.writeLockedGet ( () -> {
      if (EqualsHelper.equals (aAS4Profile, m_aDefaultProfile))
        return EChange.UNCHANGED;
      m_aDefaultProfile = aAS4Profile;
      return EChange.CHANGED;
    });

    if (eChanged.isChanged ())
      if (aAS4Profile == null)
        LOGGER.info ("Removed the default AS4 profile");
      else
        LOGGER.info ("Set the default AS4 profile to '" +
                     aAS4Profile.getID () +
                     "'" +
                     (aAS4Profile.isDeprecated () ? " which is deprecated" : ""));
  }

  public void reloadAll ()
  {
    _registerAll ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Map", m_aMap)
                                       .append ("DefaultProfile", m_aDefaultProfile)
                                       .getToString ();
  }
}
