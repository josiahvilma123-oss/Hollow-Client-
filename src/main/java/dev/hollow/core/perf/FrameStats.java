package dev.hollow.core.perf;

/**
 * Rolling frame-time statistics over a fixed window, in a preallocated ring buffer: recording a
 * frame allocates nothing, so measuring doesn't disturb what is measured.
 */
public final class FrameStats {
	private final long[] durations;
	private final long[] stamps;
	private int head;
	private int size;
	private long lastFrame = -1;

	public FrameStats(int capacity) {
		durations = new long[capacity];
		stamps = new long[capacity];
	}

	/** Records a frame that finished at {@code nanos}. */
	public void frame(long nanos) {
		if (lastFrame >= 0) {
			long duration = nanos - lastFrame;
			if (duration > 0) {
				durations[head] = duration;
				stamps[head] = nanos;
				head = (head + 1) % durations.length;
				size = Math.min(size + 1, durations.length);
			}
		}
		lastFrame = nanos;
	}

	public void reset() {
		head = 0;
		size = 0;
		lastFrame = -1;
	}

	/** Frames per second over the last {@code windowNanos} (counting frames, not averaging times). */
	public double fps(long now, long windowNanos) {
		int count = 0;
		long oldest = now;
		for (int i = 0; i < size; i++) {
			int index = Math.floorMod(head - 1 - i, durations.length);
			if (now - stamps[index] > windowNanos) {
				break;
			}
			count++;
			oldest = stamps[index] - durations[index];
		}
		long span = now - oldest;
		return count == 0 || span <= 0 ? 0 : count * 1e9 / span;
	}

	/** Mean frame time in milliseconds over the window. */
	public double averageMillis(long now, long windowNanos) {
		long total = 0;
		int count = 0;
		for (int i = 0; i < size; i++) {
			int index = Math.floorMod(head - 1 - i, durations.length);
			if (now - stamps[index] > windowNanos) {
				break;
			}
			total += durations[index];
			count++;
		}
		return count == 0 ? 0 : total / 1e6 / count;
	}

	/** Longest frame in the window, in milliseconds (stutter indicator). */
	public double worstMillis(long now, long windowNanos) {
		long worst = 0;
		for (int i = 0; i < size; i++) {
			int index = Math.floorMod(head - 1 - i, durations.length);
			if (now - stamps[index] > windowNanos) {
				break;
			}
			worst = Math.max(worst, durations[index]);
		}
		return worst / 1e6;
	}
}
