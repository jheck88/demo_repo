package com.tyrannos.data.collector.collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tyrannos.data.collector.util.DBManager;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CollectFromStarHanger {
    private static final Logger LOG = Logger.getLogger(CollectFromStarHanger.class);
    DBManager dbManager = new DBManager();
    private final String createTable = "CREATE TABLE \"StarHangerShips\" (id SERIAL NOT NULL PRIMARY KEY, time timestamp, SHIPNAME VARCHAR(100), SHIPPRICE numeric, SHIPLINK VARCHAR(100));";

    public void getStarHangerResources() throws Exception {
        try {
            Document doc = Jsoup.connect("https://star-hangar.com/star-citizen.html?p=1&product_list_limit=30").get();
            Gson gson = new GsonBuilder().create();
            String catalogSize = doc.select("div.toolbar:nth-child(1) > div:nth-child(9) > ul:nth-child(2) > li:nth-child(3) > strong:nth-child(1) > span:nth-child(1)").text();

            Map<String, List<String>> itemMap = new TreeMap<>();
            List<String> itemDetails = null;
            int counter = 1;
            while (counter != Integer.parseInt(catalogSize)) {
                doc = null;
                String url = "https://star-hangar.com/star-citizen.html?p=" + counter + "&product_list_limit=30";
                doc = Jsoup.connect(url).get();
                Elements itemsBlock = doc.getElementsByAttributeValue("data-container", "product-list");
                for (Element item : itemsBlock) {
                    itemDetails = new ArrayList<>();
                    Elements elementName = item.getElementsByAttributeValue("class", "product-item-link");
                    Elements itemPrice = item.getElementsByAttributeValue("data-price-type", "finalPrice");
                    String price = itemPrice.text().replace("$", "");
                    String link = elementName.attr("href");
                    String name = elementName.text();
                    itemDetails.add(price);
                    itemDetails.add(link);
                    itemMap.put(name, itemDetails);
                }

                counter = counter + 1;
            }


            try {
                Statement statement = dbManager.createConnection().createStatement();
                statement.execute("DROP TABLE StarHangerShips");
                statement.close();
            } catch (SQLException | ClassNotFoundException e) {
                LOG.error("Error in connection");
            }
            try {
                Statement statement = dbManager.createConnection().createStatement();
                statement.execute(createTable);
                statement.close();
            } catch (SQLException | ClassNotFoundException e) {
                LOG.error("Error in connection");
            }

            for (Map.Entry<String, List<String>> entry : itemMap.entrySet()) {



                Connection con = dbManager.createConnection();
                PreparedStatement st = con.prepareStatement("INSERT INTO \"StarHangerShips\" (time, SHIPNAME, SHIPPRICE, SHIPLINK) VALUES (?, ?, ?, ?)");
                st.setTimestamp(1, getCurrentTimeStamp());
                st.setString(2, entry.getKey());
                st.setObject(3, Double.parseDouble(entry.getValue().get(0)), 2, 2);
                st.setString(4, entry.getValue().get(1));

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
        } catch (IOException | ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static java.sql.Timestamp getCurrentTimeStamp() {
        java.util.Date today = new java.util.Date();
//        Instant.now().toEpochMilli();
        ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.of("Z"));
        long millis = now.toInstant().toEpochMilli();
        return new java.sql.Timestamp(millis);

    }
}
