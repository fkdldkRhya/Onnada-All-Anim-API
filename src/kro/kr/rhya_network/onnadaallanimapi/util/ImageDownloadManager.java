package kro.kr.rhya_network.onnadaallanimapi.util;

import kro.kr.rhya_network.onnadaallanimapi.Main;
import kro.kr.rhya_network.onnadaallanimapi.dto.ImageDownloadDTO;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

public class ImageDownloadManager {
    public static boolean saveImage(String input, String path, ImageDownloadDTO.ImageType imageType, Long id) throws IOException, SQLException, ClassNotFoundException {
        if (new File(path).exists()) return true;

        return Main.imageDownloadTask(new ImageDownloadDTO(imageType, id, path, input));
    }
}
