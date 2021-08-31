package jakarta.config.impl;

import jakarta.config.spi.ConfigContent;
import jakarta.config.spi.ConfigNode;

class NodeContentImpl implements ConfigContent.NodeContent {

    private final ConfigNode.ObjectNode data;
    private final Object stamp;

    NodeContentImpl(ConfigNode.ObjectNode data) {
        this.data = data;
        this.stamp = null;
    }

    NodeContentImpl(ConfigNode.ObjectNode data, Object stamp) {
        this.data = data;
        this.stamp = stamp;
    }

    @Override
    public ConfigNode.ObjectNode data() {
        return data;
    }
}
