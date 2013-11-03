package util.message;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.serializing.Serializable;

/**
 *
 * @author Volker Schuller
 */
@Serializable
public class ObjectDefinition {

    private String name;
    private String type;
    private Vector3f translation;
    private Quaternion rot;

    public ObjectDefinition() {
    }

    public ObjectDefinition(String name, String type, Vector3f translation, Quaternion rot) {
        this.name = name;
        this.type = type;
        this.translation = translation;
        this.rot = rot;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Vector3f getTranslation() {
        return translation;
    }

    public Quaternion getRot() {
        return rot;
    }

    @Override
    public String toString() {
        return "ObjectData{" + "name=" + name + ", type=" + type + ", translation=" + translation + ", rot=" + rot + '}';
    }
}
