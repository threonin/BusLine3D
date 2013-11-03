package util.message;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 *
 * @author Volker Schuller
 */
@Serializable
public class MovementMessage extends AbstractMessage {

    private float servertime;
    private ObjectData[] data;

    public MovementMessage() {
    }

    public MovementMessage(float servertime, ObjectData[] data) {
        this.servertime = servertime;
        this.data = data;
    }

    public float getServertime() {
        return servertime;
    }

    public ObjectData[] getData() {
        return data;
    }

    @Override
    public String toString() {
        String objects = "\n";
        for (ObjectData obj : data) {
            objects += obj + "\n";
        }
        return "MovementMessage{" + "servertime=" + servertime + ", data=[" + objects + "]}";
    }
}
