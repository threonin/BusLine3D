package busline3d.message;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 *
 * @author Volker Schuller
 */
@Serializable
public class SetNameMessage extends AbstractMessage {

    private String name;

    public SetNameMessage() {
    }

    public SetNameMessage(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "SetNameMessage{" + "name=" + name + '}';
    }
}
