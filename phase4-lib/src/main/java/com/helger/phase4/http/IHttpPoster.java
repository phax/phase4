package com.helger.phase4.http;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.functional.IConsumer;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.httpclient.HttpClientFactory;
import com.helger.phase4.client.IAS4RetryCallback;
import com.helger.phase4.dump.IAS4OutgoingDumper;

/**
 * Interface for an HTTP POST sender.
 *
 * @author Philip Helger
 * @since 0.13.0
 */
public interface IHttpPoster
{
  /**
   * @return The internal http client factory used for http sending. May not be
   *         <code>null</code>.
   */
  @Nonnull
  HttpClientFactory getHttpClientFactory ();

  /**
   * @return The HTTP Post customizer to be used. May be <code>null</code>.
   * @since 0.8.3
   */
  @Nullable
  IConsumer <? super HttpPost> getHttpCustomizer ();

  /**
   * @return <code>true</code> if HTTP header values should be quoted if they
   *         contain forbidden characters, <code>false</code> if not.
   */
  boolean isQuoteHttpHeaders ();

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
   *        The HTTP response handler that should be used to convert the HTTP
   *        response to a domain object.
   * @return The HTTP response. May be <code>null</code>.
   * @throws IOException
   *         In case of IO error
   */
  @Nullable
  <T> T sendGenericMessage (@Nonnull @Nonempty String sURL,
                            @Nullable HttpHeaderMap aCustomHttpHeaders,
                            @Nonnull HttpEntity aHttpEntity,
                            @Nonnull ResponseHandler <? extends T> aResponseHandler) throws IOException;

  /**
   * Send an arbitrary HTTP POST message to the provided URL, using the
   * contained HttpClientFactory as well as the customizer. Additionally the AS4
   * HTTP debugging is invoked in here.
   *
   * @param sURL
   *        The URL to send to. May neither be <code>null</code> nor empty.
   * @param aCustomHttpHeaders
   *        An optional http header map that should be applied. May be
   *        <code>null</code>.
   * @param aHttpEntity
   *        The HTTP entity to be send. May not be <code>null</code>.
   * @param sMessageID
   *        the AS4 message ID. May not be <code>null</code>.
   * @param nMaxRetries
   * @param nRetryIntervalMS
   * @param aResponseHandler
   *        The HTTP response handler that should be used to convert the HTTP
   *        response to a domain object.
   * @param aOutgoingDumper
   *        An optional outgoing dumper for this message. May be
   *        <code>null</code> to use the global one.
   * @param aRetryCallback
   *        An optional retry callback that is invoked, before a retry happens.
   * @param <T>
   *        Response data type
   * @return
   * @throws IOException
   */
  @Nonnull
  <T> T sendGenericMessageWithRetries (@Nonnull String sURL,
                                       @Nullable HttpHeaderMap aCustomHttpHeaders,
                                       @Nonnull HttpEntity aHttpEntity,
                                       @Nonnull String sMessageID,
                                       int nMaxRetries,
                                       long nRetryIntervalMS,
                                       @Nonnull ResponseHandler <? extends T> aResponseHandler,
                                       @Nullable IAS4OutgoingDumper aOutgoingDumper,
                                       @Nullable IAS4RetryCallback aRetryCallback) throws IOException;
}
