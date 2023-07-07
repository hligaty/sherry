package io.github.hligaty.reflection;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class EnumPropertyProcessorTest {
    
    @Test
    public void test() throws NotFoundException, CannotCompileException, IOException, ClassNotFoundException {
        EnumPropertyProcessor enumPropertyProcessor = new EnumPropertyProcessor();
        enumPropertyProcessor.process();
        new EnumPropertyProcessorTestDelegate().test();
    }
    
}
