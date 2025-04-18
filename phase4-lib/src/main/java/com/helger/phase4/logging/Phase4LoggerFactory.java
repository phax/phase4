package com.helger.phase4.logging;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.PresentForCodeCoverage;
import com.helger.commons.string.StringHelper;

/**
 * Specific logger factory for the phase4 library that allows an easy customization of log messages.
 *
 * @author Philip Helger
 * @since 3.1.0-beta3
 */
public final class Phase4LoggerFactory
{
  /**
   * A specific implementation of {@link Logger} that allows to customize the message e.g. with
   * prefix and suffix.
   *
   * @author Philip Helger
   * @since 3.1.0-beta3
   */
  public static class Phase4Logger implements Logger
  {
    private final Logger m_aDelegate;
    private final Function <String, String> m_aMsgCustomizer;

    public Phase4Logger (@Nonnull final Logger aDelegate, @Nonnull final Function <String, String> aMsgCustomizer)
    {
      ValueEnforcer.notNull (aDelegate, "Delegate");
      ValueEnforcer.notNull (aMsgCustomizer, "MsgCustomizer");
      m_aDelegate = aDelegate;
      m_aMsgCustomizer = aMsgCustomizer;
    }

    @Nonnull
    private String _getCustomized (@Nullable final String sMsg)
    {
      return m_aMsgCustomizer.apply (sMsg);
    }

    @Override
    public String getName ()
    {
      return m_aDelegate.getName ();
    }

    @Override
    public LoggingEventBuilder makeLoggingEventBuilder (final Level level)
    {
      return m_aDelegate.makeLoggingEventBuilder (level);
    }

    @Override
    public LoggingEventBuilder atLevel (final Level level)
    {
      return m_aDelegate.atLevel (level);
    }

    @Override
    public boolean isEnabledForLevel (final Level level)
    {
      return m_aDelegate.isEnabledForLevel (level);
    }

    @Override
    public boolean isTraceEnabled ()
    {
      return m_aDelegate.isTraceEnabled ();
    }

    @Override
    public void trace (final String sMsg)
    {
      m_aDelegate.trace (_getCustomized (sMsg));
    }

    @Override
    public void trace (final String sFormat, final Object aArg)
    {
      m_aDelegate.trace (_getCustomized (sFormat), aArg);
    }

    @Override
    public void trace (final String sFormat, final Object aArg1, final Object aArg2)
    {
      m_aDelegate.trace (_getCustomized (sFormat), aArg1, aArg2);
    }

    @Override
    public void trace (final String sFormat, final Object... aArgs)
    {
      m_aDelegate.trace (_getCustomized (sFormat), aArgs);
    }

    @Override
    public void trace (final String sMsg, final Throwable t)
    {
      m_aDelegate.trace (_getCustomized (sMsg), t);
    }

    @Override
    public boolean isTraceEnabled (final Marker aMarker)
    {
      return m_aDelegate.isTraceEnabled (aMarker);
    }

    @Override
    public void trace (final Marker aMarker, final String sMsg)
    {
      m_aDelegate.trace (aMarker, _getCustomized (sMsg));
    }

    @Override
    public void trace (final Marker aMarker, final String sFormat, final Object aArg)
    {
      m_aDelegate.trace (aMarker, _getCustomized (sFormat), aArg);
    }

    @Override
    public void trace (final Marker aMarker, final String sFormat, final Object aArg1, final Object aArg2)
    {
      m_aDelegate.trace (aMarker, _getCustomized (sFormat), aArg1, aArg2);
    }

    @Override
    public void trace (final Marker aMarker, final String sFormat, final Object... aArgs)
    {
      m_aDelegate.trace (aMarker, _getCustomized (sFormat), aArgs);
    }

    @Override
    public void trace (final Marker aMarker, final String sMsg, final Throwable t)
    {
      m_aDelegate.trace (aMarker, _getCustomized (sMsg), t);
    }

    @Override
    public LoggingEventBuilder atTrace ()
    {
      return m_aDelegate.atTrace ();
    }

    @Override
    public boolean isDebugEnabled ()
    {
      return m_aDelegate.isDebugEnabled ();
    }

    @Override
    public void debug (final String sMsg)
    {
      m_aDelegate.debug (_getCustomized (sMsg));
    }

    @Override
    public void debug (final String sFormat, final Object aArg)
    {
      m_aDelegate.debug (_getCustomized (sFormat), aArg);
    }

    @Override
    public void debug (final String sFormat, final Object aArg1, final Object aArg2)
    {
      m_aDelegate.debug (_getCustomized (sFormat), aArg1, aArg2);
    }

    @Override
    public void debug (final String sFormat, final Object... aArgs)
    {
      m_aDelegate.debug (_getCustomized (sFormat), aArgs);
    }

    @Override
    public void debug (final String sMsg, final Throwable t)
    {
      m_aDelegate.debug (_getCustomized (sMsg), t);
    }

    @Override
    public boolean isDebugEnabled (final Marker aMarker)
    {
      return m_aDelegate.isDebugEnabled (aMarker);
    }

