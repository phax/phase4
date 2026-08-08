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
package com.helger.phase4.model.soapfault;

import javax.xml.namespace.QName;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.phase4.model.ESoapVersion;
import com.helger.xml.XMLHelper;
import com.helger.xml.serialize.write.XMLWriter;

/**
 * Immutable, SOAP version aware domain object for a received SOAP Fault. Even though ebMS 3.0 Core
 * requires ebMS layer faults to be expressed as <code>eb:Error</code> signals, a peer may still
 * respond with a plain SOAP Fault - this class detects and represents such responses.
 *
 * @author Philip Helger
 * @since 4.6.0
 */
@Immutable
public class AS4SoapFault
{
  private final ESoapVersion m_eSoapVersion;
  private final QName m_aFaultCode;
  private final QName m_aFaultSubcode;
  private final String m_sFaultReason;
  private final String m_sFaultActorRole;
  private final Element m_aDetailElement;
  private final String m_sRawXML;

  public AS4SoapFault (@NonNull final ESoapVersion eSoapVersion,
                       @Nullable final QName aFaultCode,
                       @Nullable final QName aFaultSubcode,
                       @Nullable final String sFaultReason,
                       @Nullable final String sFaultActorRole,
                       @Nullable final Element aDetailElement,
                       @NonNull final String sRawXML)
  {
    ValueEnforcer.notNull (eSoapVersion, "SoapVersion");
    ValueEnforcer.notNull (sRawXML, "RawXML");
    m_eSoapVersion = eSoapVersion;
    m_aFaultCode = aFaultCode;
    m_aFaultSubcode = aFaultSubcode;
    m_sFaultReason = sFaultReason;
    m_sFaultActorRole = sFaultActorRole;
    m_aDetailElement = aDetailElement;
    m_sRawXML = sRawXML;
  }

  /**
   * @return The SOAP version the fault was expressed in. This may differ from the SOAP version of
   *         the conversation. Never <code>null</code>.
   */
  @NonNull
  public ESoapVersion getSoapVersion ()
  {
    return m_eSoapVersion;
  }

  /**
   * @return The fault code as a {@link QName}. For SOAP 1.1 this is the <code>faultcode</code>
   *         element content, for SOAP 1.2 the <code>Code/Value</code> element content. May be
   *         <code>null</code> if it was absent or empty.
   */
  @Nullable
  public QName getFaultCode ()
  {
    return m_aFaultCode;
  }

  public boolean hasFaultCode ()
  {
    return m_aFaultCode != null;
  }

  /**
   * @return The fault subcode as a {@link QName}. Only ever present for SOAP 1.2
   *         (<code>Code/Subcode/Value</code>). May be <code>null</code>.
   */
  @Nullable
  public QName getFaultSubcode ()
  {
    return m_aFaultSubcode;
  }

  /**
   * @return The human readable fault text. For SOAP 1.1 this is the <code>faultstring</code>
   *         element content, for SOAP 1.2 the first <code>Reason/Text</code> element content. May
   *         be <code>null</code>.
   */
  @Nullable
  public String getFaultReason ()
  {
    return m_sFaultReason;
  }

  /**
   * @return The fault actor (SOAP 1.1 <code>faultactor</code>) respectively role (SOAP 1.2
   *         <code>Role</code>). May be <code>null</code>.
   */
  @Nullable
  public String getFaultActorRole ()
  {
    return m_sFaultActorRole;
  }

  /**
   * @return The raw detail subtree (SOAP 1.1 <code>detail</code>, SOAP 1.2 <code>Detail</code>).
   *         May be <code>null</code>.
   */
  @Nullable
  public Element getDetailElement ()
  {
    return m_aDetailElement;
  }

  /**
   * @return The XML of the full fault response document, primarily meant for dumping. Never
   *         <code>null</code>.
   */
  @NonNull
  public String getRawXML ()
  {
    return m_sRawXML;
  }

  /**
   * @return The retry disposition of this fault, based on the fault code. Never <code>null</code>.
   *         Unknown or absent fault codes are considered {@link EAS4FaultDisposition#TRANSIENT}.
   */
  @NonNull
  public EAS4FaultDisposition getDisposition ()
  {
    if (m_aFaultCode != null)
    {
      final String sLocalPart = m_aFaultCode.getLocalPart ();
      // SOAP 1.1 uses "Client" (with eventual dot-separated subcategories like
      // "Client.Authentication"), SOAP 1.2 uses "Sender"
      if ("Client".equals (sLocalPart) ||
        sLocalPart.startsWith ("Client.") ||
        "Sender".equals (sLocalPart) ||
        "VersionMismatch".equals (sLocalPart) ||
        "MustUnderstand".equals (sLocalPart))
        return EAS4FaultDisposition.PERMANENT;
    }

    // "Server"/"Receiver" as well as unknown or absent fault codes
    return EAS4FaultDisposition.TRANSIENT;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("SoapVersion", m_eSoapVersion)
                                       .appendIfNotNull ("FaultCode", m_aFaultCode)
                                       .appendIfNotNull ("FaultSubcode", m_aFaultSubcode)
                                       .appendIfNotNull ("FaultReason", m_sFaultReason)
                                       .appendIfNotNull ("FaultActorRole", m_sFaultActorRole)
                                       .appendIfNotNull ("DetailElement", m_aDetailElement)
                                       .append ("RawXML", m_sRawXML)
                                       .getToString ();
  }

