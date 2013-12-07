package busline3d.command;

import busline3d.appstate.WorldAppState;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;

/**
 *
 * @author Volker Schuller
 */
public class SetMaterialCommand implements Command {

    private WorldAppState worldAppState;
    private Geometry geometry;
    private String passengerName;

    public SetMaterialCommand(WorldAppState worldAppState, Geometry geometry, String passengerName) {
        this.worldAppState = worldAppState;
        this.passengerName = passengerName;
    }

    public boolean execute() {
        Material material = worldAppState.getMaterialForPassenger(passengerName);
        if (material == null) {
            return false;
        }
        geometry.setMaterial(material);
        return true;
    }
}
