package dev.hollow.hands;

/**
 * Fixed-size ring buffer of swing samples in camera space. Each sample stores the point
 * near the hand and the point near the item tip, so consecutive samples form a ribbon.
 */
public final class TrailBuffer {
	private final float[] near;
	private final float[] far;
	private final double[] times;
	private int head;
	private int size;

	public TrailBuffer(int capacity) {
		near = new float[capacity * 3];
		far = new float[capacity * 3];
		times = new double[capacity];
	}

	public int capacity() {
		return times.length;
	}

	public void add(double time, float nx, float ny, float nz, float fx, float fy, float fz) {
		int i = head;
		near[i * 3] = nx;
		near[i * 3 + 1] = ny;
		near[i * 3 + 2] = nz;
		far[i * 3] = fx;
		far[i * 3 + 1] = fy;
		far[i * 3 + 2] = fz;
		times[i] = time;
		head = (head + 1) % times.length;
		size = Math.min(size + 1, times.length);
	}

	/** Removes samples older than {@code lifetime} seconds. */
	public void prune(double now, double lifetime) {
		while (size > 0 && now - times[oldestIndex()] > lifetime) {
			size--;
		}
	}

	public void clear() {
		size = 0;
	}

	public int size() {
		return size;
	}

	/** Index into storage of the i-th sample, 0 = oldest. */
	private int index(int i) {
		return Math.floorMod(head - size + i, times.length);
	}

	private int oldestIndex() {
		return index(0);
	}

	public double time(int i) {
		return times[index(i)];
	}

	public float near(int i, int axis) {
		return near[index(i) * 3 + axis];
	}

	public float far(int i, int axis) {
		return far[index(i) * 3 + axis];
	}
}