  /**
   * Parse the provided text content as a {@link QName}, resolving an eventually contained namespace
   * prefix against the namespace context of the provided element.
   *
   * @param sValue
   *        The text content to parse. May be <code>null</code>.
   * @param aContextElement
   *        The element in whose namespace context the prefix is resolved. May not be
   *        <code>null</code>.
   * @return <code>null</code> if the text content is empty.
   */
  @Nullable
  private static QName _parseQName (@Nullable final String sValue, @NonNull final Element aContextElement)
  {
    final String sRealValue = StringHelper.trim (sValue);
    if (StringHelper.isEmpty (sRealValue))
      return null;

    final int nColonIndex = sRealValue.indexOf (':');
    if (nColonIndex < 0)
    {
      // No prefix - resolve the default namespace
      final String sNamespaceURI = aContextElement.lookupNamespaceURI (null);
      return sNamespaceURI == null ? new QName (sRealValue) : new QName (sNamespaceURI, sRealValue);
    }

    final String sPrefix = sRealValue.substring (0, nColonIndex);
    final String sLocalPart = sRealValue.substring (nColonIndex + 1);
    final String sNamespaceURI = aContextElement.lookupNamespaceURI (sPrefix);
    if (sNamespaceURI == null)
    {
      // Unresolvable prefix - keep the local part only
      return new QName (sLocalPart);
    }
    return new QName (sNamespaceURI, sLocalPart, sPrefix);
  }

  @Nullable
  private static String _getChildElementTextContent (@NonNull final Element aParentElement,
                                                     @Nullable final String sNamespaceURI,
                                                     @NonNull final String sLocalName)
  {
    final Element aChildElement = sNamespaceURI == null ? XMLHelper.getFirstChildElementOfName (aParentElement,
                                                                                                sLocalName)
                                                        : XMLHelper.getFirstChildElementOfName (aParentElement,
                                                                                                sNamespaceURI,
                                                                                                sLocalName);
    return aChildElement == null ? null : StringHelper.trim (aChildElement.getTextContent ());
  }

  /**
   * Check if the provided SOAP document represents a SOAP Fault and return the respective
   * <code>Fault</code> element. The Fault must be the first element child of the SOAP
   * <code>Body</code>. Both the SOAP 1.1 and the SOAP 1.2 namespace URI are accepted, no matter
   * what SOAP version the conversation expects. The match is done on namespace URI and local name
   * only - never on the namespace prefix.
   *
   * @param aSoapDocument
   *        The parsed SOAP document to check. May be <code>null</code>.
   * @return The <code>Fault</code> element or <code>null</code> if the document is not a SOAP
   *         Fault.
   */
  @Nullable
  public static Element getSoapFaultElementOrNull (@Nullable final Document aSoapDocument)
  {
    if (aSoapDocument == null)
      return null;

    final Element aRootElement = aSoapDocument.getDocumentElement ();
    if (aRootElement == null)
      return null;

    // Accept SOAP 1.1 and SOAP 1.2, independent of the expected SOAP version
    final ESoapVersion eSoapVersion = ESoapVersion.getFromNamespaceURIOrNull (aRootElement.getNamespaceURI ());
    if (eSoapVersion == null)
      return null;

    final String sNamespaceURI = eSoapVersion.getNamespaceURI ();
    final Element aBodyElement = XMLHelper.getFirstChildElementOfName (aRootElement,
                                                                       sNamespaceURI,
                                                                       eSoapVersion.getBodyElementName ());
    if (aBodyElement == null)
      return null;

    final Element aBodyFirstChild = XMLHelper.getFirstChildElement (aBodyElement);
    if (aBodyFirstChild == null)
      return null;

    // Check the local name first, for a fast rejection of the common non-fault case
    if (!"Fault".equals (aBodyFirstChild.getLocalName ()))
      return null;
    if (!sNamespaceURI.equals (aBodyFirstChild.getNamespaceURI ()))
      return null;

    return aBodyFirstChild;
  }

