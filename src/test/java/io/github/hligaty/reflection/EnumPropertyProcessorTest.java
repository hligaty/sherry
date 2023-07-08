package io.github.hligaty.reflection;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class EnumPropertyProcessorTest {

    @Test
    public void test() throws NotFoundException, CannotCompileException, IOException, ClassNotFoundException {
        new EnumPropertyProcessor().process();
        /*
        为什么主线程创建的对象在其他线程序列化会出现问题呢?
        但 process 方法与序列化对象不在同一类中就不会出现上述问题(),
        打包后作为依赖使用也不会有问题
         */
        new EnumPropertyProcessorTestDelegate().test();
    }

}
