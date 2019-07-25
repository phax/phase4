/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.as4.servlet;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.callback.IThrowingRunnable;
import com.helger.commons.concurrent.BasicThreadFactory;
import com.helger.commons.concurrent.ExecutorServiceHelper;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;

/**
 * Asynchronous worker pool that handles stuff that runs in the background.
 *
 * @author Philip Helger
 */
public class AS4WorkerPool extends AbstractGlobalSingleton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4WorkerPool.class);

  private final ExecutorService m_aES;

  @Deprecated
  @UsedViaReflection
  public AS4WorkerPool ()
  {
    this (Runtime.getRuntime ().availableProcessors () * 2);
  }

  protected AS4WorkerPool (@Nonnegative final int nThreadPoolSize)
  {
    m_aES = Executors.newFixedThreadPool (nThreadPoolSize,
                                          new BasicThreadFactory.Builder ().setDaemon (true)
                                                                           .setNamingPattern ("as4-worker-%d")
                                                                           .build ());
  }

  @Nonnull
  public static AS4WorkerPool getInstance ()
  {
    return getGlobalSingleton (AS4WorkerPool.class);
  }

  @Override
  protected void onDestroy (@Nonnull final IScope aScopeInDestruction) throws Exception
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("AS4 worker pool about to be closed");
    ExecutorServiceHelper.shutdownAndWaitUntilAllTasksAreFinished (m_aES);
    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("AS4 worker pool was closed!");
  }

  @Nonnull
  public CompletableFuture <Void> run (@Nonnull final IThrowingRunnable <? extends Exception> aRunnable)
  {
    return CompletableFuture.runAsync ( () -> {
      try
      {
        aRunnable.run ();
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Error running AS4 runner " + aRunnable, ex);
      }
    }, m_aES);
  }

  @Nonnull
  public <T> CompletableFuture <T> supply (@Nonnull final Supplier <T> aSupplier)
  {
    return CompletableFuture.supplyAsync ( () -> {
      try
      {
        return aSupplier.get ();
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Error running AS4 supplier " + aSupplier, ex);
        return null;
      }
    }, m_aES);
  }
}
