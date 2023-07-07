package io.github.hligaty.reflection;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.PropertyPreFilter;
import com.google.common.base.CaseFormat;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 使用序列化框架提供的入口增加枚举名字字段, 优点是灵活简单, 缺点是反射慢一些
 * @author hligaty
 * @date 2023/07/06
 */
public class EnumPropertyPreFilter implements PropertyPreFilter {

    @Override
    public boolean process(JSONWriter writer, Object source, String name) {
        Field field;
        try {
            Class<?> clazz = source.getClass();
            field = clazz.getDeclaredField(name);
            Method getter = clazz.getDeclaredMethod("get" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name));
            Object value = getter.invoke(source);
            if (value == null) {
                return true;
            }
            EnumProperty enumProperty = field.getAnnotation(EnumProperty.class);
            if (enumProperty == null) {
                return true;
            }
            for (Enum<?> enumConstant : enumProperty.value().getEnumConstants()) {
                if (Objects.equals(enumConstant.ordinal(), value)) {
                    writer.writeName(name + "Name");
                    writer.writeColon();
                    writer.writeString(enumConstant.name());
                    return true;
                }
            }
            throw new IllegalArgumentException("enum ordinal not present, field: " + name + ", value:" + value);
        } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException ignored) {
            return false;
        }
    }
}