    @Override
    public void debug (final Marker aMarker, final String sMsg)
    {
      m_aDelegate.debug (aMarker, _getCustomized (sMsg));
    }

    @Override
    public void debug (final Marker aMarker, final String sFormat, final Object aArg)
    {
      m_aDelegate.debug (aMarker, _getCustomized (sFormat), aArg);
    }

    @Override
    public void debug (final Marker aMarker, final String sFormat, final Object aArg1, final Object aArg2)
    {
      m_aDelegate.debug (aMarker, _getCustomized (sFormat), aArg1, aArg2);
    }

    @Override
    public void debug (final Marker aMarker, final String sFormat, final Object... aArgs)
    {
      m_aDelegate.debug (aMarker, _getCustomized (sFormat), aArgs);
    }

    @Override
    public void debug (final Marker aMarker, final String sMsg, final Throwable t)
    {
      m_aDelegate.debug (aMarker, _getCustomized (sMsg), t);
    }

    @Override
    public LoggingEventBuilder atDebug ()
    {
      return m_aDelegate.atDebug ();
    }

    @Override
    public boolean isInfoEnabled ()
    {
      return m_aDelegate.isInfoEnabled ();
    }

    @Override
    public void info (final String sMsg)
    {
      m_aDelegate.info (_getCustomized (sMsg));
    }

    @Override
    public void info (final String sFormat, final Object aArg)
    {
      m_aDelegate.info (_getCustomized (sFormat), aArg);
    }

    @Override
    public void info (final String sFormat, final Object aArg1, final Object aArg2)
    {
      m_aDelegate.info (_getCustomized (sFormat), aArg1, aArg2);
    }

    @Override
    public void info (final String sFormat, final Object... aArgs)
    {
      m_aDelegate.info (_getCustomized (sFormat), aArgs);
    }

    @Override
    public void info (final String sMsg, final Throwable t)
    {
      m_aDelegate.info (_getCustomized (sMsg), t);
    }

    @Override
    public boolean isInfoEnabled (final Marker aMarker)
    {
      return m_aDelegate.isInfoEnabled (aMarker);
    }

    @Override
    public void info (final Marker aMarker, final String sMsg)
    {
      m_aDelegate.info (aMarker, _getCustomized (sMsg));
    }

    @Override
    public void info (final Marker aMarker, final String sFormat, final Object aArg)
    {
      m_aDelegate.info (aMarker, _getCustomized (sFormat), aArg);
    }

    @Override
    public void info (final Marker aMarker, final String sFormat, final Object aArg1, final Object aArg2)
    {
      m_aDelegate.info (aMarker, _getCustomized (sFormat), aArg1, aArg2);
    }

    @Override
    public void info (final Marker aMarker, final String sFormat, final Object... aArgs)
    {
      m_aDelegate.info (aMarker, _getCustomized (sFormat), aArgs);
    }

    @Override
    public void info (final Marker aMarker, final String sMsg, final Throwable t)
    {
      m_aDelegate.info (aMarker, _getCustomized (sMsg), t);
    }

    @Override
    public LoggingEventBuilder atInfo ()
    {
      return m_aDelegate.atInfo ();
    }

    @Override
    public boolean isWarnEnabled ()
    {
      return m_aDelegate.isWarnEnabled ();
    }

    @Override
    public void warn (final String sMsg)
    {
      m_aDelegate.warn (_getCustomized (sMsg));
    }

    @Override
    public void warn (final String sFormat, final Object aArg)
    {
      m_aDelegate.warn (_getCustomized (sFormat), aArg);
    }

    @Override
    public void warn (final String sFormat, final Object aArg1, final Object aArg2)
    {
      m_aDelegate.warn (_getCustomized (sFormat), aArg1, aArg2);
    }

    @Override
    public void warn (final String sFormat, final Object... aArgs)
    {
      m_aDelegate.warn (_getCustomized (sFormat), aArgs);
    }

    @Override
    public void warn (final String sMsg, final Throwable t)
    {
      m_aDelegate.warn (_getCustomized (sMsg), t);
    }

    public boolean isWarnEnabled (final Marker aMarker)
    {
      return m_aDelegate.isWarnEnabled (aMarker);
    }

    @Override
    public void warn (final Marker aMarker, final String sMsg)
    {
      m_aDelegate.warn (aMarker, _getCustomized (sMsg));
    }

    @Override
    public void warn (final Marker aMarker, final String sFormat, final Object aArg)
    {
      m_aDelegate.warn (aMarker, _getCustomized (sFormat), aArg);
    }

    @Override
    public void warn (final Marker aMarker, final String sFormat, final Object aArg1, final Object aArg2)
    {
      m_aDelegate.warn (aMarker, _getCustomized (sFormat), aArg1, aArg2);
    }

    @Override
    public void warn (final Marker aMarker, final String sFormat, final Object... aArgs)
    {
      m_aDelegate.warn (aMarker, _getCustomized (sFormat), aArgs);
    }

    @Override
    public void warn (final Marker aMarker, final String sMsg, final Throwable t)
    {
      m_aDelegate.warn (aMarker, _getCustomized (sMsg), t);
    }

