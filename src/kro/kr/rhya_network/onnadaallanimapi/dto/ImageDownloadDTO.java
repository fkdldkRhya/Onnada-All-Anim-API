package kro.kr.rhya_network.onnadaallanimapi.dto;

public class ImageDownloadDTO {
    public enum ImageType {
        ANIM, CHAR, CHAR_M_1, CHAR_M_2, CHAR_M_3
    }

    private ImageType imageType;
    private Long id;
    private String filePath;
    private String fileUri;

    public ImageDownloadDTO(ImageType imageType, Long id, String filePath, String fileUri) {
        this.imageType = imageType;
        this.id = id;
        this.filePath = filePath;
        this.fileUri = fileUri;
    }

    public ImageType getImageType() {
        return imageType;
    }

    public Long getId() {
        return id;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileUri() {
        return fileUri;
    }
}
