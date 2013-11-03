package util.message;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 *
 * @author Volker Schuller
 */
@Serializable
public class DefinitionMessage extends AbstractMessage {

    private ObjectDefinition[] definitions;

    public DefinitionMessage() {
    }

    public DefinitionMessage(ObjectDefinition[] definitions) {
        this.definitions = definitions;
    }

    public ObjectDefinition[] getDefinitions() {
        return definitions;
    }

    @Override
    public String toString() {
        String objects = "\n";
        for (ObjectDefinition obj : definitions) {
            objects += obj + "\n";
        }
        return "DefinitionMessage{data=[" + objects + "]}";
    }
}
