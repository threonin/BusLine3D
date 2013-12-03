package busline3d.control;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

/**
 *
 * @author Volker Schuller
 */
public class PassengerControl extends AbstractControl {

    private String[] passengers;

    public PassengerControl(String[] passengers) {
        this.passengers = passengers;
    }

    public String[] getPassengers() {
        return passengers;
    }

    public void setPassengers(String[] passengers) {
        this.passengers = passengers;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
    }

    @Override
    protected void controlUpdate(float tpf) {
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
}
