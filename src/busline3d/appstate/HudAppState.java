package busline3d.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.scene.Spatial;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

/**
 *
 * @author Volker Schuller
 */
public class HudAppState extends AbstractAppState implements ScreenController {

    private Nifty nifty;
    private Screen screen;
    private AppStateManager stateManager;
    private Spatial station;
    private Label stationname;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.stateManager = stateManager;
    }

    public void bind(Nifty nifty, Screen screen) {
        this.nifty = nifty;
        this.screen = screen;
        stationname = screen.findNiftyControl("stationname", Label.class);
    }

    public void onStartScreen() {
    }

    public void onEndScreen() {
    }

    public void setStation(Spatial station) {
        this.station = station;
        stationname.setText((String) station.getUserData("stationname"));
    }
}