    @Override
    public LoggingEventBuilder atWarn ()
    {
      return m_aDelegate.atWarn ();
    }

    @Override
    public boolean isErrorEnabled ()
    {
      return m_aDelegate.isErrorEnabled ();
    }

    @Override
    public void error (final String sMsg)
    {
      m_aDelegate.error (_getCustomized (sMsg));
    }

    @Override
    public void error (final String sFormat, final Object aArg)
    {
      m_aDelegate.error (_getCustomized (sFormat), aArg);
    }

    @Override
    public void error (final String sFormat, final Object aArg1, final Object aArg2)
    {
      m_aDelegate.error (_getCustomized (sFormat), aArg1, aArg2);
    }

    @Override
    public void error (final String sFormat, final Object... aArgs)
    {
      m_aDelegate.error (_getCustomized (sFormat), aArgs);
    }

    @Override
    public void error (final String sMsg, final Throwable t)
    {
      m_aDelegate.error (_getCustomized (sMsg), t);
    }

    @Override
    public boolean isErrorEnabled (final Marker aMarker)
    {
      return m_aDelegate.isErrorEnabled (aMarker);
    }

    @Override
    public void error (final Marker aMarker, final String sMsg)
    {
      m_aDelegate.error (aMarker, _getCustomized (sMsg));
    }

    @Override
    public void error (final Marker aMarker, final String sFormat, final Object aArg)
    {
      m_aDelegate.error (aMarker, _getCustomized (sFormat), aArg);
    }

    @Override
    public void error (final Marker aMarker, final String sFormat, final Object aArg1, final Object aArg2)
    {
      m_aDelegate.error (aMarker, _getCustomized (sFormat), aArg1, aArg2);
    }

    @Override
    public void error (final Marker aMarker, final String sFormat, final Object... aArgs)
    {
      m_aDelegate.error (aMarker, _getCustomized (sFormat), aArgs);
    }

    @Override
    public void error (final Marker aMarker, final String sMsg, final Throwable t)
    {
      m_aDelegate.error (aMarker, _getCustomized (sMsg), t);
    }

    @Override
    public LoggingEventBuilder atError ()
    {
      return m_aDelegate.atError ();
    }

    @Override
    public boolean equals (final Object o)
    {
      if (this == o)
        return true;
      if (o == null || getClass () != o.getClass ())
        return false;

      final Phase4Logger rhs = (Phase4Logger) o;
      return m_aDelegate.equals (rhs.m_aDelegate);
    }

    @Override
    public int hashCode ()
    {
      return m_aDelegate.hashCode ();
    }
  }

  @PresentForCodeCoverage
  private static final Phase4LoggerFactory INSTANCE = new Phase4LoggerFactory ();

  private Phase4LoggerFactory ()
  {}

  private static ThreadLocal <String> s_aThreadLocalPrefix = new ThreadLocal <> ();
  private static ThreadLocal <String> s_aThreadLocalSuffix = new ThreadLocal <> ();

  public static void setThreadLocalPrefix (@Nullable final String sPrefix)
  {
    s_aThreadLocalPrefix.set (sPrefix);
  }

  public static void setThreadLocalSuffix (@Nullable final String sPrefix)
  {
    s_aThreadLocalSuffix.set (sPrefix);
  }

  public static void clearThreadLocals ()
  {
    s_aThreadLocalPrefix.remove ();
    s_aThreadLocalSuffix.remove ();
  }

  private static final Function <String, String> MSG_CUSTOMIZER = sMsg -> {
    final String sPrefix = s_aThreadLocalPrefix.get ();
    final String sSuffix = s_aThreadLocalSuffix.get ();
    final StringBuilder aSB = new StringBuilder ();
    if (StringHelper.hasText (sPrefix))
      aSB.append (sPrefix);
    if (StringHelper.hasText (sMsg))
      aSB.append (sMsg);
    if (StringHelper.hasText (sSuffix))
      aSB.append (sSuffix);
    return aSB.toString ();
  };

  /**
   * Get a new SLF4J logger using the provided class.
   *
   * @param aClass
   *        The class to use. May not be <code>null</code>.
   * @return The wrapped {@link Phase4Logger}. Never <code>null</code>.
   */
  @Nonnull
  public static Phase4Logger getLogger (@Nonnull final Class <?> aClass)
  {
    // This is the only place, where the original SLF4J Logger Factory is invoked
    final Logger aLogger = LoggerFactory.getLogger (aClass);
    return new Phase4Logger (aLogger, MSG_CUSTOMIZER);
  }

  /**
   * Get a new SLF4J logger using the provided logger name.
   *
   * @param sLoggerName
   *        The logger name to use. May neither be <code>null</code> nor empty.
   * @return The wrapped {@link Phase4Logger}. Never <code>null</code>.
   */
  @Nonnull
  public static Phase4Logger getLogger (@Nonnull @Nonempty final String sLoggerName)
  {
    // This is the only place, where the original SLF4J Logger Factory is invoked
    final Logger aLogger = LoggerFactory.getLogger (sLoggerName);
    return new Phase4Logger (aLogger, MSG_CUSTOMIZER);
  }
}
