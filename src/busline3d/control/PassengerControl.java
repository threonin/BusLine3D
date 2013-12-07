package busline3d.control;

import busline3d.appstate.WorldAppState;
import busline3d.command.SetMaterialCommand;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.control.AbstractControl;

/**
 *
 * @author Volker Schuller
 */
public class PassengerControl extends AbstractControl {

    private String[] passengers;
    private Geometry[] geometries;
    private WorldAppState worldAppState;
    private Geometry[] geometries2;

    public PassengerControl(Geometry[] geometries, WorldAppState worldAppState, boolean doubleSided) {
        this.passengers = new String[geometries.length];
        this.geometries = geometries;
        this.worldAppState = worldAppState;
        if (doubleSided) {
            geometries2 = new Geometry[geometries.length];
            int i = 0;
            for (Geometry geometry : geometries) {
                geometries2[i] = geometry.clone(false);
                geometries2[i].rotate(0, FastMath.PI, 0);
                geometries2[i].move(1.5f, 0, 0);
                geometry.getParent().attachChild(geometries2[i++]);
            }
        }
    }

    public String[] getPassengers() {
        return passengers;
    }

    public Geometry[] getGeometries() {
        return geometries;
    }

    public void addPassenger(String name) {
        for (int i = 0; i < passengers.length; i++) {
            if (passengers[i] == null) {
                addPassenger(i, name);
                return;
            }
        }
    }

    public void addPassenger(int index, String name) {
        passengers[index] = name;
        Material material = worldAppState.getMaterialForPassenger(name);
        if (material == null) {
            worldAppState.addCommand(new SetMaterialCommand(worldAppState, geometries[index], name));
            if (geometries2 != null) {
                worldAppState.addCommand(new SetMaterialCommand(worldAppState, geometries2[index], name));
            }
            return;
        }
        geometries[index].setMaterial(material);
        if (geometries2 != null) {
            geometries2[index].setMaterial(material);
        }
    }

    public void removePassenger(int index) {
        passengers[index] = null;
        geometries[index].setMaterial(worldAppState.getGlass());
        if (geometries2 != null) {
            geometries2[index].setMaterial(worldAppState.getGlass());
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
}
