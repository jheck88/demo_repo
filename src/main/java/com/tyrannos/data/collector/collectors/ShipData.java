package com.tyrannos.data.collector.collectors;

import com.google.gson.*;
import com.tyrannos.data.collector.util.DBManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.postgresql.util.PGobject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ShipData implements Runnable {

    private final String createTable = "CREATE TABLE \"ShipModel\" (id SERIAL NOT NULL PRIMARY KEY, SHIPNAME VARCHAR(100), type VARCHAR(100), name VARCHAR(100), mounts VARCHAR(100), component_size VARCHAR(100), size VARCHAR(100), details VARCHAR(100), quantity VARCHAR(100), manufacturer VARCHAR(100), component_class VARCHAR(100), date timestamp);";
    private static final String path = "C:\\Users\\jerod\\workspace\\EvilTyrannosDataCollector\\logs\\jsonFiles\\";
    private CountDownLatch latch = null;
    private String shipName = null;
    private String shipType = null;
    private String link = null;
    private String baseUrl = null;
    private WebDriver driver = null;
    private String priceValue = null;
    DBManager dbManager = new DBManager();
    Logger LOG = Logger.getLogger(ShipData.class);

    public ShipData(WebDriver driver, String baseUrl, String link, String shipName, String shipType, CountDownLatch latch) {
        this.baseUrl = baseUrl.trim();
        this.link = link.trim();
        this.shipName = shipName.trim();
        this.latch = latch;
        this.driver = driver;
        this.shipType = shipType.trim();

        run();
    }


    @Override
    public void run() {
        if (!new File(path).exists()) new File(path).mkdirs();
        String shipFileName = shipName.trim()
                .replaceAll(" ", "-")
                .replaceAll("\\Q'\\E", "");
        shipFileName = StringUtils.replace(shipFileName, ".", "");
        shipFileName = StringUtils.stripAccents(shipFileName);
        String filePath = path + shipFileName + ".json";
        File fileChecker = new File(filePath);

        try {

            Document doc = Jsoup.connect(baseUrl + link).get();
            Gson gson = new GsonBuilder().create();

            Elements elements = doc.getElementsByAttribute("data-model");

            List<JsonObject> shipAssets = new ArrayList<>();

            String shipStatus = doc.select(".prod-status > span:nth-child(1)").text();
            for (Element element : elements) {
                String json = element.dataset().toString();
                JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
                LOG.debug(jsonObject);


                shipAssets.add(jsonObject);
            }


            try {
                Elements finalPriceElements = doc.getElementsByAttributeValue("class", "final-price");
                if (finalPriceElements.size() > 0) {
                    Element priceElement = finalPriceElements.first();
                    priceValue = priceElement.text();
                }
                if (priceValue == null || priceValue.contains("null")) {
                    priceValue = "Unavailable";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            LocalDateTime retrievalTime = getTime();

            LOG.info(filePath);
            if (new File(filePath).exists()) {
                new File(filePath).delete();
            }

            StringBuilder jsonStringBuilder = new StringBuilder();

            jsonStringBuilder.append("{\"name\": \"" + shipFileName + "\"," +
                    " \"type\" : \"" + shipType + "\", " +
                    "\"price\": \"" + priceValue + "\", " +
                    "\"status\" : \"" + shipStatus + "\", " +
                    "\"time\" : \"" + retrievalTime.toString() + "\", ");
            jsonStringBuilder.append("\"attributes\": ");
            jsonStringBuilder.append(gson.toJson(shipAssets));
            jsonStringBuilder.append("}");

            String jsonString = jsonStringBuilder.toString();
            try (FileWriter writer = new FileWriter(filePath, false)) {
                writer.write(jsonString);
            } catch (IOException e) {
                e.printStackTrace();
            }

            loadIntoDB(jsonString, shipAssets);


            latch.countDown();

        } catch (
                Exception e) {
            LOG.error(e);
            e.printStackTrace();
        }
    }

    private void loadIntoDB(String jsonString, List<JsonObject> shipList) {
        try (Connection con = dbManager.createConnection()) {
            try {

                double shipPrice = 0.0;
                if (!priceValue.equals("Unavailable")) {
                    priceValue = priceValue.replace("$", "");
                    String[] getPrice = priceValue.split(" ");
                    shipPrice = Double.valueOf(getPrice[0]);
                }

                PGobject jsonObject = new PGobject();
                PreparedStatement st = con.prepareStatement("INSERT INTO \"ShipData\" (DATA, DATE, SHIPTYPE, SHIPPRICE, SHIPNAME) VALUES (?, ?, ?, ?, ?)");
                jsonObject.setType("json");
                jsonObject.setValue(jsonString);
                st.setObject(1, jsonObject);
                st.setTimestamp(2, getCurrentTimeStamp());
                st.setString(3, shipType);
                st.setObject(4, shipPrice, 2, 2);
                st.setString(5, shipName);
                int i = st.executeUpdate();
                if (i > 0) {
                    LOG.debug("success");
                } else {
                    LOG.error("stuck somewhere");
                    throw new Exception();
                }
                st.close();
                con.close();

                getModels(shipList);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getModels(List<JsonObject> shipList) {
        Gson gson = new GsonBuilder().create();
        try {
            try {
                Statement statement = dbManager.createConnection().createStatement();
                statement.execute("DROP TABLE ShipModel");
                statement.close();
            } catch (SQLException ex) {

            }
            try {
                Statement statement = dbManager.createConnection().createStatement();
                statement.execute(createTable);
                statement.close();
            } catch (SQLException ex) {

            }
            for (JsonObject object : shipList) {
                JsonElement element = object.get("model");
                JsonObject modelObject = element.getAsJsonObject();
                modelObject.get("type").toString();
                Connection con = dbManager.createConnection();
                PreparedStatement st = con.prepareStatement("INSERT INTO \"ShipModel\" (SHIPNAME, type, name, mounts, component_size, size, details, quantity, manufacturer, component_class, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                st.setString(1, shipName);
                st.setString(2, modelObject.get("type").toString());
                st.setString(3, modelObject.get("name").toString());
                st.setString(4, modelObject.get("mounts").toString());
                st.setString(5, modelObject.get("component_size").toString());
                st.setString(6, modelObject.get("size").toString());
                st.setString(7, modelObject.get("details").toString());
                st.setString(8, modelObject.get("quantity").toString());
                st.setString(9, modelObject.get("manufacturer").toString());
                st.setString(10, modelObject.get("component_class").toString());
                st.setTimestamp(11, getCurrentTimeStamp());
                int i = st.executeUpdate();
                if (i > 0) {
                    LOG.debug("success");
                } else {
                    LOG.error("stuck somewhere");
                    throw new Exception();
                }
                st.close();
                con.close();
            }
        } catch (IOException e) {
            LOG.error(e);
        } catch (SQLException e) {
            LOG.error(e);
        } catch (ClassNotFoundException e) {
            LOG.error(e);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public java.sql.Date getCurrentDatetime() {
        java.util.Date today = new java.util.Date();
        return new java.sql.Date(today.getTime());
    }

    private LocalDateTime getTime() {
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String text = date.format(formatter);
        LocalDateTime parsedDate = LocalDateTime.parse(text, formatter);
        return parsedDate;
    }

    private static java.sql.Timestamp getCurrentTimeStamp() {
        java.util.Date today = new java.util.Date();
        return new java.sql.Timestamp(today.getTime());
    }
}