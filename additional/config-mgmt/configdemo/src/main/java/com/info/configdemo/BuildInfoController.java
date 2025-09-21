package com.info.configdemo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
//@AllArgsConstructor
/**
 * The @RefreshScope annotation is used to indicate that the bean should be
 * refreshed when the application receives a refresh event. This is useful for
 * applications that support dynamic configuration updates.
 */
@RefreshScope
public class BuildInfoController {

    @Value("${build.id:default}")
    private String buildId;

    @Value("${build.version:default}")
    private String buildVersion;

    @Value("${build.name:default}")
    private String buildName;

//    private BuildInfo buildInfo;

    @GetMapping("/build-info")
    public String getBuildInfo() {
        return "Build ID: " + buildId + ", Version: " + buildVersion + ", Name: " + buildName;
    }

//    @GetMapping("/build-info")
//    public String getBuildInfo() {
//        return "Build ID: " + buildInfo.getId() + ", Version: " + buildInfo.getVersion() + ", Name: " + buildInfo.getName();
//    }
}
