/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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
import com.helger.phase4.mgr.ManagerFactoryInMemory;
import com.helger.phase4.mgr.MetaAS4Manager;

public class MainDiscussion112
{
  public static class MyDuplicateManager implements IAS4DuplicateManager
  {
    public boolean isEmpty ()
    {
      // TODO
      return false;
    }

    public int size ()
    {
      // TODO
      return 0;
    }

    public IAS4DuplicateItem getItemOfMessageID (final String sMessageID)
    {
      // TODO
      return null;
    }

    public ICommonsList <IAS4DuplicateItem> getAll ()
    {
      // TODO
      return null;
    }

    public EContinue registerAndCheck (final String sMessageID, final String sProfileID, final String sPModeID)
    {
      // TODO
      return null;
    }

    public EChange clearCache ()
    {
      // TODO
      return null;
    }

    public ICommonsList <String> evictAllItemsBefore (final OffsetDateTime aRefDT)
    {
      // TODO
      return null;
    }
  }

  public static class MyManagerFactory extends ManagerFactoryInMemory
  {
    @Override
    public IAS4DuplicateManager createDuplicateManager ()
    {
      return new MyDuplicateManager ();
    }
  }

  public static void main (final String [] args)
  {
    MetaAS4Manager.setFactory (new MyManagerFactory ());
    // TODO rest of the code
  }
}
