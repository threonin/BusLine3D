package busline3d.command;

import busline3d.appstate.WorldAppState;

/**
 *
 * @author Volker Schuller
 */
public class SetRadiusCommand implements Command {

    private WorldAppState worldAppState;
    private float radius;

    public SetRadiusCommand(WorldAppState worldAppState, float radius) {
        this.worldAppState = worldAppState;
        this.radius = radius;
    }

    public boolean execute() {
        worldAppState.setRadius(radius);
        return true;
    }
}
