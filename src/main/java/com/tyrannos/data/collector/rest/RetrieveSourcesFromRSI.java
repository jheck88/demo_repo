package com.tyrannos.data.collector.rest;

import com.google.gson.Gson;
import com.tyrannos.data.collector.collectors.ShipData;
import com.tyrannos.data.collector.json_model.Ship;
import com.tyrannos.data.collector.util.DBManager;
import com.tyrannos.data.collector.util.Utilities;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RetrieveSourcesFromRSI {

    private static final Logger LOG = Logger.getLogger(RetrieveSourcesFromRSI.class);
    final String URL_BASE = "https://robertsspaceindustries.com";
    final String dir = "C:\\Users\\jerod\\workspace\\EvilTyrannosDataCollector\\logs\\jsonFiles\\";

    DBManager dbManager = new DBManager();

    public RetrieveSourcesFromRSI() {

    }

    public String getSources() {
        dbManager.runSqlStatement();
        List<File> baseFileArray = getBaseFileList(dir);

        runSelenium();

        List<File> newFileArray = getBaseFileList(dir);


        StringBuilder stringBuilder = new StringBuilder();
        if (baseFileArray.size() == newFileArray.size()) {
            try {
                Map<String, List<String>> shipMap = getShipStatusMap(newFileArray);
                int counter = 0;
                int totalCounter = 0;
                stringBuilder.append("{\"ships\": [");
                for (Map.Entry<String, List<String>> entrySet : shipMap.entrySet()) {
                    if (entrySet.getValue().get(0).equals("Flight Ready")) {
                        stringBuilder.append("{\"ship \" : \"" + entrySet.getKey() + "\",");
                        stringBuilder.append("\"type\" : \"" + entrySet.getValue().get(2) + "\",");
                        stringBuilder.append("\"price\" : \"" + entrySet.getValue().get(1) + "\",");
                        stringBuilder.append("\"time\" : \"" + entrySet.getValue().get(3) + "\"}");
                        counter = counter + 1;
                        stringBuilder.append(",");
                    }
                    totalCounter = totalCounter + 1;
                }
                String result = stringBuilder.toString();
                result = StringUtils.substring(result, 0, result.length() - 1);
                result = result + "]}";

                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "Error";
        } else {
            try {
                List<File> list = new ArrayList<File>(CollectionUtils.disjunction(newFileArray, baseFileArray));
                Map<String, List<String>> shipMap = getShipStatusMap(list);
                int counter = 0;
                for (Map.Entry<String, List<String>> entrySet : shipMap.entrySet()) {
                    if (entrySet.getValue().get(0).equals("Flight Ready")) {
                        stringBuilder.append("{\"ships\": [");
                        stringBuilder.append("{\"ship \" : \"" + entrySet.getKey() + "\",");
                        stringBuilder.append("\"type\" : \"" + entrySet.getValue().get(2) + "\",");
                        stringBuilder.append("\"price\" : \"" + entrySet.getValue().get(1) + "\", ");
                        stringBuilder.append("\"time\" : \"" + entrySet.getValue().get(3) + "\"}");
                        stringBuilder.append(",");
                    }

                    counter = counter + 1;
                }
                String total = " New Flyable Ships: " + counter + "<br>Flight Ready<br>" + stringBuilder.toString();

//                stringBuilder.append("Additional Flyable Ships");
                Map<String, List<String>> newShipMap = getShipStatusMap(newFileArray);
                counter = 0;
                int totalCounter = 0;
                for (Map.Entry<String, List<String>> entrySet : newShipMap.entrySet()) {
                    if (entrySet.getValue().get(0).equals("Flight Ready")) {
                        stringBuilder.append("{\"ship \" : \"" + entrySet.getKey() + "\",");
                        stringBuilder.append("\"type\" : \"" + entrySet.getValue().get(2) + "\",");
                        stringBuilder.append("\"price\" : \"" + entrySet.getValue().get(1) + "\", ");
                        stringBuilder.append("\"time\" : \"" + entrySet.getValue().get(3) + "\"}");
                        counter = counter + 1;
                        stringBuilder.append(",");
                    }
                    totalCounter = totalCounter + 1;
                    if (totalCounter != shipMap.size()) {
                        stringBuilder.append(",");
                    }
                }

                total = total + counter + "/" + totalCounter + " Total additional ships " + stringBuilder.toString();
                stringBuilder.append("]}");
                String result = stringBuilder.toString();
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "Error";
        }
    }

    private List<File> getBaseFileList(String directory) {
        List<File> filesInFolder = null;
        if(!new File(dir).exists()) new File(dir).mkdirs();
        try {
            filesInFolder = Files.walk(Paths.get(directory))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filesInFolder;
    }

    private void runSelenium() {
        if (Utilities.getInstance().isWindows()) {
            System.setProperty("webdriver.chrome.driver", "src/main/resources/chromedriver.exe");
        } else if (Utilities.getInstance().isUnix()) {
            URL inputURL = getClass().getResource("/chromedriver");
            File dest = new File("/tmp/chromedriver");
            try {
                FileUtils.copyURLToFile(inputURL, dest);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.setProperty("webdriver.chrome.driver", "/tmp/chromedriver");
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--window-size=1920,1200", "--ignore-certificate-errors");
        WebDriver driver = new ChromeDriver(options);

        driver.get(URL_BASE + "/pledge/ships");

        JavascriptExecutor js = (JavascriptExecutor) driver;
        // This  will scroll down the page by  1000 pixel vertical
        js.executeScript("window.scrollBy(0,10000)");

        try {
            long lastHeight = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");

            while (true) {
                ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(2000);

                long newHeight = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");
                if (newHeight == lastHeight) {
                    break;
                }
                lastHeight = newHeight;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String html = driver.getPageSource();
        driver.close();

        try (FileWriter writer = new FileWriter("outputHtml.html", false)) {
            writer.write(html);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Document doc = Jsoup.parse(html);
        Elements elements = doc.getElementsByAttributeValue("class", "ships-listing");

        ExecutorService cachePool = Executors.newFixedThreadPool(10);

        int totalNumberOfTasks = 0;
        for (Element shipListing : elements) {
            Elements ships = shipListing.getElementsByAttributeValue("class", "ship-item");
            for (Element ship : ships) {
                String shipName = ship.getElementsByAttributeValue("class", "name trans-02s").text();
                totalNumberOfTasks = totalNumberOfTasks + 1;
            }
        }
        CountDownLatch latch = new CountDownLatch(totalNumberOfTasks);
        for (Element shipListing : elements) {
            Elements ships = shipListing.getElementsByAttributeValue("class", "ship-item");
//            if (value.contains("ship-item")) {


            for (Element ship : ships) {

                String shipFullName = ship.getElementsByAttributeValue("class", "name trans-02s").text();
                String[] shipNameArray = shipFullName.split("-");

                String shipName = null;
                String shipType = null;
                if (shipNameArray.length >= 3) {
                    shipName = shipNameArray[0] + " " + shipNameArray[1];
                    shipType = shipNameArray[2];
                } else if (shipNameArray.length >= 2) {
                    shipName = shipNameArray[0];
                    shipType = shipNameArray[1];
                } else if (shipNameArray.length == 1) {
                    shipName = shipNameArray[0];
                    shipType = "Unknown";
                } else {
                    shipName = "Unknown";
                    shipType = "Unknown";
                }

                Element link = ship.select("a").first();
                String httpLink = link.attr("href");
                LOG.debug("ShipData: " + shipName);
                LOG.debug("ShipData Link: " + httpLink);

                String finalShipName = shipName;
                String finalShipType = shipType;
                LOG.debug(String.format("Spawning %s thread", finalShipName));

                cachePool.submit(() -> {
                    new ShipData(driver, URL_BASE, httpLink, finalShipName, finalShipType, latch);
                });

            }
        }


        LOG.info("Total Number of Tasks: " + totalNumberOfTasks);

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        cachePool.shutdown();
        cachePool.shutdownNow();
        try {
            cachePool.awaitTermination(3, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        try {
            Runtime.getRuntime().exec("taskkill /F /IM ChromeDriver.exe");
        } catch (IOException e) {

        }
    }

    private Map<String, List<String>> getShipStatusMap(List<File> newFileArray) {

        Map<String, List<String>> map = new HashMap<>();

        for (File file : newFileArray) {
            String json = null;
            LOG.info(file.getAbsoluteFile());
            try {
                json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

                Gson gson = new Gson(); // Or use new GsonBuilder().create();
                Ship ship = gson.fromJson(json, Ship.class);
                List<String> temp = new ArrayList<>();
                temp.add(ship.status);
                temp.add(ship.price);
                temp.add(ship.type);
                temp.add(ship.time);
                map.put(ship.name, temp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new TreeMap<>(map);
    }
}
