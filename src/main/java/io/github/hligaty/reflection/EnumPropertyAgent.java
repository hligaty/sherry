package io.github.hligaty.reflection;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.List;

public class EnumPropertyAgent {
    private static List<String> classnames;

    public static void premain(String agentArgs, Instrumentation inst) throws IOException {
        classnames = EnumPropertyResourceNeighborProcessor.getClassnames();
        inst.addTransformer(new EnumPropertyClassTransformer());
    }

    static class EnumPropertyClassTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            className = className.replace("/", ".");
            if (classnames.contains(className)) {
                ClassPool classPool = ClassPool.getDefault();
                CtClass ctClass = null;
                try {
                    ctClass = classPool.get(className);
                    EnumPropertyProcessor.doProcess(ctClass);
                    return ctClass.toBytecode();
                } catch (NotFoundException | CannotCompileException | IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if (ctClass != null) {
                        ctClass.detach();
                    }
                }
            }
            return null;
        }
    }
}
