package com.jarshrinker.core;

import org.objectweb.asm.*;

import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class JarAnalyzer {

    private final Map<String, Set<String>> dependencies = new HashMap<>();
    private final Set<String> allClasses = new HashSet<>();
    private final Map<String, byte[]> classBytes = new HashMap<>();

    public void loadJar(JarFile jar) throws Exception {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.endsWith(".class")) {
                String className = name.replace('/', '.').substring(0, name.length() - 6);
                allClasses.add(className);
                byte[] bytes = readBytes(jar, entry);
                classBytes.put(className, bytes);
            }
        }
        for (Map.Entry<String, byte[]> e : classBytes.entrySet()) {
            analyzeClass(e.getKey(), e.getValue());
        }
    }

    private void analyzeClass(String className, byte[] bytes) {
        Set<String> refs = new HashSet<>();
        ClassReader reader = new ClassReader(bytes);
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                addRef(superName);
                if (interfaces != null) for (String i : interfaces) addRef(i);
            }
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                addTypeDesc(desc);
                return null;
            }
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                addTypeDesc(desc);
                if (exceptions != null) for (String e : exceptions) addRef(e);
                return new MethodVisitor(Opcodes.ASM9) {
                    public void visitTypeInsn(int opcode, String type) { addRef(type); }
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        addRef(owner);
                        addTypeDesc(desc);
                    }
                    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                        addRef(owner);
                        addTypeDesc(desc);
                    }
                    public void visitLdcInsn(Object value) {
                        if (value instanceof Type) addRef(((Type) value).getInternalName());
                    }
                    public void visitMultiANewArrayInsn(String desc, int dims) { addTypeDesc(desc); }
                    public void visitLocalVariable(String n, String desc, String sig, Label start, Label end, int index) {
                        addTypeDesc(desc);
                    }
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        addTypeDesc(desc);
                        return null;
                    }
                };
            }
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                addTypeDesc(desc);
                return null;
            }
            void addRef(String internalName) {
                if (internalName == null) return;
                String dotted = internalName.replace('/', '.');
                if (dotted.startsWith("java.") || dotted.startsWith("javax.") || dotted.startsWith("sun.") || dotted.startsWith("jdk."))
                    return;
                refs.add(dotted);
            }
            void addTypeDesc(String desc) {
                if (desc == null) return;
                Type t = Type.getType(desc);
                if (t.getSort() == Type.OBJECT || t.getSort() == Type.ARRAY) {
                    String internal = t.getInternalName();
                    addRef(internal);
                }
            }
        }, ClassReader.SKIP_CODE);
        dependencies.put(className, refs);
    }

    public Set<String> findReachableClasses(Set<String> entryPoints) {
        Set<String> reachable = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        for (String ep : entryPoints) {
            if (allClasses.contains(ep)) {
                queue.add(ep);
                reachable.add(ep);
            }
        }
        while (!queue.isEmpty()) {
            String current = queue.poll();
            Set<String> refs = dependencies.getOrDefault(current, Collections.emptySet());
            for (String ref : refs) {
                if (!reachable.contains(ref) && allClasses.contains(ref)) {
                    reachable.add(ref);
                    queue.add(ref);
                }
            }
        }
        return reachable;
    }

    public Set<String> detectEntryPoints() {
        Set<String> entryPoints = new HashSet<>();
        Set<String> appPackages = new HashSet<>();

        for (String className : allClasses) {
            byte[] bytes = classBytes.get(className);
            if (bytes == null) continue;
            detectFromClass(className, bytes, entryPoints, appPackages);
        }

        for (String pkg : appPackages) {
            for (String cls : allClasses) {
                if (cls.startsWith(pkg)) entryPoints.add(cls);
            }
        }

        return entryPoints;
    }

    public Set<String> detectEntryPointsFromManifest(Manifest manifest) {
        Set<String> result = new HashSet<>();
        if (manifest == null) return result;
        String mainClass = manifest.getMainAttributes().getValue("Main-Class");
        if (mainClass != null && !mainClass.isEmpty()) {
            mainClass = mainClass.trim().replace('/', '.');
            if (allClasses.contains(mainClass)) result.add(mainClass);
        }
        return result;
    }

    private static boolean isTestNgAnnotation(String desc) {
        if (!desc.startsWith("Lorg/testng/annotations/")) return false;
        String simple = desc.substring(24, desc.length() - 1);
        return simple.equals("Test") || simple.equals("BeforeSuite") || simple.equals("AfterSuite")
                || simple.equals("BeforeClass") || simple.equals("AfterClass")
                || simple.equals("BeforeMethod") || simple.equals("AfterMethod")
                || simple.equals("BeforeTest") || simple.equals("AfterTest")
                || simple.equals("BeforeGroups") || simple.equals("AfterGroups");
    }

    private void detectFromClass(String className, byte[] bytes, Set<String> entryPoints, Set<String> appPackages) {
        final boolean[] isEntry = {false};

        ClassReader reader = new ClassReader(bytes);
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            public void visit(int version, int access, String name, String signature, String sn, String[] ifs) {
            }
            public MethodVisitor visitMethod(int access, String mName, String desc, String signature, String[] exceptions) {
                boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
                boolean isPublic = (access & Opcodes.ACC_PUBLIC) != 0;
                if (isPublic && isStatic && mName.equals("main") && desc.equals("([Ljava/lang/String;)V")) {
                    isEntry[0] = true;
                }
                return new MethodVisitor(Opcodes.ASM9) {
                    public AnnotationVisitor visitAnnotation(String aDesc, boolean visible) {
                        if (isTestNgAnnotation(aDesc) || aDesc.equals("Lorg/junit/Test;")) {
                            isEntry[0] = true;
                        }
                        return null;
                    }
                };
            }
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (isTestNgAnnotation(desc) || desc.equals("Lorg/junit/Test;")) {
                    isEntry[0] = true;
                }
                return null;
            }
        }, ClassReader.SKIP_CODE);

        if (isEntry[0]) {
            entryPoints.add(className);
            appPackages.add(className.contains(".") ? className.substring(0, className.lastIndexOf('.')) : "");
        }
    }

    public String[] getPackages() {
        Set<String> pkgs = new TreeSet<>();
        for (String cls : allClasses) {
            int dot = cls.lastIndexOf('.');
            if (dot > 0) pkgs.add(cls.substring(0, dot));
            else pkgs.add("(default)");
        }
        return pkgs.toArray(new String[0]);
    }

    public Set<String> getReachablePackages(Set<String> entryPoints) {
        Set<String> reachable = findReachableClasses(entryPoints);
        Set<String> pkgs = new TreeSet<>();
        for (String cls : reachable) {
            int dot = cls.lastIndexOf('.');
            if (dot > 0) pkgs.add(cls.substring(0, dot));
            else pkgs.add("(default)");
        }
        return pkgs;
    }

    public String[] getTopLevelPackages() {
        Set<String> pkgs = new TreeSet<>();
        for (String cls : allClasses) {
            int dot = cls.indexOf('.');
            if (dot > 0) {
                pkgs.add(cls.substring(0, cls.indexOf('.', dot + 1) > 0 ? cls.indexOf('.', dot + 1) : cls.length()));
            }
        }
        return pkgs.toArray(new String[0]);
    }

    public Set<String> getAllClasses() { return allClasses; }
    public Map<String, byte[]> getClassBytes() { return classBytes; }

    private byte[] readBytes(JarFile jar, JarEntry entry) throws Exception {
        byte[] buf = new byte[(int) entry.getSize()];
        try (InputStream in = jar.getInputStream(entry)) {
            int off = 0;
            while (off < buf.length) {
                int read = in.read(buf, off, buf.length - off);
                if (read < 0) break;
                off += read;
            }
        }
        return buf;
    }
}
