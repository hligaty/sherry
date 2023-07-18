package io.github.hligaty.reflection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 为数值类型的字段生成枚举名字的get方法用于序列化
 *
 * @author hligaty
 * @date 2023/07/06
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumProperty {

    Class<? extends Enum<?>> value();
}
