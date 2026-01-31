/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.client;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.WillNotClose;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.model.message.EAS4MessageType;
import com.helger.phase4.util.AS4ResourceHelper;

/**
 * Abstract AS4 client for signal messages with arbitrary content.
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        Implementation type
 */
public abstract class AbstractAS4ClientSignalMessage <IMPLTYPE extends AbstractAS4ClientSignalMessage <IMPLTYPE>>
                                                     extends
                                                     AbstractAS4Client <IMPLTYPE>
{
  private final ICommonsList <Object> m_aAny = new CommonsArrayList <> ();

  protected AbstractAS4ClientSignalMessage (@NonNull final EAS4MessageType eMessageType,
                                            @NonNull @WillNotClose final AS4ResourceHelper aResHelper)
  {
    super (eMessageType, aResHelper);
  }

  /**
   * @return The SignalMessage payload, usually from a different namespace URI than the main
   *         message. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableObject
  public final ICommonsList <Object> any ()
  {
    return m_aAny;
  }
}
