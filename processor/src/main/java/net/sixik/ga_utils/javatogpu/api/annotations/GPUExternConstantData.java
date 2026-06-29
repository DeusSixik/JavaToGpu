package net.sixik.ga_utils.javatogpu.api.annotations;

/**
 * Declares an externally provided module-level OpenCL constant-data symbol.
 *
 * <p>This annotation is meant for {@code static final} array fields that should be emitted as
 * real OpenCL {@code extern __constant} declarations without embedding any initializer data.
 *
 * <p>The current Java authoring contract requires an explicit {@code = null} initializer so the
 * field is clearly treated as a declaration-only symbol rather than embedded data.
 *
 * <pre>{@code
 * @GPUExternConstantData
 * static final float[] WEIGHTS = null;
 * }</pre>
 */
public @interface GPUExternConstantData {
}
