package com.helger.as4server.ws.msh;

/**
 * This class represents the Sending and Receiving MSH including the functions
 * these two need: Submit, Deliver, Notify, Send and Receive.
 * 
 * @author bayerlma
 */
public class MessageServiceHandler
{
  /**
   * This operation transfers enough data from the producer to the Sending MSH
   * to generate an ebMS User Message Unit.
   */
  public void submit ()
  {

  }

  /**
   * This operation makes data of a previously received (via Receive operation)
   * ebMS User Message Unit available to the Consumer.
   */
  public void deliver ()
  {

  }

  /**
   * This operation notifies either a Producer or a Consumer about the status of
   * a previously submitted or received eebMS User Message Unit, or about
   * general MSH status.
   */
  public void notifyMSH ()
  {

  }

  /**
   * This operation initiates the transfer of an ebMS user message from the
   * Sending MSH to the Receiving MSH, after all headers intended for the
   * Receiving MSH have been added (including security and/or reliability, as
   * required).
   */
  public void send ()
  {

  }

  /**
   * This operation completes the transfer of an ebMS user message from the
   * Sending MSH to the Receiving MSH. A successful reception means that a
   * contained User Message Unit is now available for further processing by the
   * Receiving MSH.
   */
  public void receive ()
  {

  }
}
