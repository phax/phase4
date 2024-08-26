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
package com.helger.phase4.messaging.http;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.function.Consumer;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.HttpEntityWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.concurrent.ThreadHelper;
import com.helger.commons.http.CHttp;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.timing.StopWatch;
import com.helger.commons.wrapper.Wrapper;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.IHttpClientProvider;
import com.helger.phase4.client.IAS4RetryCallback;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.messaging.EAS4MessageMode;
import com.helger.phase4.util.MultiOutputStream;

/**
 * A generic HTTP POST wrapper based on {@link IHttpClientProvider} and
 * {@link HttpPost}. Since 0.13.0 this is a standalone class which is injected
 * as a member into the respective AS4 clients.
 *
 * @author Philip Helger
 */
public class BasicHttpPoster implements IHttpPoster
{
  /**
   * @return The default {@link HttpClientFactory} to be used. Never
   *         <code>null</code>.
   * @since 0.8.3
   */
  @Nonnull
  public static HttpClientFactory createDefaultHttpClientFactory ()
  {
    return new HttpClientFactory ();
  }

  public static final boolean DEFAULT_QUOTE_HTTP_HEADERS = false;
  private static final Logger LOGGER = LoggerFactory.getLogger (BasicHttpPoster.class);

  // By default no special SSL context present
  private HttpClientFactory m_aHttpClientFactory = createDefaultHttpClientFactory ();
  private Consumer <? super HttpPost> m_aHttpCustomizer;
  private boolean m_bQuoteHttpHeaders = DEFAULT_QUOTE_HTTP_HEADERS;

  public BasicHttpPoster ()
  {}

  @Nonnull
  public final HttpClientFactory getHttpClientFactory ()
  {
    return m_aHttpClientFactory;
  }

  @Nonnull
  public final BasicHttpPoster setHttpClientFactory (@Nonnull final HttpClientFactory aHttpClientFactory)
  {
    ValueEnforcer.notNull (aHttpClientFactory, "HttpClientFactory");
    m_aHttpClientFactory = aHttpClientFactory;
    return this;
  }

  @Nullable
  public final Consumer <? super HttpPost> getHttpCustomizer ()
  {
    return m_aHttpCustomizer;
  }

  @Nonnull
  public final BasicHttpPoster setHttpCustomizer (@Nullable final Consumer <? super HttpPost> aHttpCustomizer)
  {
    m_aHttpCustomizer = aHttpCustomizer;
    return this;
  }

  public final boolean isQuoteHttpHeaders ()
  {
    return m_bQuoteHttpHeaders;
  }

  @Nonnull
  public final BasicHttpPoster setQuoteHttpHeaders (final boolean bQuoteHttpHeaders)
  {
    m_bQuoteHttpHeaders = bQuoteHttpHeaders;
    return this;
  }

  /**
   * Send an arbitrary HTTP POST message to the provided URL, using the
   * contained HttpClientFactory as well as the customizer. Additionally the AS4
   * HTTP debugging is invoked in here.<br>
   * This method does NOT retry
   *
   * @param <T>
   *        Response data type
   * @param sURL
   *        The URL to send to. May neither be <code>null</code> nor empty.
   * @param aCustomHttpHeaders
   *        An optional http header map that should be applied. May be
   *        <code>null</code>.
   * @param aHttpEntity
   *        The HTTP entity to be send. May not be <code>null</code>.
   * @param aResponseHandler
   *        The Http response handler that should be used to convert the HTTP
   *        response to a domain object.
   * @return The HTTP response. May be <code>null</code>.
   * @throws IOException
   *         In case of IO error
   */
  @Nullable
  public <T> T sendGenericMessage (@Nonnull @Nonempty final String sURL,
                                   @Nullable final HttpHeaderMap aCustomHttpHeaders,
                                   @Nonnull final HttpEntity aHttpEntity,
                                   @Nonnull final HttpClientResponseHandler <? extends T> aResponseHandler) throws IOException
  {
    ValueEnforcer.notEmpty (sURL, "URL");
    ValueEnforcer.notNull (aHttpEntity, "HttpEntity");

    final StopWatch aSW = StopWatch.createdStarted ();
    LOGGER.info ("Starting to transmit AS4 Message to '" + sURL + "'");

    IOException aCaughtException = null;
    try (final HttpClientManager aClientMgr = new HttpClientManager (m_aHttpClientFactory))
    {
      final HttpPost aPost = new HttpPost (sURL);

      if (aCustomHttpHeaders != null)
      {
        // Always unify line endings
        // By default quoting is disabled
        aCustomHttpHeaders.forEachSingleHeader (aPost::addHeader, true, m_bQuoteHttpHeaders);
      }

      aPost.setEntity (aHttpEntity);

      // Invoke optional customizer
      if (m_aHttpCustomizer != null)
        m_aHttpCustomizer.accept (aPost);

      // Debug sending
      AS4HttpDebug.debug ( () -> {
        final StringBuilder ret = new StringBuilder ("SEND-START to ").append (sURL).append ("\n");
        try
        {
          for (final Header aHeader : aPost.getHeaders ())
            ret.append (aHeader.getName ()).append (": ").append (aHeader.getValue ()).append (CHttp.EOL);
          ret.append (CHttp.EOL);
          if (aHttpEntity.isRepeatable ())
            ret.append (EntityUtils.toString (aHttpEntity));
          else
            ret.append ("## The payload is marked as 'not repeatable' and is the therefore not printed in debugging");
        }
        catch (final Exception ex)
        {
          ret.append ("## Exception listing payload: " + ex.getClass ().getName () + " -- " + ex.getMessage ())
             .append (CHttp.EOL);
          ret.append ("## ").append (StackTraceHelper.getStackAsString (ex));
        }
        return ret.toString ();
      });

      return aClientMgr.execute (aPost, aResponseHandler);
    }
    catch (final IOException ex)
    {
      aCaughtException = ex;
      throw ex;
    }
    finally
    {
      aSW.stop ();
      LOGGER.info ((aCaughtException != null ? "Failed" : "Finished") +
                   " transmitting AS4 Message to '" +
                   sURL +
                   "' after " +
                   aSW.getMillis () +
                   " ms");
    }
  }

