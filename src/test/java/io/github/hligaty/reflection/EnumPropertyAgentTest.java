package io.github.hligaty.reflection;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import javassist.CannotCompileException;
import javassist.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class EnumPropertyAgentTest {

    @Test
    public void test() throws NotFoundException, CannotCompileException, IOException, ClassNotFoundException {
        new EnumPropertyProcessor().process();
        Application application = new Application();
        application.setStatus(1);
        application.setPlatform(0);
        Application.User user = new Application.User();
        user.setSex(1);
        application.setUser(user);
        Assertions.assertEquals(
                """
                        {
                        	"platform":0,
                        	"platformName":"MOBILE",
                        	"status":1,
                        	"statusName":"CLOSE",
                        	"user":{
                        		"sex":1,
                        		"sexName":"FEMALE"
                        	}
                        }""",
                JSON.toJSONString(application, JSONWriter.Feature.PrettyFormat)
        );
    }
}
