package jakarta.config.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.config.Config;
import jakarta.config.spi.ConfigNode.ListNode;

public class ConfigListImpl extends ConfigComplexImpl<ListNode> {

    ConfigListImpl(KeyImpl prefix,
                   KeyImpl key,
                   ListNode listNode,
                   ConfigFactory factory,
                   ConversionSupport mapperManager) {
        super(Type.LIST, prefix, key, listNode, factory, mapperManager);
    }


    @Override
    public Optional<List<Config>> asNodeList() {
        return Optional.of(IntStream.range(0, node().size())
            .boxed()
            .map(index -> get(Integer.toString(index)))
            .collect(Collectors.toList()));
    }

    @Override
    public String toString() {
        return "[" + realKey() + "] LIST (elements: " + node().size() + ")";
    }
}
