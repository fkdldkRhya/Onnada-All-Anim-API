package kro.kr.rhya_network.onnadaallanimapi;

import kro.kr.rhya_network.onnadaallanimapi.core.HTMLParsingManager;
import kro.kr.rhya_network.onnadaallanimapi.dto.ImageDownloadDTO;
import kro.kr.rhya_network.onnadaallanimapi.util.DatabaseManager;
import org.apache.commons.lang3.time.StopWatch;
import org.ini4j.Ini;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;

public class Main {
    public static String IMAGE_SAVE_PATH_FOR_ANIM = "\\resources\\anim_search\\images_anim";
    public static String IMAGE_SAVE_PATH_FOR_CHAR = "\\resources\\anim_search\\images_character";
    public static String DATABASE_SETTING_FILE = "\\resources\\anim_search\\onnada_all_anim_api_database_info.ini";

    public static void main(String[] args) throws InterruptedException, IOException, SQLException, ClassNotFoundException {
        // 초기 메시지 출력
        System.out.println("RHYA.Network Onnada-All-Anim-API Start");
        System.out.println("ONNADA Animation Data Extraction API (RHYA.Network only)");
        System.out.println("");
        System.out.println("Copyright (c) 2023 RHYA.Network. All rights reserved.");
        System.out.println("");
        System.out.println("This API acts as 'https://onnada.com/''s data extraction and purification.");
        System.out.println("We hereby notify you that the data is not commercially available.");
        System.out.println("");
        System.out.println("");

        final String PROPERTIES_INI_SECTION = "properties";
        File propertiesFile = new File("AnimSearch_OnnadaAllAnimAPI.txt");
        if (propertiesFile.exists()) {
            Ini ini = new Ini(propertiesFile);

            IMAGE_SAVE_PATH_FOR_ANIM = ini.get(PROPERTIES_INI_SECTION, "path_for_anim");
            IMAGE_SAVE_PATH_FOR_CHAR = ini.get(PROPERTIES_INI_SECTION, "path_for_character");
            DATABASE_SETTING_FILE = ini.get(PROPERTIES_INI_SECTION, "path_for_database");
        }

        // 실행 시간 계산
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // Core 함수 호출
        try {
            setSSL();

            System.out.println("Start, HTMLParsingManager!");

            HTMLParsingManager htmlParsingManager = new HTMLParsingManager();
            htmlParsingManager.setAnimDataList();
        }catch (Exception ex) {
            // 오류 발생 작업 중지!
            System.out.println("<== [WARNING] ==>");
            System.out.println(ex.toString());
        }

        // 실행 시간 출력
        stopWatch.stop();

        System.out.println();
        System.out.println("---------------------------------------------");
        System.out.println(String.format("Total StopWatch 'Onnada-All-Anim-API': running time = %d ns", stopWatch.getTime()));
        System.out.println();
    }

    public static void setSSL() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        // TODO Auto-generated method stub
                        return null;
                    }
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                        // TODO Auto-generated method stub

                    }
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                        // TODO Auto-generated method stub
                    }
                }
        };
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    public static boolean imageDownloadTask(ImageDownloadDTO imageDownloadDTO) throws SQLException, IOException, ClassNotFoundException {
        try {
            URL url;
            InputStream in = null;
            OutputStream out = null;
            try {
                url = new URL(imageDownloadDTO.getFileUri());
                in = url.openStream();
                out = new FileOutputStream(imageDownloadDTO.getFilePath());

                while(true){
                    int data = in.read();

                    if(data == -1)
                        break;

                    out.write(data);
                }

                in.close();
                out.close();

                return true;
            } catch (Exception e) {
                e.printStackTrace();

                throw e;
            }finally {
                if(in != null){in.close();}
                if(out != null){out.close();}
            }
        }catch (Exception ex) {
            DatabaseManager databaseManager = new DatabaseManager(DATABASE_SETTING_FILE);
            databaseManager.connection();

            boolean updateChecker = false;

            switch (imageDownloadDTO.getImageType()) {
                case ANIM:
                    databaseManager.setPreparedStatement("UPDATE onnada_anim_info SET image = 0 WHERE anim_id = ?");
                    databaseManager.getPreparedStatement().setLong(1, imageDownloadDTO.getId());
                    databaseManager.executeUpdate();
                    databaseManager.allClose();

                    updateChecker = true;

                    break;

                case CHAR:
                    databaseManager.setPreparedStatement("UPDATE onnada_anim_character_info SET image = 0 WHERE character_id = ?");
                    databaseManager.getPreparedStatement().setLong(1, imageDownloadDTO.getId());
                    databaseManager.executeUpdate();
                    databaseManager.allClose();

                    updateChecker = true;

                    break;

                case CHAR_M_1:
                    databaseManager.setPreparedStatement("UPDATE onnada_anim_character_info SET more_image_1 = 0 WHERE character_id = ?");
                    databaseManager.getPreparedStatement().setLong(1, imageDownloadDTO.getId());
                    databaseManager.executeUpdate();
                    databaseManager.allClose();

                    updateChecker = true;

                    break;

                case CHAR_M_2:
                    databaseManager.setPreparedStatement("UPDATE onnada_anim_character_info SET more_image_2 = 0 WHERE character_id = ?");
                    databaseManager.getPreparedStatement().setLong(1, imageDownloadDTO.getId());
                    databaseManager.executeUpdate();
                    databaseManager.allClose();

                    updateChecker = true;

                    break;

                case CHAR_M_3:
                    databaseManager.setPreparedStatement("UPDATE onnada_anim_character_info SET more_image_3 = 0 WHERE character_id = ?");
                    databaseManager.getPreparedStatement().setLong(1, imageDownloadDTO.getId());
                    databaseManager.executeUpdate();
                    databaseManager.allClose();

                    updateChecker = true;

                    break;
            }

            // 오류 발생
            System.out.println("<== [IMAGE DOWNLOADER WARNING] ==>");
            System.out.println(String.format("Image File URL: %s", imageDownloadDTO.getFileUri()));
            System.out.println(String.format("Image File Path: %s", imageDownloadDTO.getFilePath()));
            System.out.println(String.format("[ Auto Fix Result ] Database update: %b", updateChecker));
            System.out.println(String.format("[ Auto Fix Result ] Image file delete: %b", new File(imageDownloadDTO.getFilePath()).delete()));
            System.out.println("<== -------------------------- ==>");

            return false;
        }
    }
}
