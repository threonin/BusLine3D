package util;

import busline3d.message.NewPassengerMessage;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;
import com.mortennobel.imagescaling.AdvancedResizeOp;
import com.mortennobel.imagescaling.ResampleOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import util.appstate.ClientAppState;

/**
 *
 * @author Volker Schuller
 */
public class NetworkAssetLocator implements AssetLocator {

    private static final ResampleOp resampleOp = new ResampleOp(75, 75);
    private static HashMap<String, byte[]> pictures = new HashMap<String, byte[]>();
    private static ClientAppState clientAppState;
    private static final JFileChooser fileChooser = new JFileChooser();
    private String rootPath;

    static {
        fileChooser.setFileFilter(new FileNameExtensionFilter("Bilder", "jpg", "jpeg", "gif", "png"));
        resampleOp.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.VerySharp);
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public AssetInfo locate(final AssetManager manager, final AssetKey key) {
        if (!key.getName().contains(rootPath)) {
            return null;
        }
        final String name = key.getName().replace(rootPath, "").split("\\.")[0];
        if (pictures.containsKey(name)) {
            return new AssetInfo(manager, key) {
                @Override
                public InputStream openStream() {
                    return new ByteArrayInputStream(pictures.get(name));
                }
            };
        }
        return null;
    }

    public static void openDialog(final NameCallback callback) {
        new Thread() {
            @Override
            public void run() {
                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    try {
                        BufferedImage src = ImageIO.read(fileChooser.getSelectedFile());
                        BufferedImage rescaled = resampleOp.filter(src, null);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(rescaled, "JPEG", baos);
                        baos.close();
                        String name = fileChooser.getSelectedFile().getName().split("\\.")[0];
                        byte[] data = baos.toByteArray();
                        addPicture(name, data);
                        if (clientAppState != null) {
                            clientAppState.sendMessage(new NewPassengerMessage(name, data));
                        }
                        callback.returnName(name);
                    } catch (IOException ex) {
                        Logger.getLogger(NetworkAssetLocator.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }.start();
    }

    public interface NameCallback {

        public void returnName(String name);
    }

    public static void addPicture(String name, byte[] data) {
        pictures.put(name, data);
    }

    public static void setClientAppState(ClientAppState clientAppState) {
        NetworkAssetLocator.clientAppState = clientAppState;
    }

    public static HashMap<String, byte[]> getPictures() {
        return pictures;
    }
}
