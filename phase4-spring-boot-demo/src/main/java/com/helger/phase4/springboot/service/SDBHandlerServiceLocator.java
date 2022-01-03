/*
 * Copyright (C) 2021-2022 Philip Helger (www.helger.com)
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
package com.helger.phase4.springboot.service;

import javax.annotation.Nonnull;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.helger.phase4.springboot.enumeration.ESBDHHandlerServiceSelector;

@Component
public class SDBHandlerServiceLocator implements ApplicationContextAware
{
  private static ApplicationContext s_aAppContext;

  /**
   * Returns the Spring managed bean instance of the given service if it exists.
   * Returns null otherwise.
   *
   * @param selector
   *        Selector enum. May not be <code>null</code>.
   * @return The desired service
   */
  @Nonnull
  public static ISBDHandlerService getService (@Nonnull final ESBDHHandlerServiceSelector selector)
  {
    return s_aAppContext.getBean (selector.getLabel (), ISBDHandlerService.class);
  }

  @Override
  public void setApplicationContext (@Nonnull final ApplicationContext aCtx) throws BeansException
  {
    // store ApplicationContext reference to access required beans later on
    s_aAppContext = aCtx;
  }
}
