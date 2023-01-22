package kro.kr.rhya_network.onnadaallanimapi.core;

import kro.kr.rhya_network.onnadaallanimapi.Main;
import kro.kr.rhya_network.onnadaallanimapi.dto.ImageDownloadDTO;
import kro.kr.rhya_network.onnadaallanimapi.util.DatabaseManager;
import kro.kr.rhya_network.onnadaallanimapi.util.ImageDownloadManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

public class HTMLParsingManager {
    // Connection 정보
    private final String MOZILLA_5_0 = "Mozilla/5.0";
    // JavaScript 정보
    private final String SCRIPT_CONTAINS_TEXT = "document.domain = \"onnada.com\";";

    public void setAnimDataList() throws IOException, ParseException, SQLException, ClassNotFoundException {
        // URL 정보
        final String QUARTER_ROOT_URL = "https://onnada.com/anime/quarter";

        System.out.println("Loading quarterly animation data...");

        // Connection web
        Connection rootConnection = Jsoup.connect(QUARTER_ROOT_URL);
        rootConnection.userAgent(MOZILLA_5_0);
        Document rootDocument = rootConnection.get();

        String quarterJson = rootDocument
                .getElementsByTag("body")
                .select("script")
                .toString();
        quarterJson = scriptReplaceV1(quarterJson);

        JSONParser jsonParser = new JSONParser();
        JSONObject quartersJSONObject = null;

        String[] quarterJsonSplit = quarterJson.split("\r\n");
        for (String quarterLine : quarterJsonSplit) {
            if (quarterLine.replace(" ", "").length() > 0 && quarterLine.contains(SCRIPT_CONTAINS_TEXT)) {
                quarterLine = scriptReplaceV2(quarterLine);
                quarterLine = quarterLine.substring(0, quarterLine.length() - 1);

                quartersJSONObject = (JSONObject) jsonParser.parse(quarterLine);

                break;
            }
        }

        if (quartersJSONObject != null) {
            System.out.println("Importing quarterly animation data succeeded!");

            JSONArray quartersJSONArray = (JSONArray)((JSONObject) quartersJSONObject.get("result")).get("items");

            System.out.println("Start extracting animation and character data!");

            for (int rootQuartersIndex = 0; rootQuartersIndex < quartersJSONArray.size(); rootQuartersIndex ++) {
                JSONArray quarterJSONArray = (JSONArray) quartersJSONArray.get(rootQuartersIndex);

                System.out.println(String.format("Total progress: %d / %d", rootQuartersIndex, quartersJSONArray.size()));

                for (int subQuartersIndex = 0; subQuartersIndex < quarterJSONArray.size(); subQuartersIndex ++) {
                    String quarter = (String) quarterJSONArray.get(subQuartersIndex);

                    System.out.println(String.format("Animated quarter: %s", quarter));
                    System.out.println(String.format("Whether animation data extraction is in progress: %b", dateChecker(quarter)));

                    if (!dateChecker(quarter))
                        continue;

                    System.out.println(String.format("%s Start Data Extraction for Quarterly.", quarter));
                    System.out.println("Refining quarter data...");

                    final String quarterURL = String.format("%s/%s", QUARTER_ROOT_URL, quarter);
                    // Connection
                    Connection quarterConnection = Jsoup.connect(quarterURL);
                    quarterConnection.userAgent(MOZILLA_5_0);
                    Document quarterDocument = quarterConnection.get();

                    String quarterAnimJson = quarterDocument
                            .getElementsByTag("body")
                            .select("script")
                            .toString();
                    quarterAnimJson = scriptReplaceV1(quarterAnimJson);

                    JSONObject quarterAnimJSONObject = null;
                    String[] quarterAnimJsonSplit = quarterAnimJson.split("\r\n");
                    for (String quarterLine : quarterAnimJsonSplit) {
                        if (quarterLine.replace(" ", "").length() > 0 && quarterLine.contains(SCRIPT_CONTAINS_TEXT)) {
                            quarterLine = scriptReplaceV2(quarterLine);
                            quarterLine = quarterLine.substring(0, quarterLine.length() - 1);

                            quarterAnimJSONObject = (JSONObject) jsonParser.parse(quarterLine);

                            break;
                        }
                    }

                    System.out.println("Quarterly data purification successful!");

                    if (quartersJSONObject != null) {
                        JSONObject quarterAnimJSONArrayOrJSONObjectCheckTemp = (JSONObject) quarterAnimJSONObject.get("result");
                        if (quarterAnimJSONArrayOrJSONObjectCheckTemp.get("items") instanceof JSONArray) {
                            JSONArray quartersAnimListJSONArray = (JSONArray) quarterAnimJSONArrayOrJSONObjectCheckTemp.get("items");
                            for (int animListIndex = 0 ; animListIndex < quartersAnimListJSONArray.size(); animListIndex ++) {
                                JSONArray quarterAnimListJSONArray = (JSONArray) quartersAnimListJSONArray.get(animListIndex);

                                System.out.println(String.format("Quarterly(main) progress: %d / %d", animListIndex, quartersAnimListJSONArray.size()));

                                for (int animIndex = 0 ; animIndex < quarterAnimListJSONArray.size(); animIndex ++) {
                                    long animID = (long) ((JSONObject) quarterAnimListJSONArray.get(animIndex)).get("id");
                                    String animURL = (String) ((JSONObject) quarterAnimListJSONArray.get(animIndex)).get("uri");

                                    System.out.println(String.format("Quarterly(sub) progress: %d / %d", animIndex, quarterAnimListJSONArray.size()));

                                    if (animDataInputChecker(animID)) {
                                        System.out.println("[Quarterly(sub)] Start inserting animation data!");
                                        animDataInput(animURL, animID, quarter);

                                        System.out.println("[Quarterly(sub)] Finished inserting animation data.");
                                    }
                                }
                            }
                        }else {
                            JSONObject quartersAnimListJSONObject = (JSONObject) quarterAnimJSONArrayOrJSONObjectCheckTemp.get("items");
                            Iterator quartersAnimListJSONObjecIterator = quartersAnimListJSONObject.keySet().iterator();

                            int index = 0;

                            for (; quartersAnimListJSONObjecIterator.hasNext();) {

                                System.out.println(String.format("Quarterly(main) progress: %d / %d", index, quartersAnimListJSONObject.size()));
                                index = index + 1;

                                String animKey = quartersAnimListJSONObjecIterator.next().toString();
                                JSONArray quarterAnimListJSONArray = (JSONArray) quartersAnimListJSONObject.get(animKey);
                                for (int animIndex = 0 ; animIndex < quarterAnimListJSONArray.size(); animIndex ++) {
                                    long animID = (long) ((JSONObject) quarterAnimListJSONArray.get(animIndex)).get("id");
                                    String animURL = (String) ((JSONObject) quarterAnimListJSONArray.get(animIndex)).get("uri");

                                    System.out.println(String.format("Quarterly(sub) progress: %d / %d", animIndex, quarterAnimListJSONArray.size()));

                                    if (animDataInputChecker(animID)) {
                                        System.out.println("[Quarterly(sub)] Start inserting animation data!");

                                        animDataInput(animURL, animID, quarter);

                                        System.out.println("[Quarterly(sub)] Finished inserting animation data.");
                                    }
                                }
                            }
                        }
                    }
                    System.out.println(String.format("%s Quarterly Data Extraction Completed!", quarter));
                }
            }

            System.out.println("End animation and character data extraction. ");
        }
    }

