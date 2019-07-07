package com.tyrannos.data.collector;

import com.tyrannos.data.collector.collectors.CollectFromStarHanger;
import com.tyrannos.data.collector.rest.RestService;
import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.util.concurrent.TimeUnit;

@EnableAutoConfiguration
@SpringBootApplication
public class DataCollectorMain extends SpringBootServletInitializer {

    final static Logger LOG = Logger.getLogger(DataCollectorMain.class);
    private static final String CONTEXT_XML = "classpath:/application-context.xml";

    public static void main(String[] args) throws Exception {

        SpringApplication application = new SpringApplication();
        application.setWebApplicationType(WebApplicationType.REACTIVE);
        application.run(DataCollectorMain.class, args);
//        RestService restService = new RestService();
//        restService.getNewSources();

        CollectFromStarHanger collectFromStarHanger = new CollectFromStarHanger();
        try {
            while(true) {
                collectFromStarHanger.getStarHangerResources();
                Thread.sleep(TimeUnit.MINUTES.toMillis(30));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



    }


}