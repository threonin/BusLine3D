package busline3d.appstate;

import busline3d.control.PassengerControl;
import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.scene.Spatial;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.effects.EffectEventId;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.ImageRenderer;
import de.lessvoid.nifty.render.NiftyImage;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.util.HashMap;
import java.util.Map;

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
    private Element[] busImages = new Element[10];
    private boolean[] busPlaces = new boolean[10];
    private Element[] stationImages = new Element[10];
    private boolean[] stationPlaces = new boolean[8];
    private Map<String, NiftyImage> passengerImages = new HashMap<String, NiftyImage>();

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.stateManager = stateManager;
    }

    public void bind(Nifty nifty, Screen screen) {
        this.nifty = nifty;
        this.screen = nifty.getScreen("hud");;
        stationname = screen.findNiftyControl("stationname", Label.class);
        for (int i = 1; i <= 10; i++) {
            busImages[i - 1] = this.screen.findElementByName("b" + i);
        }
        for (int i = 1; i <= 8; i++) {
            stationImages[i - 1] = this.screen.findElementByName("s" + i);
        }
    }

    public void onStartScreen() {
    }

    public void onEndScreen() {
    }

    public void setStation(Spatial station) {
        if (this.station == station) {
            return;
        }
        this.station = station;
        stationname.setText((String) station.getUserData("stationname"));
        String[] passengers = station.getControl(PassengerControl.class).getPassengers();
        for (int i = 0; i < passengers.length; i++) {
            if (passengers[i] != null) {
                stationPlaces[i] = true;
                if (!passengerImages.containsKey(passengers[i])) {
                    passengerImages.put(passengers[i], nifty.createImage("Textures/passengers/" + passengers[i] + ".jpg", false));
                }
                stationImages[i].getRenderer(ImageRenderer.class).setImage(passengerImages.get(passengers[i]));
                if (insertPassengerIntoBus(passengers[i]) != -1) {
                    removePassengerFromStation(i);
                    passengers[i] = null;
                } else {
                    stationImages[i].stopEffectWithoutChildren(EffectEventId.onCustom);
                }
            } else {
                stationPlaces[i] = false;
                stationImages[i].getRenderer(ImageRenderer.class).setImage(null);
            }
        }
    }

    public int insertPassengerIntoBus(String passenger) {
        return insertPassengerIntoArray(passenger, busPlaces, busImages);
    }

    public void removePassengerFromBus(int i) {
        removePassengerFromArray(i, busPlaces, busImages);
    }

    public int insertPassengerIntoStation(String passenger) {
        return insertPassengerIntoArray(passenger, stationPlaces, stationImages);
    }

    public void removePassengerFromStation(int i) {
        removePassengerFromArray(i, stationPlaces, stationImages);
    }

    private int insertPassengerIntoArray(String passenger, boolean places[], Element images[]) {
        int nr = 0;
        for (boolean occupied : places) {
            if (!occupied) {
                break;
            }
            nr++;
        }
        if (nr == places.length) {
            return -1;
        }
        images[nr].getRenderer(ImageRenderer.class).setImage(passengerImages.get(passenger));
        images[nr].startEffectWithoutChildren(EffectEventId.onCustom, null, "onShow");
        places[nr] = true;
        return nr;
    }

    private void removePassengerFromArray(int i, boolean places[], Element images[]) {
        images[i].startEffectWithoutChildren(EffectEventId.onCustom, null, "onHide");
        places[i] = false;
    }
}
