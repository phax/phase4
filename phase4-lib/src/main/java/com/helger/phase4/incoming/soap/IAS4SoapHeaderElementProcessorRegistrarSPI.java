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
package com.helger.phase4.incoming.soap;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.IsSPIInterface;

/**
 * SPI interface to register additional {@link ISoapHeaderElementProcessor} implementations in the
 * default {@link SoapHeaderElementProcessorRegistry}. Without this, SOAP header elements that are
 * unknown to phase4 and that carry <code>mustUnderstand</code> lead to a rejection of the incoming
 * message.<br>
 * Implementations are invoked at the very end of
 * {@link SoapHeaderElementProcessorRegistry#createDefault(com.helger.phase4.model.pmode.resolve.IAS4PModeResolver, com.helger.phase4.crypto.IAS4CryptoFactory, com.helger.phase4.crypto.IAS4CryptoFactory, com.helger.phase4.model.pmode.IPMode, com.helger.phase4.incoming.crypto.IAS4IncomingSecurityConfiguration, com.helger.phase4.incoming.IAS4IncomingReceiverConfiguration)}
 * - that means after the Ebms3 Messaging and the WSS4J processors were registered. Since the
 * registration order is also the execution order, the registered processors run last.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@IsSPIInterface
public interface IAS4SoapHeaderElementProcessorRegistrarSPI
{
  /**
   * Register additional SOAP header element processors.
   *
   * @param aRegistry
   *        The registry to register the processors in. Never <code>null</code>. Note:
   *        {@link SoapHeaderElementProcessorRegistry#registerHeaderElementProcessor(javax.xml.namespace.QName, ISoapHeaderElementProcessor)}
   *        throws an exception if a QName is already registered - use
   *        {@link SoapHeaderElementProcessorRegistry#containsHeaderElementProcessor(javax.xml.namespace.QName)}
   *        to check first.
   */
  void registerSoapHeaderElementProcessors (@NonNull SoapHeaderElementProcessorRegistry aRegistry);
}
