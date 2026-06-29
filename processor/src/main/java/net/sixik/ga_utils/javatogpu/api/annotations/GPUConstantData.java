package net.sixik.ga_utils.javatogpu.api.annotations;

/**
 * Declares an embedded module-level OpenCL constant-data field.
 *
 * <p>This annotation is meant for {@code static final} array fields that should be emitted as
 * real OpenCL {@code __constant} definitions instead of being passed as kernel parameters.
 *
 * <pre>{@code
 * @GPUConstantData
 * static final float[] WEIGHTS = {0.25f, 0.5f, 0.25f};
 * }</pre>
 */
public @interface GPUConstantData {
}
