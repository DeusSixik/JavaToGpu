package net.sixik.ga_utils.javatogpu.runtime.opencl;

import java.util.IdentityHashMap;
import java.util.Map;

public final class OpenClDeviceBufferRegistry {

    private final Map<Object, OpenClDeviceBufferHandle> handles = new IdentityHashMap<>();

    public OpenClDeviceBufferHandle acquire(OpenClBufferBinding binding) {
        return handles.computeIfAbsent(
                binding.sourceArray(),
                key -> new OpenClDeviceBufferHandle(
                        "buffer@" + Integer.toHexString(System.identityHashCode(key)),
                        binding.kind(),
                        binding.access(),
                        key,
                        binding.length()
                )
        );
    }

    void clear() {
        handles.clear();
    }

    int cacheSize() {
        return handles.size();
    }
}