  @Nonnull
  protected static HttpEntity createDumpingHttpEntity (@Nullable final IAS4OutgoingDumper aOutgoingDumper,
                                                       @Nonnull final HttpEntity aSrcEntity,
                                                       @Nonnull @Nonempty final String sMessageID,
                                                       @Nullable final HttpHeaderMap aCustomHttpHeaders,
                                                       @Nonnegative final int nTry,
                                                       @Nonnull final Wrapper <OutputStream> aDumpOSHolder) throws IOException
  {
    if (aOutgoingDumper == null)
    {
      // No dumper
      return aSrcEntity;
    }

    // We don't have a message processing state
    final OutputStream aDumpOS = aOutgoingDumper.onBeginRequest (EAS4MessageMode.REQUEST,
                                                                 null,
                                                                 null,
                                                                 sMessageID,
                                                                 aCustomHttpHeaders,
                                                                 nTry);
    if (aDumpOS == null)
    {
      // No dumping needed
      return aSrcEntity;
    }

    // Otherwise multiple calls to writeTo and getContent would crash
    if (!aSrcEntity.isRepeatable ())
      throw new IllegalStateException ("If dumping of outgoing messages is enabled, a repeatable entity must be provided");

    // Remember the output stream used for dumping (to be able to close it
    // later)
    aDumpOSHolder.set (aDumpOS);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Creating dumping entity for the current request");

    return new HttpEntityWrapper (aSrcEntity)
    {
      @Override
      public void writeTo (@Nonnull @WillNotClose final OutputStream aHttpOS) throws IOException
      {
        // Write to multiple output streams
        final MultiOutputStream aMultiOS = new MultiOutputStream (aHttpOS, aDumpOS);
        // write to both streams
        super.writeTo (aMultiOS);
        // Flush both, but do not close both
        aMultiOS.flush ();
      }
    };
  }

