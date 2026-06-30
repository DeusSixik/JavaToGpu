package net.sixik.ga_utils.javatogpu.runtime;

/**
 * Explicit execution configuration for runtime kernel launches.
 *
 * <p>The current public runtime contract supports 1D and 2D global work sizes. 3D can be added later when the
 * backend/runtime surface is extended end-to-end.
 */
public record GpuExecutionConfig(
        int dimensions,
        long globalX,
        long globalY,
        long localX,
        long localY
) {

    public GpuExecutionConfig {
        if (dimensions != 1 && dimensions != 2) {
            throw new IllegalArgumentException("dimensions must be 1 or 2: " + dimensions);
        }
        if (globalX <= 0L) {
            throw new IllegalArgumentException("globalX must be positive: " + globalX);
        }
        if (dimensions == 1) {
            if (globalY != 1L) {
                throw new IllegalArgumentException("globalY must be 1 for 1D execution: " + globalY);
            }
            if (localY != 0L) {
                throw new IllegalArgumentException("localY must be 0 for 1D execution: " + localY);
            }
            if (localX < 0L) {
                throw new IllegalArgumentException("localX must be >= 0: " + localX);
            }
        } else {
            if (globalY <= 0L) {
                throw new IllegalArgumentException("globalY must be positive for 2D execution: " + globalY);
            }
            boolean localUnset = localX == 0L && localY == 0L;
            boolean localSet = localX > 0L && localY > 0L;
            if (!localUnset && !localSet) {
                throw new IllegalArgumentException("localX/localY must both be zero or both be > 0 for 2D execution");
            }
        }
    }

    public static GpuExecutionConfig oneDimensional(long globalWorkSize) {
        return new GpuExecutionConfig(1, globalWorkSize, 1L, 0L, 0L);
    }

    public static GpuExecutionConfig oneDimensional(long globalWorkSize, long localWorkSize) {
        return new GpuExecutionConfig(1, globalWorkSize, 1L, localWorkSize, 0L);
    }

    public static GpuExecutionConfig twoDimensional(long globalX, long globalY) {
        return new GpuExecutionConfig(2, globalX, globalY, 0L, 0L);
    }

    public static GpuExecutionConfig twoDimensional(long globalX, long globalY, long localX, long localY) {
        return new GpuExecutionConfig(2, globalX, globalY, localX, localY);
    }

    public long globalWorkSize() {
        return globalX;
    }
}
