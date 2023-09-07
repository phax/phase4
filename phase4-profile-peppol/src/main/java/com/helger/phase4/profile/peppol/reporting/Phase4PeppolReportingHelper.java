package com.helger.phase4.profile.peppol.reporting;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helper.peppol.reporting.api.PeppolReportingItem;

public class Phase4PeppolReportingHelper
{
  private Phase4PeppolReportingHelper ()
  {}

  public static void storeReportingItem (@Nonnull final PeppolReportingItem aReportingItem)
  {
    ValueEnforcer.notNull (aReportingItem, "ReportingItem");
    // TODO
  }
}
