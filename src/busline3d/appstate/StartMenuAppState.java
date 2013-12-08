package busline3d.appstate;

import busline3d.message.NewPassengerMessage;
import busline3d.message.PassengerBusMessage;
import busline3d.message.PassengerStationMessage;
import busline3d.message.RadiusMessage;
import busline3d.message.SetNameMessage;
import busline3d.message.WatchThisMessage;
import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.network.serializing.Serializer;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.appstate.ClientAppState;
import util.appstate.ServerAppState;
import util.message.DefinitionMessage;
import util.message.MovementMessage;
import util.message.ObjectData;
import util.message.ObjectDefinition;
import util.message.ResetTimerMessage;

/**
 *
 * @author Volker Schuller
 */
public class StartMenuAppState extends AbstractAppState implements ScreenController {

    private Nifty nifty;
    private Screen screen;
    private AppStateManager stateManager;
    private String host;
    private int port;
    private String stationname;
    private String error;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.stateManager = stateManager;
    }

    public void bind(Nifty nifty, Screen screen) {
        this.nifty = nifty;
        this.screen = screen;
        if (screen.getScreenId().equals("client") && host != null) {
            screen.findNiftyControl("host", TextField.class).setText(host);
            screen.findNiftyControl("port", TextField.class).setText(port + "");
            screen.findNiftyControl("stationname", TextField.class).setText(stationname);
            screen.findNiftyControl("lblError", Label.class).setText(error);
        }
    }

    public void onStartScreen() {
    }

    public void onEndScreen() {
    }

    public void singleplayer() {
        nifty.removeScreen("start");
        startServer(0);
    }

    public void serverdialog() {
        nifty.gotoScreen("server");
    }

    public void server() {
        getPort();
        if (port != 0) {
            nifty.removeScreen("server");
            initSerializer();
            startServer(port);
        }
    }

    private void startServer(int port) {
        stateManager.attach(new WorldAppState());
        BulletAppState bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        stateManager.attach(new ServerAppState(port));
        stateManager.attach(new DriveBusAppState());
        stateManager.attach(new BusServerAppState(port == 0));
        stateManager.detach(this);
    }

    public void clientdialog() {
        nifty.gotoScreen("client");
    }

    public void setClientData(String host, int port, String stationname, String error) {
        this.host = host;
        this.port = port;
        this.stationname = stationname;
        this.error = error;
    }

    public void client() {
        getPort();
        if (port != 0) {
            nifty.removeScreen("client");
            String host = screen.findNiftyControl("host", TextField.class).getRealText();
            String stationname = screen.findNiftyControl("stationname", TextField.class).getRealText();
            initSerializer();
            stateManager.attach(new WorldAppState());
            stateManager.attach(new ClientAppState(host, port));
            BusClientAppState busClientAppState = new BusClientAppState(stationname);
            stateManager.attach(busClientAppState);
            nifty.fromXml("Interface/station_hud.xml", "station_hud", busClientAppState);
        }
    }

    private void getPort() {
        try {
            port = Integer.parseInt(screen.findNiftyControl("port", TextField.class).getRealText());
        } catch (NumberFormatException ex) {
            Logger.getLogger(BusClientAppState.class.getName()).log(Level.SEVERE, "invalid number!");
        }
    }

    private void initSerializer() {
        Serializer.registerClass(ObjectData.class);
        Serializer.registerClass(MovementMessage.class);
        Serializer.registerClass(ObjectDefinition.class);
        Serializer.registerClass(DefinitionMessage.class);
        Serializer.registerClass(ResetTimerMessage.class);
        Serializer.registerClass(RadiusMessage.class);
        Serializer.registerClass(WatchThisMessage.class);
        Serializer.registerClass(SetNameMessage.class);
        Serializer.registerClass(NewPassengerMessage.class);
        Serializer.registerClass(PassengerBusMessage.class);
        Serializer.registerClass(PassengerStationMessage.class);
    }
}