  /**
   * Check if the provided SOAP document represents a SOAP Fault and parse it into an
   * {@link AS4SoapFault}. The raw XML for dumping is created by serializing the provided document.
   *
   * @param aSoapDocument
   *        The parsed SOAP document to check. May be <code>null</code>.
   * @return <code>null</code> if the document is not a SOAP Fault.
   * @see #getSoapFaultElementOrNull(Document)
   */
  @Nullable
  public static AS4SoapFault createOrNull (@Nullable final Document aSoapDocument)
  {
    return createOrNull (aSoapDocument, null);
  }

  /**
   * Check if the provided SOAP document represents a SOAP Fault and parse it into an
   * {@link AS4SoapFault}.
   *
   * @param aSoapDocument
   *        The parsed SOAP document to check. May be <code>null</code>.
   * @param sRawXML
   *        The raw XML the document was parsed from, for dumping. If <code>null</code>, the
   *        provided document is serialized instead.
   * @return <code>null</code> if the document is not a SOAP Fault.
   * @see #getSoapFaultElementOrNull(Document)
   */
  @Nullable
  public static AS4SoapFault createOrNull (@Nullable final Document aSoapDocument, @Nullable final String sRawXML)
  {
    final Element aFaultElement = getSoapFaultElementOrNull (aSoapDocument);
    if (aFaultElement == null)
      return null;

    // The detection above already verified it's one of the two known namespace URIs
    final ESoapVersion eSoapVersion = ESoapVersion.getFromNamespaceURIOrNull (aFaultElement.getNamespaceURI ());
    final String sRealRawXML = sRawXML != null ? sRawXML : XMLWriter.getNodeAsString (aSoapDocument);

    return switch (eSoapVersion)
    {
      case SOAP_11 ->
      {
        // All fault child elements are unqualified
        final Element aFaultCodeElement = XMLHelper.getFirstChildElementOfName (aFaultElement, "faultcode");
        final QName aFaultCode = aFaultCodeElement == null ? null
                                                           : _parseQName (aFaultCodeElement.getTextContent (),
                                                                          aFaultCodeElement);
        final String sFaultString = _getChildElementTextContent (aFaultElement, null, "faultstring");
        final String sFaultActor = _getChildElementTextContent (aFaultElement, null, "faultactor");
        final Element aDetailElement = XMLHelper.getFirstChildElementOfName (aFaultElement, "detail");
        yield new AS4SoapFault (eSoapVersion, aFaultCode, null, sFaultString, sFaultActor, aDetailElement, sRealRawXML);
      }
      case SOAP_12 ->
      {
        // SOAP 1.2 - all fault child elements are in the SOAP 1.2 namespace
        final String sNamespaceURI = eSoapVersion.getNamespaceURI ();
        QName aFaultCode = null;
        QName aFaultSubcode = null;
        final Element aCodeElement = XMLHelper.getFirstChildElementOfName (aFaultElement, sNamespaceURI, "Code");
        if (aCodeElement != null)
        {
          final Element aValueElement = XMLHelper.getFirstChildElementOfName (aCodeElement, sNamespaceURI, "Value");
          if (aValueElement != null)
            aFaultCode = _parseQName (aValueElement.getTextContent (), aValueElement);

          final Element aSubcodeElement = XMLHelper.getFirstChildElementOfName (aCodeElement, sNamespaceURI, "Subcode");
          if (aSubcodeElement != null)
          {
            final Element aSubcodeValueElement = XMLHelper.getFirstChildElementOfName (aSubcodeElement,
                                                                                       sNamespaceURI,
                                                                                       "Value");
            if (aSubcodeValueElement != null)
              aFaultSubcode = _parseQName (aSubcodeValueElement.getTextContent (), aSubcodeValueElement);
          }
        }

        String sFaultReason = null;
        final Element aReasonElement = XMLHelper.getFirstChildElementOfName (aFaultElement, sNamespaceURI, "Reason");
        if (aReasonElement != null)
        {
          // Take the first "Text" element
          final Element aTextElement = XMLHelper.getFirstChildElementOfName (aReasonElement, sNamespaceURI, "Text");
          if (aTextElement != null)
            sFaultReason = StringHelper.trim (aTextElement.getTextContent ());
        }

        final String sRole = _getChildElementTextContent (aFaultElement, sNamespaceURI, "Role");
        final Element aDetailElement = XMLHelper.getFirstChildElementOfName (aFaultElement, sNamespaceURI, "Detail");
        yield new AS4SoapFault (eSoapVersion,
                                aFaultCode,
                                aFaultSubcode,
                                sFaultReason,
                                sRole,
                                aDetailElement,
                                sRealRawXML);
      }
    };
  }
}
