package io.github.hligaty.reflection;

import com.google.common.io.Resources;
import javassist.ClassPool;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.List;

import static io.github.hligaty.reflection.EnumPropertyResourceNeighborProcessor.ENUM_PROPERTY_CLASS_RESOURCE_LOCATION;

public class EnumPropertyAgent {
    static List<String> classnames;

    public static void premain(String agentArgs, Instrumentation inst) throws IOException {
        inst.addTransformer(new EnumPropertyClassTransformer());
        URL resource = Resources.getResource(ENUM_PROPERTY_CLASS_RESOURCE_LOCATION);
        classnames = Resources.readLines(resource, StandardCharsets.UTF_8);
        ClassPool classPool = ClassPool.getDefault();
    }

    static class EnumPropertyClassTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            className = className.replace("/", ".");
            return null;
        }
    }
}
