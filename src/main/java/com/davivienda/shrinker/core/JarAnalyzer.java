package com.davivienda.shrinker.core;

import org.objectweb.asm.*;

import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
