package jakarta.config.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.config.Config;
import jakarta.config.spi.ConfigNode.ObjectNode;

class ConfigObjectImpl extends ConfigComplexImpl<ObjectNode> {

    ConfigObjectImpl(KeyImpl prefix,
                     KeyImpl key,
                     ObjectNode objectNode,
                     ConfigFactory factory,
                     ConversionSupport mapperManager) {
        super(Type.OBJECT, prefix, key, objectNode, factory, mapperManager);
    }

    @Override
    public Optional<List<Config>> asNodeList() {
        return Optional.of(node().keySet()
            .stream()
            .map(this::get)
            .collect(Collectors.toList()));
    }

    @Override
    public String toString() {
        return "[" + realKey() + "] OBJECT (members: " + node().size() + ")";
    }

}
