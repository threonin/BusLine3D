package busline3d.appstate;

import busline3d.control.HostedConnectionControl;
import busline3d.control.PassengerControl;
import busline3d.message.PassengerBusMessage;
import busline3d.message.PassengerStationMessage;
import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.FastMath;
import com.jme3.network.HostedConnection;
import com.jme3.scene.Spatial;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.effects.EffectEventId;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.ImageRenderer;
import de.lessvoid.nifty.render.NiftyImage;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import util.appstate.ServerAppState;

/**
 *
 * @author Volker Schuller
 */
public class HudAppState extends AbstractAppState implements ScreenController {

    private Nifty nifty;
    private Screen screen;
    private WorldAppState worldAppState;
    private ServerAppState serverAppState;
    private Spatial station;
    private Label stationname;
    private Element[] busImages = new Element[10];
    private boolean[] busPlaces = new boolean[10];
    private String[] busPassengers = new String[10];
    private Element[] stationImages = new Element[8];
    private boolean[] stationPlaces = new boolean[8];
    private Map<String, NiftyImage> passengerImages = new HashMap<String, NiftyImage>();
    private boolean singleplayer;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.worldAppState = stateManager.getState(WorldAppState.class);
        this.serverAppState = stateManager.getState(ServerAppState.class);
        this.singleplayer = stateManager.getState(BusServerAppState.class).getSingleplayer();
    }

    public void bind(Nifty nifty, Screen screen) {
        this.nifty = nifty;
        this.screen = nifty.getScreen("hud");
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
        String name = (String) station.getUserData("stationname");
        stationname.setText(name);
        PassengerControl stationControl = station.getControl(PassengerControl.class);
        HostedConnection connection = null;
        if (!singleplayer) {
            connection = station.getControl(HostedConnectionControl.class).getConnection();
        }
        String[] stationPassengers = stationControl.getPassengers();
        for (int i = 0; i < stationPassengers.length; i++) {
            if (stationPassengers[i] != null) {
                stationPlaces[i] = true;
                if (!passengerImages.containsKey(stationPassengers[i])) {
                    passengerImages.put(stationPassengers[i], nifty.createImage("Textures/passengers/" + stationPassengers[i] + ".jpg", false));
                }
                stationImages[i].getRenderer(ImageRenderer.class).setImage(passengerImages.get(stationPassengers[i]));
            } else {
                stationPlaces[i] = false;
                stationImages[i].getRenderer(ImageRenderer.class).setImage(null);
            }
        }
        ArrayList<Integer> exitPassengers = new ArrayList<Integer>();
        for (int i = 0; i < busPlaces.length; i++) {
            if (busPlaces[i] && FastMath.nextRandomInt(0, 3) == 0) {
                int sn = insertPassengerIntoStation(busPassengers[i]);
                if (sn == -1) {
                    break;
                }
                stationControl.addPassenger(sn, busPassengers[i]);
                if (!singleplayer) {
                    connection.send(new PassengerStationMessage(sn, busPassengers[i]).setReliable(true));
                }
                exitPassengers.add(sn);
                removePassengerFromBus(i);
            }
        }
        for (int i = 0; i < stationPlaces.length; i++) {
            if (stationPlaces[i] == true && (!exitPassengers.contains(i))) {
                if (insertPassengerIntoBus(stationPassengers[i]) != -1) {
                    removePassengerFromStation(i);
                    stationControl.removePassenger(i);
                    if (!singleplayer) {
                        connection.send(new PassengerStationMessage(i, null).setReliable(true));
                    }
                } else {
                    stationImages[i].stopEffectWithoutChildren(EffectEventId.onCustom);
                }
            }
        }
    }

    private int insertPassengerIntoBus(String passenger) {
        int i = insertPassengerIntoArray(passenger, busPlaces, busImages);
        if (i != -1) {
            busPassengers[i] = passenger;
            worldAppState.getPassengerControl().addPassenger(i, passenger);
            if (!singleplayer) {
                serverAppState.getServer().broadcast(new PassengerBusMessage(i, passenger).setReliable(true));
            }
        }
        return i;
    }

    private void removePassengerFromBus(int i) {
        removePassengerFromArray(i, busPlaces, busImages);
        busPassengers[i] = null;
        worldAppState.getPassengerControl().removePassenger(i);
        if (!singleplayer) {
            serverAppState.getServer().broadcast(new PassengerBusMessage(i, null).setReliable(true));
        }
    }

    private int insertPassengerIntoStation(String passenger) {
        return insertPassengerIntoArray(passenger, stationPlaces, stationImages);
    }

    private void removePassengerFromStation(int i) {
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
