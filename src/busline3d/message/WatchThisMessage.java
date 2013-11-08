package busline3d.message;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 *
 * @author Volker Schuller
 */
@Serializable
public class WatchThisMessage extends AbstractMessage {

    private String name;

    public WatchThisMessage() {
    }

    public WatchThisMessage(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "WatchThisMessage{" + "name=" + name + '}';
    }
}
