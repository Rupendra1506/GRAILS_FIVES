package grails_fives

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.EnvironmentAware
import org.springframework.core.env.Environment
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration


class Application extends GrailsAutoConfiguration implements EnvironmentAware {

    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    void setEnvironment(Environment environment) {
        String configPath = System.properties["local.config.location"] ?: "app-config.yml"
        Resource resourceConfig = new FileSystemResource(configPath)
        YamlPropertiesFactoryBean ypfb = new YamlPropertiesFactoryBean()
        ypfb.setResources([ resourceConfig ] as Resource[])
        ypfb.afterPropertiesSet()
        Properties properties = ypfb.getObject()
        environment.propertySources.addFirst(new PropertiesPropertySource("local.config.location", properties))
    }

}
