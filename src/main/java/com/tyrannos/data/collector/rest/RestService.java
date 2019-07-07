package com.tyrannos.data.collector.rest;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tyrannos.data.collector.ShipData;
import com.tyrannos.data.collector.json.ContextModel;
import com.tyrannos.data.collector.json.Ship;
import com.tyrannos.data.collector.util.Utilities;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class RestService {

    private static final Logger LOG = Logger.getLogger(RestService.class);
    final String dir = "C:\\Users\\jerod\\workspace\\EvilTyrannosDataCollector\\logs\\jsonFiles\\";

    @RequestMapping(value = "/test")
    public
    @ResponseBody
    String test() {
        String string = "hello";
        return string;
    }

    @RequestMapping(value = "/api")
    public String getRoot() {
        URLConnection con = null;
        String body = null;
        URL url = null;
        try {
            url = new URL("http://localhost:8080/actuator/mappings");
            con = url.openConnection();
            InputStream in = con.getInputStream();
            String encoding = con.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            body = IOUtils.toString(in, encoding);


            Gson gson = new Gson();
            ContextModel contextModel = gson.fromJson(body, ContextModel.class);
            LOG.info(body);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        Map<String, String> map = new HashMap<>();
//
//        for (File file : newFileArray) {
//            String json = null;
//            try {
//                json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
//
//                Gson gson = new Gson(); // Or use new GsonBuilder().create();
//                Ship ship = gson.fromJson(json, Ship.class);
//
//                map.put(ship.name, ship.status);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        return map;
        return "";
    }

    @RequestMapping(value = "/api/ships")
    public String getAttachmentList() {

        Set<String> listOfFiles = listFilesUsingJavaIO(dir);

        StringBuilder sb = new StringBuilder();

        for (String string : listOfFiles) {
            sb.append(string).append("<br>");
        }

        return sb.toString();
    }

    @GetMapping("/api/ships/{ship}")
    public
    @ResponseBody
    String getShip(@PathVariable String ship) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<String> fileContents = null;
        try {
            fileContents = com.google.common.io.Files.readLines(new File(dir + ship), Charset.defaultCharset());
        } catch (IOException e) {

        }

        StringBuilder sb = new StringBuilder();

        for (String string : fileContents) {
            sb.append(string);//.append("<br>");
        }

        String json = sb.toString().replace("[", "").replace("]", "");
        return json;
    }

    public Set<String> listFilesUsingJavaIO(String dir) {
        return Stream.of(new File(dir).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
    }

    @RequestMapping(value = "/getNewSources", method = RequestMethod.GET, produces = "application/json")
    public String getNewSources() {

        RetrieveSourcesFromRSI retrieveSourcesFromRSI = new RetrieveSourcesFromRSI();

        return retrieveSourcesFromRSI.getSources();
    }





}