    private String scriptReplaceV1(String input) {
        return input
                .replace("<script>", String.format("%s<script>%s", "\r\n", "\r\n"))
                .replace("</script>", String.format("%s</script>%s", "\r\n", "\r\n"))
                .replace("<script type=\"text/javascript\">", String.format("%s<script type=\"text/javascript\">%s", "\r\n", "\r\n"));
    }

    private String scriptReplaceV2(String input) {
        return input
                .replace(SCRIPT_CONTAINS_TEXT, "")
                .replace("\r\n", "")
                .replace("var ONNADA = ", "")
                .trim();
    }

    private boolean dateChecker(String quarter) {
        if (!quarter.contains("."))
            return false;

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);

        String[] split = quarter.replace(".", "-").split("-");
        String input_year = split[0];
        String input_quarter_num = split[1];

        int now_quarter_num = -1;

        switch (month) {
            case 0:
            case 1:
            case 2:
                now_quarter_num = 1;
                break;
            case 3:
            case 4:
            case 5:
                now_quarter_num = 2;
                break;
            case 6:
            case 7:
            case 8:
                now_quarter_num = 3;
                break;
            case 9:
            case 10:
            case 11:
                now_quarter_num = 4;
                break;
        }

        if (now_quarter_num == -1)
            return false;

