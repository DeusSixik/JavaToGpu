package net.sixik.ga_utils.examples;

import net.sixik.ga_utils.javatogpu.api.anotations.CCode;
import net.sixik.ga_utils.javatogpu.api.anotations.CCodeLibrary;

@CCodeLibrary
public final class ReusableMathLibrary {

    private static final float SCALE = 0.5f;

    private ReusableMathLibrary() {
    }

    @CCode(inline = true)
    public static float square(float value) {
        return (value * value) * SCALE;
    }

    @CCode
    public static float norm(float value) {
        return square(value) + 1.0f;
    }
}
