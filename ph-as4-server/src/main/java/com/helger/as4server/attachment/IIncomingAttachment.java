package com.helger.as4server.attachment;

import javax.annotation.Nullable;

import com.helger.commons.collection.attr.IAttributeContainer;
import com.helger.commons.io.IHasInputStream;
import com.helger.http.CHTTPHeader;

/**
 * Base interface for a single incoming attachment.
 *
 * @author Philip Helger
 */
public interface IIncomingAttachment extends IHasInputStream, IAttributeContainer <String, String>
{
  @Nullable
  default String getContentID ()
  {
    return getAttributeAsString ("Content-ID");
  }

  @Nullable
  default String getContentTransferEncoding ()
  {
    return getAttributeAsString (CHTTPHeader.CONTENT_TRANSFER_ENCODING);
  }

  @Nullable
  default String getContentType ()
  {
    return getAttributeAsString (CHTTPHeader.CONTENT_TYPE);
  }
}
