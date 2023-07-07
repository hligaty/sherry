package io.github.hligaty.reflection;


import io.swagger.v3.oas.annotations.media.Schema;

public class Application {

    @EnumProperty(Status.class)
    @Schema
    public Integer status;

    @EnumProperty(Platform.class)
    @Schema
    private Integer platform;

    private User user;

    public static class User {
        
        @EnumProperty(Sex.class)
        @Schema
        private Integer sex;

        public Integer getSex() {
            return sex;
        }

        public void setSex(Integer sex) {
            this.sex = sex;
        }
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getPlatform() {
        return platform;
    }

    public void setPlatform(Integer platform) {
        this.platform = platform;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
