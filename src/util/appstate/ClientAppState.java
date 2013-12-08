package util.appstate;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.message.DefinitionMessage;
import util.control.InterpolationControl;
import util.control.LocRotTime;
import util.message.MovementMessage;
import util.message.ObjectData;
import util.message.ObjectDefinition;
import util.message.ResetTimerMessage;

/**
 *
 * @author Volker Schuller
 */
public class ClientAppState extends AbstractAppState implements MessageListener<Client> {

    private SimpleApplication app;
    private Node rootNode;
    private Map<String, ConcurrentLinkedQueue<LocRotTime>> coming = new HashMap<String, ConcurrentLinkedQueue<LocRotTime>>();
    private Client client;
    private final static float LATENCY = 0.1f;
    private ConcurrentLinkedQueue<Spatial> newSpatials = new ConcurrentLinkedQueue<Spatial>();
    private MessageHandler messageHandler;
    private String host;
    private int port;
    private String error;

    public ClientAppState(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        this.rootNode = this.app.getRootNode();
        try {
            client = Network.connectToServer(host, port);
            client.start();
            client.addMessageListener(this);
        } catch (IOException ex) {
            Logger.getLogger(ClientAppState.class.getName()).log(Level.SEVERE, null, ex);
            error = ex.getLocalizedMessage();
        }
    }

    @Override
    public void update(float tpf) {
        Spatial newSpatial = newSpatials.poll();
        while (newSpatial != null) {
            rootNode.attachChild(newSpatial);
            newSpatial = newSpatials.poll();
        }
    }

    @Override
    public void cleanup() {
        try {
            client.close();
        } catch (Exception ex) {
        }
        super.cleanup();
    }

    public void messageReceived(Client source, Message message) {
        if (messageHandler != null) {
            if (messageHandler.messageReceived(source, message)) {
                return;
            }
        }
        if (message instanceof MovementMessage) {
            MovementMessage mm = (MovementMessage) message;
            for (ObjectData data : mm.getData()) {
                ConcurrentLinkedQueue<LocRotTime> queue = coming.get(data.getName());
                if (queue == null) {
                    Logger.getLogger(ClientAppState.class.getName()).log(Level.WARNING, "Object not found {0}", data.getName());
                    continue;
                }
                float servertime = mm.getServertime();
                if (servertime > 0) {
                    servertime += LATENCY;
                }
                queue.add(new LocRotTime(servertime, data.getTranslation(), data.getRot()));
            }
        } else if (message instanceof DefinitionMessage) {
            for (ObjectDefinition definition : ((DefinitionMessage) message).getDefinitions()) {
                Spatial model = (Spatial) app.getAssetManager().loadModel(definition.getType());
                Node object = new Node(definition.getName());
                object.attachChild(model);
                object.setLocalRotation(definition.getRot());
                object.setLocalTranslation(definition.getTranslation());
                observeSpatial(definition.getName(), object);
                newSpatials.add(object);
            }
        } else if (message instanceof ResetTimerMessage) {
            this.app.getTimer().reset();
            for (Spatial spatial : this.rootNode.getChildren()) {
                InterpolationControl control = spatial.getControl(InterpolationControl.class);
                if (control != null) {
                    control.reset();
                }
            }
        }
    }

    public void observeSpatial(String name, Spatial object) {
        ConcurrentLinkedQueue<LocRotTime> queue = new ConcurrentLinkedQueue<LocRotTime>();
        coming.put(name, queue);
        object.addControl(new InterpolationControl(queue, this.app.getTimer()));
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    public void sendMessage(Message message) {
        client.send(message);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getError() {
        return error;
    }

    public interface MessageHandler {

        public boolean messageReceived(Client source, Message message);
    }
}
