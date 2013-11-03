package util.control;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 *
 * @author Volker Schuller
 */
public class LocRotTime {

    private float time;
    private Vector3f translation;
    private Quaternion rot;

    public LocRotTime(float time, Vector3f translation, Quaternion rot) {
        this.time = time;
        this.translation = translation;
        this.rot = rot;
    }

    public float getTime() {
        return time;
    }

    public Vector3f getTranslation() {
        return translation;
    }

    public Quaternion getRot() {
        return rot;
    }

    @Override
    public String toString() {
        return "ObjectData{" + "time=" + time + ", translation=" + translation + ", rot=" + rot + '}';
    }
}
