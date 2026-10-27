package dev.hollow.core.module;

/** Modules that run every client tick. Called whether or not the module is enabled. */
public interface Tickable {
	void tick();
}
