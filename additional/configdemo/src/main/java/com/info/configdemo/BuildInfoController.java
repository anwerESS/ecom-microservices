package com.info.configdemo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BuildInfoController {

//    @Value("${OS:default}")
    @Value("${build.id}")
    private String buildId;

//    @Value("${PROCESSOR_LEVEL:default}")
    @Value("${build.version}")
    private String buildVersion;

//    @Value("${JAVA_HOME:default}")
    @Value("${build.name}")
    private String buildName;


    // use configurationProperties way
    @Autowired
    private BuildInfo buildInfo;

    @GetMapping("/build-info")
    public String getBuildInfo() {
//        return "Build ID: " + buildId + ", Version: " + buildVersion + ", Name: " + buildName;
        return buildInfo.toString();
    }
}




//Precedence Order
//
//→ Command-line arguments (--build.id=12345)
//
//→ Java system properties (-Dbuild.id=12345)
//
//→ OS environment variables (export BUILD_ID=12345)
//
//→ application.properties or application.yml
//
//→ Spring Cloud Config Server (if used)
//
//→ Default values inside the application code