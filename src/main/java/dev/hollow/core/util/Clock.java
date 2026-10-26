package dev.hollow.core.util;

/** Millisecond time source; injectable so timing logic can be unit tested. */
@FunctionalInterface
public interface Clock {
	Clock SYSTEM = System::currentTimeMillis;

	long millis();
}
