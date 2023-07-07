package io.github.hligaty.reflection;

import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

/**
 * 生成 META-INF/sherry/io.github.hligaty.reflection.EnumPropertyNeighborResourceProcessor.imports 用于运行时修改类
 * 在字段上有 {@link EnumProperty} 注解的类所在包生成一个 EnumPropertyNeighbor 类, 用于 javassit
 * @author hligaty
 * @date 2023/07/06
 * @see <a href="https://github.com/jboss-javassist/javassist/issues/369#issuecomment-1617207353">javassist cannot work on jdk 16</a>
 */
@AutoService(Processor.class)
public class EnumPropertyResourceNeighborProcessor extends AbstractProcessor {
    static final String NEIGHBOR_CLASS_NAME = "EnumPropertyNeighbor";
    static final String ENUM_PROPERTY_CLASS_RESOURCE_LOCATION = "META-INF/sherry/" + EnumPropertyResourceNeighborProcessor.class.getName() + ".imports";
    private final HashSet<String> classSet = HashSet.newHashSet(0);

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(EnumProperty.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Filer filer = processingEnv.getFiler();
        if (roundEnv.processingOver()) {
            // 生成资源文件, 用于运行期获取到需要修改的类
            try (Writer resourceWriter = filer.createResource(StandardLocation.CLASS_OUTPUT, "", ENUM_PROPERTY_CLASS_RESOURCE_LOCATION).openWriter()) {
                resourceWriter.write(String.join("\n", classSet));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            HashSet<String> packageSet = HashSet.newHashSet(0);
            for (Element element : roundEnv.getElementsAnnotatedWith(EnumProperty.class)) {
                if (!(element.getEnclosingElement() instanceof TypeElement typeElement)) {
                    return true;
                }
                ClassSymbol classSymbol = getClassSymbol(typeElement);
                if (classSet.add(classSymbol.flatName) && packageSet.add(classSymbol.packageName)) {
                    // 是正常的类, 添加到资源文件中(同一个类多个字段只需要一次)成功, 且这个包下没创建过相邻类
                    try (Writer sourceWriter = filer.createSourceFile(classSymbol.packageName + "." + NEIGHBOR_CLASS_NAME).openWriter()) {
                        sourceWriter.write("""
                                package %s;
                                                                
                                public class %s {
                                }""".formatted(classSymbol.packageName, NEIGHBOR_CLASS_NAME));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return true;
    }

    /**
     * 获取类的包名和类名, 类名与内部类名使用 "$" 分割, 而非 "."
     * @param element 类元素
     * @return 包名和类名
     */
    private ClassSymbol getClassSymbol(TypeElement element) {
        if (element.getEnclosingElement() instanceof PackageElement packageElement) {
            return new ClassSymbol(packageElement.getQualifiedName().toString(), element.getQualifiedName().toString());
        }
        String flatName = element.getSimpleName().toString();
        while (true) {
            if (element.getEnclosingElement() instanceof TypeElement typeElement) {
                flatName = "%s$%s".formatted(typeElement.getSimpleName(), flatName);
                element = typeElement;
            }
            if (element.getEnclosingElement() instanceof PackageElement packageElement) {
                String packageName = packageElement.getQualifiedName().toString();
                return new ClassSymbol(packageName, packageName + "." + flatName);
            }
        }
    }
    
    record ClassSymbol(String packageName, String flatName) {
        
    }
}
