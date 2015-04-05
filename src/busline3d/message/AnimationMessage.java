package busline3d.message;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 *
 * @author Volker Schuller
 */
@Serializable
public class AnimationMessage extends AbstractMessage {

    private String object;
    private String animation;
    private float speed;

    public AnimationMessage() {
    }

    public AnimationMessage(String object, String animation, float speed) {
        this.object = object;
        this.animation = animation;
        this.speed = speed;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getAnimation() {
        return animation;
    }

    public void setAnimation(String animation) {
        this.animation = animation;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }
}
