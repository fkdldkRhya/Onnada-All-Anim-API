package kro.kr.rhya_network.onnadaallanimapi.util;

import kro.kr.rhya_network.onnadaallanimapi.dto.ImageDownloadDTO;

import java.io.IOException;
import java.util.ArrayList;

public class ImageDownloadManager {
    public static ArrayList<ImageDownloadDTO> imageDownloadDTOS = new ArrayList<>();

    public void saveImage(String input, String path, ImageDownloadDTO.ImageType imageType, Long id) throws IOException {
        imageDownloadDTOS.add(new ImageDownloadDTO(imageType, id, path, input));
    }
}
