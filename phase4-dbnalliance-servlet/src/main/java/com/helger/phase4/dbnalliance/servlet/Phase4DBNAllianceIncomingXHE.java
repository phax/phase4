/*
 * Copyright (C) 2025-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.dbnalliance.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

import org.jspecify.annotations.NonNull;
import org.w3c.dom.Element;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.codec.base64.Base64InputStream;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.iface.IHasInputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.phase4.incoming.AS4IncomingJAXBDocument;
import com.helger.xhe.v10.CXHE10;
import com.helger.xhe.v10.XHE10Marshaller;
import com.helger.xhe.v10.XHE10XHEType;
import com.helger.xml.XMLFactory;
import com.helger.xml.stax.StAXHelper;
import com.helger.xml.stax.StAXTextContentReader;

/**
 * The incoming DBNAlliance Exchange Header Envelope, as passed to
 * {@link IPhase4DBNAllianceIncomingXHEHandlerSPI}. The document is not held in memory: use
 * {@link #getInputStream()} to read the complete XHE as often as needed, the payload specific
 * methods to read the content of a single payload, and only use {@link #getXHE()} if the complete
 * document is really needed in memory.
 * <p>
 * Every method reads the XHE again from the start. Nothing is re-serialised: text payloads are
 * delivered as the characters contained in the document, binary payloads are Base64 decoded while
 * reading, and XML payloads are delivered as an {@link XMLStreamReader} or as a DOM element.
 * </p>
 * <p>
 * The underlying data is backed by resources (e.g. temporary files) that are deleted when the
 * message processing is finished, so it must be consumed while the handler is invoked.
 * </p>
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@NotThreadSafe
public class Phase4DBNAllianceIncomingXHE extends AS4IncomingJAXBDocument <XHE10XHEType>
{
  private static final QName QNAME_PAYLOAD = new QName (CXHE10.NAMESPACE_URI_AGGREGATE_COMPONENTS, "Payload");
  private static final QName QNAME_PAYLOAD_CONTENT = new QName (CXHE10.NAMESPACE_URI_AGGREGATE_COMPONENTS,
                                                                "PayloadContent");
  /** The element depth of XHE/Payloads/Payload */
  private static final int DEPTH_PAYLOAD = 3;

  /**
   * An {@link XMLStreamReader} that also closes the underlying input stream.
   */
  private static final class ClosingXMLStreamReader extends StreamReaderDelegate
  {
    private final InputStream m_aIS;

    ClosingXMLStreamReader (@NonNull final XMLStreamReader aReader, @NonNull final InputStream aIS)
    {
      super (aReader);
      m_aIS = aIS;
    }

    @Override
    public void close () throws XMLStreamException
    {
      try
      {
        super.close ();
      }
      finally
      {
        StreamHelper.close (m_aIS);
      }
    }
  }

  /**
   * An {@link InputStream} over a {@link Reader} that only contains ASCII characters, like Base64
   * encoded data.
   */
  private static final class AsciiReaderInputStream extends InputStream
  {
    private final Reader m_aReader;
    private final char [] m_aCharBuf = new char [8192];

    AsciiReaderInputStream (@NonNull final Reader aReader)
    {
      m_aReader = aReader;
    }

    @Override
    public int read () throws IOException
    {
      final int c = m_aReader.read ();
      if (c > 0x7f)
        throw new IOException ("Unexpected non-ASCII character in Base64 encoded payload content");
      return c;
    }

    @Override
    public int read (final byte @NonNull [] aBuf, final int nOfs, final int nLen) throws IOException
    {
      ValueEnforcer.isArrayOfsLen (aBuf, nOfs, nLen);
      if (nLen == 0)
        return 0;
      final int nRead = m_aReader.read (m_aCharBuf, 0, Math.min (nLen, m_aCharBuf.length));
      for (int i = 0; i < nRead; ++i)
      {
        final char c = m_aCharBuf[i];
        if (c > 0x7f)
          throw new IOException ("Unexpected non-ASCII character in Base64 encoded payload content");
        aBuf[nOfs + i] = (byte) c;
      }
      return nRead;
    }

    @Override
    public void close () throws IOException
    {
      m_aReader.close ();
    }
  }

  /**
   * Constructor
   *
   * @param aISP
   *        The input stream provider of the decrypted and decompressed payload. May not be
   *        <code>null</code> and must be readable multiple times.
   */
  public Phase4DBNAllianceIncomingXHE (@NonNull final IHasInputStream aISP)
  {
    super (aISP, XHE10Marshaller::new);
  }

  /**
   * Open the XHE and move to the PayloadContent element of the payload with the provided index.
   *
   * @return A reader positioned on the start element of the PayloadContent. The caller must close
   *         it, which also closes the input stream.
   */
  @NonNull
  private XMLStreamReader _openOnPayloadContent (@Nonnegative final int nPayloadIndex) throws IOException
  {
    ValueEnforcer.isGE0 (nPayloadIndex, "PayloadIndex");

    final InputStream aIS = getInputStream ();
    XMLStreamReader aReader = null;
    boolean bSuccess = false;
    try
    {
      aReader = new ClosingXMLStreamReader (XMLFactory.createNonCoalescingXMLInputFactory ()
                                                      .createXMLStreamReader (aIS), aIS);
      int nDepth = 0;
      int nPayloadCount = -1;
      boolean bInTargetPayload = false;
      while (aReader.hasNext ())
      {
        final int nEventType = aReader.next ();
        if (nEventType == XMLStreamConstants.START_ELEMENT)
        {
          nDepth++;
          if (nDepth == DEPTH_PAYLOAD && QNAME_PAYLOAD.equals (aReader.getName ()))
          {
            nPayloadCount++;
            bInTargetPayload = nPayloadCount == nPayloadIndex;
          }
          else
            if (bInTargetPayload &&
                nDepth == DEPTH_PAYLOAD + 1 &&
                QNAME_PAYLOAD_CONTENT.equals (aReader.getName ()))
            {
              bSuccess = true;
              return aReader;
            }
        }
        else
          if (nEventType == XMLStreamConstants.END_ELEMENT)
          {
            if (bInTargetPayload && nDepth == DEPTH_PAYLOAD)
              throw new IOException ("The XHE payload with index " + nPayloadIndex + " has no PayloadContent");
            nDepth--;
          }
      }
      throw new IOException ("The XHE contains no payload with index " + nPayloadIndex);
    }
    catch (final XMLStreamException ex)
    {
      throw new IOException ("Failed to read the XHE", ex);
    }
    finally
    {
      if (!bSuccess)
      {
        if (aReader != null)
          try
          {
            aReader.close ();
          }
          catch (final XMLStreamException ex)
          {
            // Ignore - the original exception is more relevant
          }
        StreamHelper.close (aIS);
      }
    }
  }

  /**
   * Get the complete Exchange Header Envelope as a JAXB object. It is parsed on the first call and
   * cached afterwards. Note: the memory needed depends on the document size, so prefer
   * {@link #getInputStream()} or the payload specific methods for large documents.
   *
   * @return The parsed Exchange Header Envelope. Never <code>null</code>.
   * @throws IOException
   *         If the document cannot be read or parsed
   */
  @NonNull
  public XHE10XHEType getXHE () throws IOException
  {
    return getParsedDocument ();
  }

  /**
   * Open a reader over the content of a text payload. The characters are delivered as they are
   * contained in the document, chunk by chunk.
   *
   * @param nPayloadIndex
   *        The 0-based index of the payload. Must be &ge; 0.
   * @return A new reader that must be closed by the caller. Never <code>null</code>.
   * @throws IOException
   *         If the payload does not exist, has no content or the content contains an XML element
   *         (the latter is reported while reading)
   * @see com.helger.dbnalliance.xhe.DBNAlliancePayload#getPayloadContentKind()
   */
  @NonNull
  public Reader openPayloadTextReader (@Nonnegative final int nPayloadIndex) throws IOException
  {
    return new StAXTextContentReader (_openOnPayloadContent (nPayloadIndex));
  }

  /**
   * Open a stream over the content of a binary payload. The Base64 encoded content is decoded
   * while reading.
   *
   * @param nPayloadIndex
   *        The 0-based index of the payload. Must be &ge; 0.
   * @return A new stream that must be closed by the caller. Never <code>null</code>.
   * @throws IOException
   *         If the payload does not exist or has no content. Invalid Base64 content is reported
   *         while reading.
   * @see com.helger.dbnalliance.xhe.DBNAlliancePayload#getPayloadContentKind()
   */
  @NonNull
  public InputStream openPayloadBinaryStream (@Nonnegative final int nPayloadIndex) throws IOException
  {
    return new Base64InputStream (new AsciiReaderInputStream (openPayloadTextReader (nPayloadIndex)));
  }

  /**
   * Open an {@link XMLStreamReader} positioned on the root element of an XML payload. The reader
   * is not limited to the payload: read only up to the matching end element (e.g. via JAXB or
   * {@link StAXHelper#readCurrentElementAsDOM(XMLStreamReader)}). The reader is created by a
   * non-coalescing factory, so large text nodes are reported in several chunks.
   *
   * @param nPayloadIndex
   *        The 0-based index of the payload. Must be &ge; 0.
   * @return A new reader that must be closed by the caller. Closing it also closes the underlying
   *         stream. Never <code>null</code>.
   * @throws IOException
   *         If the payload does not exist or its content is not an XML element
   * @see com.helger.dbnalliance.xhe.DBNAlliancePayload#getPayloadContentKind()
   */
  @NonNull
  public XMLStreamReader openPayloadXMLStreamReader (@Nonnegative final int nPayloadIndex) throws IOException
  {
    final XMLStreamReader aReader = _openOnPayloadContent (nPayloadIndex);
    boolean bSuccess = false;
    try
    {
      while (true)
      {
        final int nEventType = aReader.next ();
        switch (nEventType)
        {
          case XMLStreamConstants.START_ELEMENT:
            bSuccess = true;
            return aReader;
          case XMLStreamConstants.CHARACTERS:
          case XMLStreamConstants.CDATA:
          case XMLStreamConstants.SPACE:
            if (!aReader.isWhiteSpace ())
              throw new IOException ("The content of the XHE payload with index " +
                                     nPayloadIndex +
                                     " is not an XML element");
            break;
          case XMLStreamConstants.COMMENT:
          case XMLStreamConstants.PROCESSING_INSTRUCTION:
            break;
          default:
            throw new IOException ("The content of the XHE payload with index " +
                                   nPayloadIndex +
                                   " is not an XML element");
        }
      }
    }
    catch (final XMLStreamException ex)
    {
      throw new IOException ("Failed to read the XHE", ex);
    }
    finally
    {
      if (!bSuccess)
        try
        {
          aReader.close ();
        }
        catch (final XMLStreamException ex)
        {
          // Ignore - the original exception is more relevant
        }
    }
  }

  /**
   * Read the root element of an XML payload into a DOM. Only this payload is held in memory, not
   * the rest of the XHE.
   *
   * @param nPayloadIndex
   *        The 0-based index of the payload. Must be &ge; 0.
   * @return The payload root element in a new document. Never <code>null</code>.
   * @throws IOException
   *         If the payload does not exist or its content is not an XML element
   */
  @NonNull
  public Element getPayloadElement (@Nonnegative final int nPayloadIndex) throws IOException
  {
    final XMLStreamReader aReader = openPayloadXMLStreamReader (nPayloadIndex);
    try
    {
      return StAXHelper.readCurrentElementAsDOM (aReader);
    }
    catch (final XMLStreamException ex)
    {
      throw new IOException ("Failed to read the XHE payload with index " + nPayloadIndex, ex);
    }
    finally
    {
      try
      {
        aReader.close ();
      }
      catch (final XMLStreamException ex)
      {
        // Ignore
      }
    }
  }
}
