package kro.kr.rhya_network.onnadaallanimapi.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class ImageDownloadManager {
    public void saveImage(String input, String path) throws IOException {
        URL url = null;
        InputStream in = null;
        OutputStream out = null;
        try {
            url = new URL(input);
            in = url.openStream();
            out = new FileOutputStream(path);

            while(true){
                int data = in.read();

                if(data == -1)
                    break;

                out.write(data);
            }

            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();

            throw e;
        }finally {
            if(in != null){in.close();}
            if(out != null){out.close();}
        }
    }
}
