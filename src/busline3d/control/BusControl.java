package busline3d.control;

import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.LightControl;

/**
 *
 * @author Volker Schuller
 */
public class BusControl extends AbstractControl {

    private Node parent;
    private SpotLight light1;
    private SpotLight light2;

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
        parent = spatial.getParent();
        light1 = addLight(1);
        light2 = addLight(-1);
    }

    private SpotLight addLight(float posx) {
        SpotLight spot = new SpotLight();
        spot.setSpotRange(200f);
        spot.setSpotInnerAngle(20f * FastMath.DEG_TO_RAD);
        spot.setSpotOuterAngle(30f * FastMath.DEG_TO_RAD);
        spot.setColor(ColorRGBA.White.mult(2f));
        LightControl lightControl = new LightControl(spot);
        Node headlight = new Node();
        headlight.setLocalTranslation(posx, 2, 5);
        headlight.setLocalRotation(new Quaternion(new float[]{-FastMath.PI * 0.4f, 0, 0}));
        ((Node) spatial).attachChild(headlight);
        headlight.addControl(lightControl);
        return spot;
    }

    public void switchLights(boolean on) {
        if (on) {
            parent.addLight(light1);
            parent.addLight(light2);

        } else {
            parent.removeLight(light1);
            parent.removeLight(light2);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            parent.addLight(light1);
            parent.addLight(light2);
        } else {
            parent.removeLight(light1);
            parent.removeLight(light2);
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
}
