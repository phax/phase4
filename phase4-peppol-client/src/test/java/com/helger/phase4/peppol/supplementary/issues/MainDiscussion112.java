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
package com.helger.phase4.peppol.supplementary.issues;

import java.time.OffsetDateTime;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.state.EContinue;
import com.helger.phase4.duplicate.IAS4DuplicateItem;
import com.helger.phase4.duplicate.IAS4DuplicateManager;
import com.helger.phase4.mgr.AS4ManagerFactoryInMemory;
import com.helger.phase4.mgr.MetaAS4Manager;

public class MainDiscussion112
{
  public static class MyDuplicateManager implements IAS4DuplicateManager
  {
    public boolean isEmpty ()
    {
      // placeholder
      return false;
    }

    public int size ()
    {
      // placeholder
      return 0;
    }

    public IAS4DuplicateItem getItemOfMessageID (final String sMessageID)
    {
      // placeholder
      return null;
    }

    public ICommonsList <IAS4DuplicateItem> getAll ()
    {
      // placeholder
      return null;
    }

    public EContinue registerAndCheck (final String sMessageID, final String sProfileID, final String sPModeID)
    {
      // placeholder
      return null;
    }

    public EChange clearCache ()
    {
      // placeholder
      return null;
    }

    public ICommonsList <String> evictAllItemsBefore (final OffsetDateTime aRefDT)
    {
      // placeholder
      return null;
    }
  }

  public static class MyManagerFactory extends AS4ManagerFactoryInMemory
  {
    @Override
    public IAS4DuplicateManager createDuplicateManager ()
    {
      // Create your duplicate manager
      return new MyDuplicateManager ();
    }
  }

  public static void main (final String [] args)
  {
    MetaAS4Manager.setFactory (new MyManagerFactory ());
    // add the rest of the code here
  }
}
