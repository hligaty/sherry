package io.github.hligaty.reflection;

import javassist.CannotCompileException;
import javassist.NotFoundException;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.List;

/**
 * 等同 EnumPropertyProcessor, 使用 JavaAgent 方式
 *
 * @author hligaty
 * @date 2023/07/06
 */
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
                try {
                    return EnumPropertyProcessor.doProcess(className, true);
                } catch (NotFoundException | CannotCompileException | IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}
