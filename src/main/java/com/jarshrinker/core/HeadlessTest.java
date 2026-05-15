package com.jarshrinker.core;

import java.io.File;
import java.util.*;
import java.util.jar.JarFile;

public class HeadlessTest {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Uso: HeadlessTest <input.jar> [output.jar] [packages]");
            return;
        }

        File input = new File(args[0]);
        File output = args.length > 1 ? new File(args[1]) : new File(input.getParent(), input.getName().replace(".jar", "-min.jar"));

        String pkg = args.length > 2 ? args[2] : "";

        System.out.println("JAR: " + input.getName() + " (" + input.length() / 1024 + " KB)");
        System.out.println("Analizando...");

        JarAnalyzer analyzer = new JarAnalyzer();
        try (JarFile jf = new JarFile(input)) {
            analyzer.loadJar(jf);
        }

        System.out.println("Clases totales: " + analyzer.getAllClasses().size());

        Set<String> entryPoints = new HashSet<>();
        if (!pkg.isEmpty()) {
            for (String cls : analyzer.getAllClasses()) {
                for (String p : pkg.split(",")) {
                    if (cls.startsWith(p.trim())) entryPoints.add(cls);
                }
            }
        } else {
            entryPoints.addAll(analyzer.getAllClasses());
        }

        System.out.println("Entry points: " + entryPoints.size());
        Set<String> reachable = analyzer.findReachableClasses(entryPoints);
        System.out.println("Clases alcanzables: " + reachable.size());

        JarMinimizer.MinimizeResult r = JarMinimizer.minimize(input, output, reachable,
                analyzer.getAllClasses(), analyzer.getClassBytes());

        File outf = output;
        System.out.println("\nRESULTADO:");
        System.out.println("  Original: " + input.length() / 1024 + " KB (" + r.totalClasses + " clases)");
        System.out.println("  Optimizado: " + (input.length() - r.savedBytes) / 1024 + " KB (" + r.keptClasses + " clases)");
        System.out.println("  Eliminadas: " + r.removedClasses + " clases");
        System.out.println("  Ahorro: " + r.savedBytes / 1024 + " KB (" + r.savedPercent + "%)");
        System.out.println("  Archivo: " + outf.getAbsolutePath());
    }
}
