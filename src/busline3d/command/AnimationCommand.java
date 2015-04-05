package busline3d.command;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.scene.Node;

/**
 *
 * @author Volker Schuller
 */
public class AnimationCommand implements Command {

    private Node rootNode;
    private String object;
    private String animation;
    private float speed;

    public AnimationCommand(Node rootNode, String object, String animation, float speed) {
        this.rootNode = rootNode;
        this.object = object;
        this.animation = animation;
        this.speed = speed;
    }

    public boolean execute() {
        Node node = (Node) rootNode.getChild(object);
        if (node == null) {
            return false;
        }
        node = (Node) node.getChild(object);
        AnimControl anim = node.getControl(AnimControl.class);

        AnimChannel channel;
        if (anim.getNumChannels() == 0) {
            channel = anim.createChannel();
        } else {
            channel = anim.getChannel(0);
        }
        channel.setAnim(animation);
        channel.setSpeed(speed);
        channel.setLoopMode(LoopMode.DontLoop);
        return true;
    }
}
