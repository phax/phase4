package com.helger.phase4.euctp;

public enum EEuCtpService
{
	TRADER_TO_CUSTOMS("eu_ics2_t2c"),
	CUSTOMS_TO_TRADER("eu_ics2_c2t");

	private final String value;

	EEuCtpService(String value)
	{
		this.value = value;
	}

	public String getValue()
	{
		return value;
	}
}
