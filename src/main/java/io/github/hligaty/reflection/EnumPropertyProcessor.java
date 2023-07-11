package io.github.hligaty.reflection;

import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.Schema;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.hligaty.reflection.EnumPropertyResourceNeighborProcessor.NEIGHBOR_CLASS_NAME;

/**
 * 枚举字段处理器
 * 1. 枚举序列化get方法生成处理类
 * 2. Swagger Schema 注解生成 description 描述
 *
 * @author hligaty
 * @date 2023/07/06
 */
public class EnumPropertyProcessor {
    /**
     * get方法名后缀
     */
    private static final String SUFFIX = "Name";
    /**
     * Swagger Schema
     */
    private static final String SWAGGER_SCHEMA_CLASS_NAME = "io.swagger.v3.oas.annotations.media.Schema";
    private static final String SWAGGER_SCHEMA_DESCRIPTION_METHOD_NAME = "description";

    public void process() throws NotFoundException, ClassNotFoundException, CannotCompileException, IOException {
        List<String> classnames = EnumPropertyResourceNeighborProcessor.getClassnames();
        ClassPool classPool = ClassPool.getDefault();
        for (String classname : classnames) {
            CtClass ctClass = classPool.get(classname);
            try {
                doProcess(ctClass);
                Class<?> neighborClass = Class.forName(ctClass.getPackageName() + "." + NEIGHBOR_CLASS_NAME);
                ctClass.toClass(neighborClass);
            } finally {
                ctClass.detach();
            }
        }
    }

    static void doProcess(CtClass ctClass) throws ClassNotFoundException, CannotCompileException {
        for (CtField ctField : ctClass.getDeclaredFields()) {
            EnumProperty enumProperty = (EnumProperty) ctField.getAnnotation(EnumProperty.class);
            if (enumProperty != null) {
                Class<? extends Enum<?>> enumClass = enumProperty.value();
                generateSwaggerSchemaDescription(ctClass, ctField, enumClass);
                generateEnumNameGetter(ctClass, ctField, enumClass);
            }
        }
    }

    private static void generateSwaggerSchemaDescription(CtClass ctClass, CtField ctField, Class<? extends Enum<?>> enumClass) throws ClassNotFoundException {
        if (ctField.hasAnnotation(SWAGGER_SCHEMA_CLASS_NAME)
            && ctField.getAnnotation(Schema.class) instanceof Schema schema
            && Strings.isNullOrEmpty(schema.description())) {
            // description sample: EnumSimpleName(ordinal0-name0, ordinal1-name1, ...)
            String description = enumClass.getSimpleName() + Stream.of(enumClass.getEnumConstants())
                    .map(enumConstant -> enumConstant.ordinal() + "-" + enumConstant.name())
                    .collect(Collectors.joining(", ", "(", ")"));
            ClassFile classFile = ctClass.getClassFile();
            ConstPool constPool = classFile.getConstPool();
            AnnotationsAttribute attribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
            Annotation annotation = new Annotation(SWAGGER_SCHEMA_CLASS_NAME, constPool);
            annotation.addMemberValue(SWAGGER_SCHEMA_DESCRIPTION_METHOD_NAME, new StringMemberValue(description, constPool));
            attribute.addAnnotation(annotation);
            ctField.getFieldInfo().addAttribute(attribute);
        }
    }

    private static void generateEnumNameGetter(CtClass ctClass, CtField ctField, Class<? extends Enum<?>> enumClass) throws CannotCompileException {
        String fieldName = ctField.getName() + SUFFIX;
        CtMethod method = CtNewMethod.make(
                """
                        public String get%s() {
                            return %s.getEnumName(%s, %s.class, "%s");
                        }""".formatted(
                        CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName),
                        EnumPropertyProcessor.class.getName(),
                        ctField.getName(),
                        enumClass.getName(),
                        ctField.getName()
                ),
                ctClass
        );
        ctClass.addMethod(method);
    }

    public static String getEnumName(Integer value, Class<? extends Enum<?>> enumClass, String fieldName) {
        if (value == null) {
            return null;
        }
        for (Enum<?> constant : enumClass.getEnumConstants()) {
            if (constant.ordinal() == value) {
                return constant.name();
            }
        }
        throw new IllegalArgumentException("enum ordinal not present, field: " + fieldName + ", value:" + value);
    }
}
