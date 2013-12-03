package busline3d.message;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 *
 * @author Volker Schuller
 */
@Serializable
public class NewPassengerMessage extends AbstractMessage {

    private String name;
    private byte[] data;

    public NewPassengerMessage() {
    }

    public NewPassengerMessage(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
