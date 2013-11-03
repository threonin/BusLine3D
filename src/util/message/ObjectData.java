package util.message;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.serializing.Serializable;

/**
 *
 * @author Volker Schuller
 */
@Serializable
public class ObjectData {

    private String name;
    private Vector3f translation;
    private Quaternion rot;

    public ObjectData() {
    }

    public ObjectData(String name, Vector3f translation, Quaternion rot) {
        this.name = name;
        this.translation = translation;
        this.rot = rot;
    }

    public String getName() {
        return name;
    }

    public Vector3f getTranslation() {
        return translation;
    }

    public Quaternion getRot() {
        return rot;
    }

    @Override
    public String toString() {
        return "ObjectData{" + "name=" + name + ", translation=" + translation + ", rot=" + rot + '}';
    }
}
