package io.github.hligaty.reflection;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import org.junit.jupiter.api.Assertions;

class EnumPropertyProcessorTestDelegate {

    public void test() {
        Application application = new Application();
        application.setStatus(1);
        application.setPlatform(0);
        Application.User user = new Application.User();
        user.setSex(1);
        application.setUser(user);
        Assertions.assertEquals(
                JSON.toJSONString(application, JSONWriter.Feature.PrettyFormat),
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
                        }"""
        );
    }
    
}