        if (Integer.parseInt(input_year) < year)
            return true;

        if (Integer.parseInt(input_year) > year)
            return false;

        if (Integer.parseInt(input_year) == year) {
            if (now_quarter_num > Integer.parseInt(input_quarter_num))
                return true;
            if (now_quarter_num == Integer.parseInt(input_quarter_num))
                return false;
            if (now_quarter_num < Integer.parseInt(input_quarter_num))
                return false;
        }

        return false;
    }

    private boolean animDataInputChecker(long id) throws SQLException, ClassNotFoundException, IOException {
        try {
            DatabaseManager databaseManager = new DatabaseManager(Main.DATABASE_SETTING_FILE);
            databaseManager.connection();
            databaseManager.setPreparedStatement("SELECT EXISTS (SELECT anim_id FROM onnada_anim_info WHERE anim_id = ? LIMIT 1) AS success;");
            databaseManager.getPreparedStatement().setLong(1, id);
            databaseManager.setResultSet();

            boolean result = false;

            if (databaseManager.getResultSet().next())
                result = databaseManager.getResultSet().getInt("success") == 1 ? false : true;

            databaseManager.allClose();

            return result;
        }catch (Exception ex) {
            throw ex;
        }
    }

    private void animDataInput(String animURL, long animID, String quarter) throws SQLException, ClassNotFoundException, IOException, ParseException {
        DatabaseManager databaseManager = new DatabaseManager(Main.DATABASE_SETTING_FILE);

        // URL 정보
        final String CHARACTER_ROOT_URL_FORMAT = "https://onnada.com/character/%d";

        System.out.println(String.format("[AnimDataInput (%d, %s)] Loading animation data...", animID, quarter));

        try {
            // Connection web
            Connection rootConnection = Jsoup.connect(animURL);
            rootConnection.userAgent(MOZILLA_5_0);
            Document rootDocument = rootConnection.get();

            String animJson = rootDocument
                    .getElementsByTag("body")
                    .select("script")
                    .toString();
            animJson = scriptReplaceV1(animJson);

            JSONParser jsonParser = new JSONParser();
            JSONObject animJSONObject = null;
            String[] animJsonSplit = animJson.split("\r\n");
            for (String animLine : animJsonSplit) {
                if (animLine.replace(" ", "").length() > 0 && animLine.contains(SCRIPT_CONTAINS_TEXT)) {
                    animLine = scriptReplaceV2(animLine);
                    animLine = animLine.substring(0, animLine.length() - 1);

                    animJSONObject = (JSONObject) jsonParser.parse(animLine);

                    break;
                }
            }

            StringBuilder stringBuilder = new StringBuilder();
            databaseManager.connection();

            if (animJSONObject != null) {
                animJSONObject = (JSONObject) animJSONObject.get("result");

                System.out.println(String.format("[AnimDataInput (%d, %s)] Importing animation data succeeded!", animID, quarter));

                // -----------------------------------------------------------
                // 애니메이션 데이터
                // -----------------------------------------------------------

                databaseManager.setPreparedStatement("INSERT INTO onnada_anim_info (anim_id, quarter, week, image, name_kr, name_v2, description, original, director, screenplay, character_design, music, production_company, genre, classification, keyword, country_of_production, air_date, age_classification) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                databaseManager.getPreparedStatement().setLong(1, animID);
                databaseManager.getPreparedStatement().setString(2, quarter);

                try {
                    databaseManager.getPreparedStatement().setString(3, (String) animJSONObject.get("week"));
                }catch (Exception ex) {
                    databaseManager.getPreparedStatement().setString(3, null);
                }

                try {
                    String animImageUri = (String) ((JSONObject) animJSONObject.get("image")).get("uri");
                    boolean result = ImageDownloadManager.saveImage(
                            animImageUri,
                            String.format("%s%s%d.jpg", Main.IMAGE_SAVE_PATH_FOR_ANIM, File.separator, animID),
                            ImageDownloadDTO.ImageType.ANIM,
                            animID);
                    if (result)
                        databaseManager.getPreparedStatement().setInt(4, 1);
                    else
                        databaseManager.getPreparedStatement().setInt(4, 0);
                }catch (Exception ex) {
                    databaseManager.getPreparedStatement().setInt(4, 0);
                }

                databaseManager.getPreparedStatement().setString(5, (String) animJSONObject.get("main_title"));
                databaseManager.getPreparedStatement().setString(6, (String) animJSONObject.get("official_title"));

                try {
                    String value = (String) animJSONObject.get("content");
                    if (value == null || value.replace(" ", "").length() == 0) {
                        databaseManager.getPreparedStatement().setString(7, null);
                    }else {
                        Document doc = Jsoup.parseBodyFragment(value);
                        databaseManager.getPreparedStatement().setString(7, doc.text());
                    }
                }catch (Exception ex) {
                    databaseManager.getPreparedStatement().setString(7, null);
                }

                try {
                    JSONArray jsonArray = (JSONArray) animJSONObject.get("original");
                    stringBuilder.setLength(0);
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JSONObject object = (JSONObject) jsonArray.get(i);
                        if (i == 0)
                            stringBuilder.append(object.get("name"));
                        else {
                            stringBuilder.append(",");
                            stringBuilder.append(object.get("name"));
                        }
                    }
                    databaseManager.getPreparedStatement().setString(8, stringBuilder.toString().replace(" ", "").length() == 0 ? null : stringBuilder.toString());
                }catch (Exception ex) {
                    databaseManager.getPreparedStatement().setString(8, null);
                }

                try {
                    JSONArray jsonArray = (JSONArray) animJSONObject.get("direction");
                    stringBuilder.setLength(0);
                    for (int i = 0; i < jsonArray.size(); i ++) {
                        JSONObject object = (JSONObject) jsonArray.get(i);
                        if (i == 0)
                            stringBuilder.append(object.get("name"));
                        else {
                            stringBuilder.append(",");
                            stringBuilder.append(object.get("name"));
                        }
                    }
                    databaseManager.getPreparedStatement().setString(9, stringBuilder.toString().replace(" ", "").length() == 0 ? null : stringBuilder.toString());
                }catch (Exception ex) {
                    databaseManager.getPreparedStatement().setString(9, null);
                }

                try {
                    JSONArray jsonArray = (JSONArray) animJSONObject.get("scenario");
                    stringBuilder.setLength(0);
                    for (int i = 0; i < jsonArray.size(); i ++) {
                        JSONObject object = (JSONObject) jsonArray.get(i);
                        if (i == 0)
                            stringBuilder.append(object.get("name"));
                        else {
                            stringBuilder.append(",");
                            stringBuilder.append(object.get("name"));
                        }
                    }
                    databaseManager.getPreparedStatement().setString(10, stringBuilder.toString().replace(" ", "").length() == 0 ? null : stringBuilder.toString());
                }catch (Exception ex) {
                    databaseManager.getPreparedStatement().setString(10, null);
                }

                try {
                    JSONArray jsonArray = (JSONArray) animJSONObject.get("characterdesign");
                    stringBuilder.setLength(0);
                    for (int i = 0; i < jsonArray.size(); i ++) {
                        JSONObject object = (JSONObject) jsonArray.get(i);
                        if (i == 0)
                            stringBuilder.append(object.get("name"));
                        else {
                            stringBuilder.append(",");
                            stringBuilder.append(object.get("name"));
                        }
                    }
                    databaseManager.getPreparedStatement().setString(11, stringBuilder.toString().replace(" ", "").length() == 0 ? null : stringBuilder.toString());
                }catch (Exception ex) {
                    databaseManager.getPreparedStatement().setString(11, null);
                }

                try {
                    JSONArray jsonArray = (JSONArray) animJSONObject.get("music");
                    stringBuilder.setLength(0);
                    for (int i = 0; i < jsonArray.size(); i ++) {
                        JSONObject object = (JSONObject) jsonArray.get(i);
                        if (i == 0)
                            stringBuilder.append(object.get("name"));
                        else {
                            stringBuilder.append(",");
                            stringBuilder.append(object.get("name"));
                        }
                    }
                    databaseManager.getPreparedStatement().setString(12, stringBuilder.toString().replace(" ", "").length() == 0 ? null : stringBuilder.toString());
                }catch (Exception ex) {
                    databaseManager.getPreparedStatement().setString(12, null);
                }

                try {
                    JSONArray jsonArray = (JSONArray) animJSONObject.get("production");
                    stringBuilder.setLength(0);
                    for (int i = 0; i < jsonArray.size(); i ++) {
                        JSONObject object = (JSONObject) jsonArray.get(i);
                        if (i == 0)
                            stringBuilder.append(object.get("name"));
                        else {
                            stringBuilder.append(",");
                            stringBuilder.append(object.get("name"));
                        }
                    }
                    databaseManager.getPreparedStatement().setString(13, stringBuilder.toString().replace(" ", "").length() == 0 ? null : stringBuilder.toString());
                }catch (Exception ex) {
                    databaseManager.getPreparedStatement().setString(13, null);
                }

                try {
                    databaseManager.getPreparedStatement().setString(14, (String) animJSONObject.get("category"));
                }catch (Exception ex) {
                    databaseManager.getPreparedStatement().setString(14, null);
                }

                try {
                    databaseManager.getPreparedStatement().setString(15, (String) animJSONObject.get("type"));
                }catch (Exception ex) {
                    databaseManager.getPreparedStatement().setString(15, null);
                }

                try {
                    databaseManager.getPreparedStatement().setString(16, (String) animJSONObject.get("keyword"));
                }catch (Exception ex) {
                    databaseManager.getPreparedStatement().setString(16, null);
                }

                try {
                    databaseManager.getPreparedStatement().setString(17, (String) animJSONObject.get("country"));
                }catch (Exception ex) {
                    databaseManager.getPreparedStatement().setString(17, null);
                }

                try {
                    databaseManager.getPreparedStatement().setString(18, (String) animJSONObject.get("date"));
                }catch (Exception ex) {
                    databaseManager.getPreparedStatement().setString(18, null);
                }

                try {
                    databaseManager.getPreparedStatement().setString(19, (String) animJSONObject.get("rate"));
                }catch (Exception ex) {
                    databaseManager.getPreparedStatement().setString(19, null);
                }

                databaseManager.executeUpdate();
                databaseManager.closePreparedStatement();
                // -----------------------------------------------------------
                // -----------------------------------------------------------

                System.out.println(String.format("[AnimDataInput (%d, %s)] Animated data insertion successful!", animID, quarter));
                System.out.println(String.format("[AnimDataInput (%d, %s)] Run an animated character data insertion task.", animID, quarter));

                // items 데이터
                JSONObject animItemsJSONObject = (JSONObject) animJSONObject.get("items");

                // -----------------------------------------------------------
                // 애니메이션 케릭터 데이터 ---> #################################
                // -----------------------------------------------------------
                if (animItemsJSONObject != null) {
                    JSONArray animCharacterJSONArray = (JSONArray) animItemsJSONObject.get("character");
                    for (int charIndex = 0 ; charIndex < animCharacterJSONArray.size(); charIndex ++) {
                        JSONObject charSubInfo = (JSONObject) animCharacterJSONArray.get(charIndex);

                        Long charID = Long.parseLong((String) charSubInfo.get("id"));
                        String url = String.format(CHARACTER_ROOT_URL_FORMAT, charID);

                        System.out.println(String.format("[AnimDataInput - Character (%d(anim), %s, %d(character))] Animation character task progress: %d / %d", animID, quarter, charID, charIndex, animCharacterJSONArray.size()));

                        // Connection web
                        Connection charConnection = Jsoup.connect(url);
                        charConnection.userAgent(MOZILLA_5_0);
                        Document charDocument = charConnection.get();

                        String charJson = charDocument
                                .getElementsByTag("body")
                                .select("script")
                                .toString();
                        charJson = scriptReplaceV1(charJson);

                        JSONObject charJSONObject = null;
                        String[] charJsonSplit = charJson.split("\r\n");
                        for (String charLine : charJsonSplit) {
                            if (charLine.replace(" ", "").length() > 0 && charLine.contains(SCRIPT_CONTAINS_TEXT)) {
                                charLine = scriptReplaceV2(charLine);
                                charLine = charLine.substring(0, charLine.length() - 1);

                                charJSONObject = (JSONObject) jsonParser.parse(charLine);

                                break;
                            }
                        }

                        if (charJSONObject != null) {
                            charJSONObject = (JSONObject) charJSONObject.get("result");

                            databaseManager.setPreparedStatement("INSERT INTO onnada_anim_character_info (character_id, anim_id, image, more_image_1, more_image_2, more_image_3, name_kr, name_en, name_jp1, name_jp2, voice_actor_name_kr, voice_actor_name_en, voice_actor_name_jp1, profile, age, birthday, height) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                            databaseManager.getPreparedStatement().setLong(1, charID);
                            databaseManager.getPreparedStatement().setLong(2, animID);

                            try {
                                System.out.println((JSONObject) charJSONObject.get("image"));
                                String charImageUri = (String) ((JSONObject) charJSONObject.get("image")).get("uri");
                                boolean result = ImageDownloadManager.saveImage(
                                        charImageUri,
                                        String.format("%s%s%d.jpg", Main.IMAGE_SAVE_PATH_FOR_CHAR, File.separator, charID),
                                        ImageDownloadDTO.ImageType.CHAR,
                                        charID);
                                if (result)
                                    databaseManager.getPreparedStatement().setInt(3, 1);
                                else
                                    databaseManager.getPreparedStatement().setInt(3, 0);
                            }catch (Exception ex) {
                                databaseManager.getPreparedStatement().setInt(3, 0);
                            }

                            JSONObject charItemsJSONObject = (JSONObject) charJSONObject.get("items");

                            try {
                                JSONArray charMoreImage = (JSONArray) charItemsJSONObject.get("image");

                                boolean check_parm_4 = false;
                                boolean check_parm_5 = false;
                                boolean check_parm_6 = false;

                                for (int i = 0; i < charMoreImage.size(); i ++) {
                                    if (i >= 3) break;

                                    JSONObject jsonObject = (JSONObject) charMoreImage.get(i);
                                    String uri = (String) ((JSONObject) jsonObject.get("image")).get("uri");

                                    int db_index = -1;

                                    switch (i) {
                                        case 0 : {
                                            db_index = 4;
                                            check_parm_4 = true;
                                            break;
                                        }

                                        case 1 : {
                                            db_index = 5;
                                            check_parm_5 = true;
                                            break;
                                        }

                                        case 2 : {
                                            db_index = 6;
                                            check_parm_6 = true;
                                            break;
                                        }
                                    }

                                    boolean result = false;

                                    if (db_index != -1) {
                                        try {
                                            if (db_index == 4) {
                                                result = ImageDownloadManager.saveImage(
                                                        uri,
                                                        String.format("%s%s%d_1.jpg", Main.IMAGE_SAVE_PATH_FOR_CHAR, File.separator, charID),
                                                        ImageDownloadDTO.ImageType.CHAR_M_1,
                                                        charID);
                                            }else if (db_index == 5) {
                                                result = ImageDownloadManager.saveImage(
                                                        uri,
                                                        String.format("%s%s%d_2.jpg", Main.IMAGE_SAVE_PATH_FOR_CHAR, File.separator, charID),
                                                        ImageDownloadDTO.ImageType.CHAR_M_2,
                                                        charID);
                                            }else {
                                                result = ImageDownloadManager.saveImage(
                                                        uri,
                                                        String.format("%s%s%d_3.jpg", Main.IMAGE_SAVE_PATH_FOR_CHAR, File.separator, charID),
                                                        ImageDownloadDTO.ImageType.CHAR_M_3,
                                                        charID);
                                            }

                                            if (result)
                                                databaseManager.getPreparedStatement().setInt(db_index, 1);
                                            else
                                                databaseManager.getPreparedStatement().setInt(db_index, 0);
                                        }catch (Exception ex) {
                                            databaseManager.getPreparedStatement().setInt(db_index, 0);
                                        }
                                    }
                                }

                                if (!check_parm_4)
                                    databaseManager.getPreparedStatement().setInt(4, 0);
                                if (!check_parm_5)
                                    databaseManager.getPreparedStatement().setInt(5, 0);
                                if (!check_parm_6)
                                    databaseManager.getPreparedStatement().setInt(6, 0);
                            }catch (Exception ex) {
                                databaseManager.getPreparedStatement().setInt(4, 0);
                                databaseManager.getPreparedStatement().setInt(5, 0);
                                databaseManager.getPreparedStatement().setInt(6, 0);
                            }

                            try {
                                databaseManager.getPreparedStatement().setString(7, (String) charJSONObject.get("name_kr"));
                            }catch (Exception ex) {
                                databaseManager.getPreparedStatement().setString(7, null);
                            }

                            try {
                                databaseManager.getPreparedStatement().setString(8, (String) charJSONObject.get("name_en"));
                            }catch (Exception ex) {
                                databaseManager.getPreparedStatement().setString(8, null);
                            }

                            try {
                                databaseManager.getPreparedStatement().setString(9, (String) charJSONObject.get("name_jp1"));
                            }catch (Exception ex) {
                                databaseManager.getPreparedStatement().setString(9, null);
                            }

                            try {
                                databaseManager.getPreparedStatement().setString(10, (String) charJSONObject.get("name_jp2"));
                            }catch (Exception ex) {
                                databaseManager.getPreparedStatement().setString(10, null);
                            }

                            try {
                                databaseManager.getPreparedStatement().setString(11, (String) charJSONObject.get("name2_kr"));
                            }catch (Exception ex) {
                                databaseManager.getPreparedStatement().setString(11, null);
                            }

                            try {
                                databaseManager.getPreparedStatement().setString(12, (String) charJSONObject.get("name2_en"));
                            }catch (Exception ex) {
                                databaseManager.getPreparedStatement().setString(12, null);
                            }

                            try {
                                databaseManager.getPreparedStatement().setString(13, (String) charJSONObject.get("name2_jp1"));
                            }catch (Exception ex) {
                                databaseManager.getPreparedStatement().setString(13, null);
                            }

                            try {
                                databaseManager.getPreparedStatement().setString(14, (String) charJSONObject.get("profile"));
                            }catch (Exception ex) {
                                databaseManager.getPreparedStatement().setString(14, null);
                            }

                            try {
                                String value = (String) charJSONObject.get("profile");
                                if (value == null || value.replace(" ", "").length() == 0) {
                                    databaseManager.getPreparedStatement().setString(14, null);
                                }else {
                                    Document doc = Jsoup.parseBodyFragment(value);
                                    databaseManager.getPreparedStatement().setString(14, doc.text());
                                }
                            }catch (Exception ex) {
                                databaseManager.getPreparedStatement().setString(14, null);
                            }

                            try {
                                databaseManager.getPreparedStatement().setInt(15, (int) charJSONObject.get("age"));
                            }catch (Exception ex) {
                                databaseManager.getPreparedStatement().setInt(15, -1);
                            }

                            try {
                                databaseManager.getPreparedStatement().setString(16, (String) charJSONObject.get("birth"));
                            }catch (Exception ex) {
                                databaseManager.getPreparedStatement().setString(16, null);
                            }

                            try {
                                databaseManager.getPreparedStatement().setInt(17, (int) charJSONObject.get("cm"));
                            }catch (Exception ex) {
                                databaseManager.getPreparedStatement().setInt(17, -1);
                            }

                            databaseManager.executeUpdate();
                            databaseManager.closePreparedStatement();

                            // -----------------------------------------------------------
                            // 애니메이션 명대사 데이터 ---> *********************************
                            // -----------------------------------------------------------

                            JSONArray charFamousJSONArray = (JSONArray) charItemsJSONObject.get("famous");
                            if (charFamousJSONArray != null) {
                                for (int i = 0; i < charFamousJSONArray.size(); i ++) {
                                    JSONObject jsonObject = (JSONObject) charFamousJSONArray.get(i);

                                    databaseManager.setPreparedStatement("INSERT INTO onnada_anim_famous_info (famous_id, anim_id, character_id, text) VALUES (?,?,?,?)");
                                    databaseManager.getPreparedStatement().setLong(1, Long.parseLong((String) jsonObject.get("id")));
                                    databaseManager.getPreparedStatement().setLong(2, animID);
                                    databaseManager.getPreparedStatement().setLong(3, charID);

                                    try {
                                        databaseManager.getPreparedStatement().setString(4, (String) jsonObject.get("content"));
                                    }catch (Exception ex) {
                                        databaseManager.getPreparedStatement().setString(4, null);
                                    }

                                    databaseManager.executeUpdate();
                                    databaseManager.closePreparedStatement();
                                }
                            }

                            // -----------------------------------------------------------
                            // ---> ******************************************************
                            // -----------------------------------------------------------
                        }
                    }
                }
                // -----------------------------------------------------------
                // ---> ######################################################
                // -----------------------------------------------------------
            }

            databaseManager.allClose();
        }catch (Exception ex) {
            databaseManager.allClose();
            throw ex;
        }
    }
}
