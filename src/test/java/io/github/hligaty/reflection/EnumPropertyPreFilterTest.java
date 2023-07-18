package io.github.hligaty.reflection;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class EnumPropertyPreFilterTest {

    @Test
    public void test() {
        EnumPropertyPreFilter enumPropertyPreFilter = new EnumPropertyPreFilter();
        Application application = new Application();
        application.setStatus(1);
        application.setPlatform(0);
        Application.User user = new Application.User();
        user.setSex(1);
        application.setUser(user);
        Assertions.assertEquals(
                """
                        {
                        	"platformName":"MOBILE",
                        	"platform":0,
                        	"statusName":"OPEN",
                        	"status":1,
                        	"user":{
                        		"sexName":"FEMALE",
                        		"sex":1
                        	}
                        }""",
                JSON.toJSONString(application, enumPropertyPreFilter, JSONWriter.Feature.PrettyFormat)
        );
    }

}
