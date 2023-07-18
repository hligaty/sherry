package io.github.hligaty.reflection;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class EnumPropertyAgentTest {

    @Test
    public void test() {
        // use -javaagent:... run
        Application application = new Application();
        application.setPlatform(0);
        Application.User user = new Application.User();
        user.setSex(1);
        application.setUser(user);
        Assertions.assertEquals(
                """
                        {
                        	"platform":0,
                        	"platformName":"MOBILE",
                        	"user":{
                        		"sex":1,
                        		"sexName":"FEMALE"
                        	}
                        }""",
                JSON.toJSONString(application, JSONWriter.Feature.PrettyFormat)
        );
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<Application>> constraintViolations =
                    validator.validate(application);
            Assertions.assertEquals(1, constraintViolations.size());
            String description = Status.class.getSimpleName() + Stream.of(Status.class.getEnumConstants())
                    .map(enumConstant -> enumConstant.ordinal() + "-" + enumConstant.name())
                    .collect(Collectors.joining(", ", "(", ")"));
            Assertions.assertEquals(description + " must not be null", constraintViolations.iterator().next().getMessage());
        }
    }
    
}
