package net.sixik.ga_utils;

import net.sixik.ga_utils.javatogpu.api.DoublePtr;
import net.sixik.ga_utils.javatogpu.api.GPU;
import net.sixik.ga_utils.javatogpu.api.anotations.CCode;
import net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal;
import net.sixik.ga_utils.javatogpu.runtime.GpuRuntime;
import net.sixik.ga_utils.javatogpu.runtime.opencl.OpenClGpuRuntimeBackend;

public class AdvancedVoxelGen {

    public static void main(String[] args) {
        try (OpenClGpuRuntimeBackend backend = new OpenClGpuRuntimeBackend()) {
            GpuRuntime.setBackend(backend);

            for (int i = 0; i < 2; i++) {
                method();
            }

        } catch (RuntimeException exception) {
            System.err.println("GPU Execution Failed!");
            exception.printStackTrace();
        } finally {
            GpuRuntime.setBackend(GpuRuntime.defaultBackend());
        }
    }

    public static void method() {
        int width = 16;
        int height = 256;
        int depth = 16;
        int totalSize = width * height * depth;

        double[] xCoords = new double[totalSize];
        double[] yCoords = new double[totalSize];
        double[] zCoords = new double[totalSize];
        double[] densityBuffer = new double[totalSize];

        System.out.println("Инициализация массивов (Размер: " + totalSize + " элементов)...");

        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    xCoords[index] = x;
                    yCoords[index] = y;
                    zCoords[index] = z;
                    index++;
                }
            }
        }

        System.out.println("Трансляция AST и компиляция OpenCL ядра...");
        long startTime = System.currentTimeMillis();

        AdvancedVoxelGen.ChunkKernel.carve_caves(xCoords, yCoords, zCoords, densityBuffer);

        long endTime = System.currentTimeMillis();
        System.out.println("Выполнение на GPU заняло: " + (endTime - startTime) + " мс.");

        System.out.println("\nСрез плотности по центру чанка (X=8, Z=8) на разных высотах:");
        System.out.println("------------------------------------------------------------");

        for (int testY = 10; testY <= 250; testY += 30) {
            int i = (testY * depth * width) + (8 * width) + 8;
            double density = densityBuffer[i];

            String blockType = density > 0.0 ? "🟩 Stone" : "⬛ Air";
            System.out.printf("Высота Y = %3d | Плотность: %7.4f | Блок: %s%n", testY, density, blockType);
        }
    }

    public static class ChunkKernel {

        @net.sixik.ga_utils.javatogpu.api.anotations.GPU
        public static void carve_caves(
                @GPUGlobal double[] x_coords,
                @GPUGlobal double[] y_coords,
                @GPUGlobal double[] z_coords,
                @GPUGlobal double[] density_buffer
        ) {
            int id = GPU.get_global_id(0);

            double x = x_coords[id];
            double y = y_coords[id];
            double z = z_coords[id];

            DoublePtr noiseAccumulator = new DoublePtr();
            noiseAccumulator.value = 0.0;

            NoiseMath.fbm3D(noiseAccumulator, x * 0.05, y * 0.05, z * 0.05, 4);

            double baseDensity = 1.0;

            double heightGradient = y / 128.0;
            double finalDensity = baseDensity - heightGradient + noiseAccumulator.value;

            density_buffer[id] = GPU.clamp(finalDensity, -1.0, 1.0);
        }
    }

    public static class NoiseMath {

        /**
         * Fractional Brownian Motion (fBm)
         */
        @CCode(inline = false)
        public static void fbm3D(DoublePtr accum, double x, double y, double z, int octaves) {
            double amplitude = 1.0;
            double frequency = 1.0;
            double maxValue = 0.0;

            for (int i = 0; i < octaves; i++) {
                accum.value += valueNoise3D(x * frequency, y * frequency, z * frequency) * amplitude;
                maxValue += amplitude;
                amplitude *= 0.5;
                frequency *= 2.0;
            }

            accum.value = accum.value / maxValue;
        }

        /**
         * Базовый 3D Value Noise с интерполяцией
         */
        @CCode(inline = false)
        public static double valueNoise3D(double x, double y, double z) {
            double ix = GPU.floor(x);
            double iy = GPU.floor(y);
            double iz = GPU.floor(z);

            double fx = x - ix;
            double fy = y - iy;
            double fz = z - iz;

            double ux = GPU.smoothstep(0.0, 1.0, fx);
            double uy = GPU.smoothstep(0.0, 1.0, fy);
            double uz = GPU.smoothstep(0.0, 1.0, fz);

            double c000 = fast_hash(ix, iy, iz);
            double c100 = fast_hash(ix + 1.0, iy, iz);
            double c010 = fast_hash(ix, iy + 1.0, iz);
            double c110 = fast_hash(ix + 1.0, iy + 1.0, iz);
            double c001 = fast_hash(ix, iy, iz + 1.0);
            double c101 = fast_hash(ix + 1.0, iy, iz + 1.0);
            double c011 = fast_hash(ix, iy + 1.0, iz + 1.0);
            double c111 = fast_hash(ix + 1.0, iy + 1.0, iz + 1.0);

            double x00 = GPU.mix(c000, c100, ux);
            double x10 = GPU.mix(c010, c110, ux);
            double x01 = GPU.mix(c001, c101, ux);
            double x11 = GPU.mix(c011, c111, ux);

            double y0 = GPU.mix(x00, x10, uy);
            double y1 = GPU.mix(x01, x11, uy);

            return GPU.mix(y0, y1, uz);
        }

        /**
         * <p>
         * Нативный Си-код для быстрого псевдослучайного хэширования координат.
         * <br>
         * Используем битовые сдвиги, которые на GPU выполняются за 1 такт.
         * <br>
         * Сигнатура принимает double, но внутри мы кастуем к int.
         * </p>
         */
        @CCode(code = """
                int n = (int)x + (int)y * 57 + (int)z * 131;
                n = (n << 13) ^ n;
                int res = (n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff;
                return (1.0 - (double)res / 1073741824.0);
                """)
        public static native double fast_hash(double x, double y, double z);
    }
}