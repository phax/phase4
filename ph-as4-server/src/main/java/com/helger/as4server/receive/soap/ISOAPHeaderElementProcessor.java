package com.helger.as4server.receive.soap;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.as4server.receive.AS4MessageState;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.state.ESuccess;

/**
 * Base interface for SOAP header processors that are invoked for incoming
 * messages.
 *
 * @author Philip Helger
 */
public interface ISOAPHeaderElementProcessor
{
  /**
   * Process the passed header element.
   *
   * @param aSOAPDoc
   *        The complete SOAP document (logically no MIME parts are contained).
   *        Never <code>null</code>.
   * @param aHeaderElement
   *        The DOM node with the header element. Never <code>null</code>.
   * @param aState
   *        The current processing state. Never <code>null</code>.
   * @param aErrorList
   *        The error list to be filled in case there are processing errors.
   *        Never <code>null</code>. The list is always empty initially.
   * @return Never <code>null</code>. If {@link ESuccess#FAILURE} than the
   *         header is treated as "not handled".
   */
  @Nonnull
  ESuccess processHeaderElement (@Nonnull Document aSOAPDoc,
                                 @Nonnull Element aHeaderElement,
                                 @Nonnull AS4MessageState aState,
                                 @Nonnull ErrorList aErrorList);
}
