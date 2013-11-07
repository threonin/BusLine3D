package busline3d.message;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 *
 * @author Volker Schuller
 */
@Serializable
public class RadiusMessage extends AbstractMessage {

    private float radius;

    public RadiusMessage() {
    }

    public RadiusMessage(float radius) {
        this.radius = radius;
    }

    public float getRadius() {
        return radius;
    }

    @Override
    public String toString() {
        return "RadiusMessage{" + "radius=" + radius + '}';
    }
}