  @Nullable
  public <T> T sendGenericMessageWithRetries (@Nonnull final String sURL,
                                              @Nullable final HttpHeaderMap aCustomHttpHeaders,
                                              @Nonnull final HttpEntity aHttpEntity,
                                              @Nonnull final String sMessageID,
                                              @Nonnull final HttpRetrySettings aRetrySettings,
                                              @Nonnull final HttpClientResponseHandler <? extends T> aResponseHandler,
                                              @Nullable final IAS4OutgoingDumper aOutgoingDumper,
                                              @Nullable final IAS4RetryCallback aRetryCallback) throws IOException
  {
    // Parameter or global one - may still be null
    final IAS4OutgoingDumper aRealOutgoingDumper = aOutgoingDumper != null ? aOutgoingDumper : AS4DumpManager
                                                                                                             .getOutgoingDumper ();

    // This class holds the effective OutputStream to which the dump is written
    final Wrapper <OutputStream> aDumpOSHolder = new Wrapper <> ();
    IOException aCaughtException = null;
    try
    {
      if (aRetrySettings.isRetryEnabled ())
      {
        // Send with retry
        if (!aHttpEntity.isRepeatable ())
          throw new IllegalStateException ("If retry is enabled, a repeatable entity must be provided");

        final int nMaxRetries = aRetrySettings.getMaxRetries ();
        final int nMaxTries = 1 + nMaxRetries;
        Duration aDurationBeforeRetry = aRetrySettings.getDurationBeforeRetry ();
        for (int nTry = 0; nTry < nMaxTries; nTry++)
        {
          if (nTry > 0)
            LOGGER.info ("Retry #" + nTry + "/" + nMaxRetries + " for sending message with ID '" + sMessageID + "'");

          try
          {
            // Create a new one every time (for new filename, new timestamp,
            // etc.)
            final HttpEntity aDumpingEntity = createDumpingHttpEntity (aRealOutgoingDumper,
                                                                       aHttpEntity,
                                                                       sMessageID,
                                                                       aCustomHttpHeaders,
                                                                       nTry,
                                                                       aDumpOSHolder);

            // Dump only for the first try - the remaining tries
            return sendGenericMessage (sURL, aCustomHttpHeaders, aDumpingEntity, aResponseHandler);
          }
          catch (final IOException ex)
          {
            // Last try? -> propagate exception
            if (nTry == nMaxTries - 1)
              throw ex;

            // After the first retry, increase the waiting time
            if (nTry > 1)
              aDurationBeforeRetry = HttpRetrySettings.getIncreased (aDurationBeforeRetry,
                                                                     aRetrySettings.getRetryIncreaseFactor ());

            if (aRetryCallback != null)
              if (aRetryCallback.onBeforeRetry (sMessageID, sURL, nTry, nMaxTries, aDurationBeforeRetry.toMillis (), ex)
                                .isBreak ())
              {
                // Explicitly interrupt retry
                LOGGER.warn ("Error sending message '" +
                             sMessageID +
                             "' to '" +
                             sURL +
                             ": " +
                             ex.getClass ().getSimpleName () +
                             " - " +
                             ex.getMessage () +
                             " - retrying was explicitly stopped by the RetryCallback");

                // Propagate Exception as if it would be the last retry
                throw ex;
              }

            LOGGER.warn ("Error sending message '" +
                         sMessageID +
                         "' to '" +
                         sURL +
                         "': " +
                         ex.getClass ().getSimpleName () +
                         " - " +
                         ex.getMessage () +
                         " - waiting " +
                         aDurationBeforeRetry.toMillis () +
                         " ms, than retrying");

            // Sleep and try again afterwards
            ThreadHelper.sleep (aDurationBeforeRetry.toMillis ());
          }
          finally
          {
            // Flush and close the dump output stream (if any)
            StreamHelper.close (aDumpOSHolder.get ());
          }
        }
        throw new IllegalStateException ("Should never be reached (after maximum of " + nMaxTries + " tries)!");
      }

      // else non retry
      {
        final HttpEntity aDumpingEntity = createDumpingHttpEntity (aRealOutgoingDumper,
                                                                   aHttpEntity,
                                                                   sMessageID,
                                                                   aCustomHttpHeaders,
                                                                   0,
                                                                   aDumpOSHolder);
        try
        {
          // Send without retry
          return sendGenericMessage (sURL, aCustomHttpHeaders, aDumpingEntity, aResponseHandler);
        }
        finally
        {
          // Close the dump output stream (if any)
          StreamHelper.close (aDumpOSHolder.get ());
        }
      }
    }
    catch (final IOException ex)
    {
      aCaughtException = ex;
      throw ex;
    }
    finally
    {
      // Add the possibility to close open resources
      if (aRealOutgoingDumper != null && aDumpOSHolder.isSet ())
        try
        {
          aRealOutgoingDumper.onEndRequest (EAS4MessageMode.REQUEST, null, null, sMessageID, aCaughtException);
        }
        catch (final Exception ex)
        {
          LOGGER.error ("OutgoingDumper.onEndRequest failed. Dumper=" +
                        aRealOutgoingDumper +
                        "; MessageID=" +
                        sMessageID,
                        ex);
        }
    }
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("HttpClientFactory", m_aHttpClientFactory)
                                       .append ("HttpCustomizer", m_aHttpCustomizer)
                                       .append ("QuoteHttpHeaders", m_bQuoteHttpHeaders)
                                       .getToString ();
  }
}
