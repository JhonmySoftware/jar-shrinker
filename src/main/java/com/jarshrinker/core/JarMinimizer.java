package com.jarshrinker.core;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;

public class JarMinimizer {

    public static MinimizeResult minimize(
            File inputJar,
            File outputJar,
            Set<String> reachableClasses,
            Set<String> allClasses,
            Map<String, byte[]> classBytes
    ) throws Exception {
        int totalClasses = allClasses.size();
        int keptClasses = 0;
        long removedBytes = 0;
        long totalBytes = 0;

        try (JarFile original = new JarFile(inputJar);
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJar))) {

            jos.setLevel(Deflater.DEFAULT_COMPRESSION);
            Enumeration<JarEntry> entries = original.entries();
            Set<String> copied = new HashSet<>();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                boolean isClass = name.endsWith(".class");
                String className = isClass ? name.replace('/', '.').substring(0, name.length() - 6) : null;
                long entrySize = Math.max(0, entry.getSize());

                if (isClass && !reachableClasses.contains(className)) {
                    removedBytes += entrySize;
                    totalBytes += entrySize;
                    continue;
                }

                totalBytes += entrySize;
                if (isClass) keptClasses++;

                JarEntry newEntry = new JarEntry(name);
                newEntry.setTime(entry.getTime());
                newEntry.setMethod(ZipEntry.DEFLATED);
                jos.putNextEntry(newEntry);

                byte[] data;
                if (isClass && classBytes.containsKey(className)) {
                    data = classBytes.get(className);
                } else {
                    data = readEntrySafe(original, entry);
                }
                jos.write(data);
                jos.closeEntry();
                copied.add(name);
            }

            copyMetaInf(inputJar, jos, copied);
        }

        long savedBytes = removedBytes;
        int savedPercent = totalBytes > 0 ? (int) (savedBytes * 100 / totalBytes) : 0;

        return new MinimizeResult(totalClasses, keptClasses, totalClasses - keptClasses, savedBytes, savedPercent);
    }

    private static byte[] readEntrySafe(JarFile jar, JarEntry entry) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(4096);
        try (InputStream in = jar.getInputStream(entry)) {
            byte[] tmp = new byte[8192];
            int read;
            while ((read = in.read(tmp)) != -1) {
                buffer.write(tmp, 0, read);
            }
        }
        return buffer.toByteArray();
    }

    private static void copyMetaInf(File inputJar, JarOutputStream jos, Set<String> copied) throws Exception {
        try (JarFile original = new JarFile(inputJar)) {
            Enumeration<JarEntry> entries = original.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (copied.contains(name)) continue;
                if (name.startsWith("META-INF/") && !name.endsWith(".class")) {
                    JarEntry newEntry = new JarEntry(name);
                    newEntry.setTime(entry.getTime());
                    newEntry.setMethod(ZipEntry.DEFLATED);
                    jos.putNextEntry(newEntry);
                    jos.write(readEntrySafe(original, entry));
                    jos.closeEntry();
                }
            }
        }
    }

    public static class MinimizeResult {
        public final int totalClasses, keptClasses, removedClasses;
        public final long savedBytes, savedPercent;

        public MinimizeResult(int total, int kept, int removed, long savedBytes, int savedPercent) {
            this.totalClasses = total;
            this.keptClasses = kept;
            this.removedClasses = removed;
            this.savedBytes = savedBytes;
            this.savedPercent = savedPercent;
        }
    }
}
