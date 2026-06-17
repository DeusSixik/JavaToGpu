package net.sixik.ga_utils.javatogpu.runtime;

import java.util.ArrayList;
import java.util.List;

public final class GpuLauncherNaming {

    private GpuLauncherNaming() {
    }

    public static String launcherClassName(Class<?> ownerClass, String methodName) {
        String packageName = ownerClass.getPackageName();
        String generatedPackage = packageName.isEmpty() ? "generated" : packageName + ".generated";

        List<String> ownerNames = new ArrayList<>();
        Class<?> current = ownerClass;
        while (current != null) {
            ownerNames.add(0, current.getSimpleName());
            current = current.getEnclosingClass();
        }
        ownerNames.add(methodName);
        ownerNames.add("GpuLauncher");
        return generatedPackage + "." + String.join("_", ownerNames);
    }

    public static String launcherInternalName(String ownerInternalName, String methodName) {
        int packageSeparator = ownerInternalName.lastIndexOf('/');
        String packageInternalName = packageSeparator >= 0 ? ownerInternalName.substring(0, packageSeparator) : "";
        String ownerSimplePath = packageSeparator >= 0 ? ownerInternalName.substring(packageSeparator + 1) : ownerInternalName;
        String generatedPackage = packageInternalName.isEmpty() ? "generated" : packageInternalName + "/generated";
        String flattenedOwnerName = ownerSimplePath.replace('$', '_');
        return generatedPackage + "/" + flattenedOwnerName + "_" + methodName + "_GpuLauncher";
    }
}
