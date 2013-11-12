package busline3d;

import busline3d.appstate.StartMenuAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.niftygui.NiftyJmeDisplay;
import de.lessvoid.nifty.Nifty;

/**
 *
 * @author Volker Schuller
 */
public class BusLine3D extends SimpleApplication {

    public static void main(String[] args) {
        BusLine3D app = new BusLine3D();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        setPauseOnLostFocus(false);
        StartMenuAppState startMenu = new StartMenuAppState();
        stateManager.attach(startMenu);
        NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(
                assetManager, inputManager, audioRenderer, guiViewPort);
        Nifty nifty = niftyDisplay.getNifty();
        nifty.fromXml("Interface/screen.xml", "start", startMenu);
        guiViewPort.addProcessor(niftyDisplay);
        flyCam.setEnabled(false);
        this.setDisplayFps(false);
        this.setDisplayStatView(false);
    }
}
