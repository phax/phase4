/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4server.ws.msh;

import javax.annotation.Nullable;
import javax.jws.WebResult;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.BindingType;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.soap.SOAPBinding;

@WebServiceProvider (portName = "mshPort", serviceName = "mshService")
@ServiceMode (Service.Mode.MESSAGE)
@BindingType (SOAPBinding.SOAP12HTTP_BINDING)
public class MSHWebservice implements Provider <SOAPMessage>
{
  public MSHWebservice ()
  {}

  @Nullable
  @WebResult
  public SOAPMessage invoke (@Nullable final SOAPMessage aRequest)
  {
    try
    {
      final SOAPBody requestBody = aRequest == null ? null : aRequest.getSOAPBody ();

      final MessageFactory aMsgFactory = MessageFactory.newInstance (SOAPConstants.SOAP_1_2_PROTOCOL);
      final SOAPFactory aSOAPFactory = SOAPFactory.newInstance (SOAPConstants.SOAP_1_2_PROTOCOL);

      final SOAPMessage aSOAPResponse = aMsgFactory.createMessage ();
      final SOAPBody aResponseBody = aSOAPResponse.getSOAPBody ();
      aResponseBody.addBodyElement (aSOAPFactory.createName ("dummy"));
      aResponseBody.addChildElement ("value").setValue ("123.00");
      aSOAPResponse.saveChanges ();
      return aSOAPResponse;
    }
    catch (final SOAPException ex)
    {}
    return null;
  }
}
