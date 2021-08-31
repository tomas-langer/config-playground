package jakarta.config.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

class KeyImpl implements Key {
    private final String name;
    private final KeyImpl parent;
    private final List<String> path;
    private final String fullKey;

    private KeyImpl(KeyImpl parent, String name) {
        Objects.requireNonNull(name, "name is mandatory");

        if (name.contains(".")) {
            throw new IllegalArgumentException("Illegal key token format. Dot character ('.') is not supported.");
        }

        this.parent = parent;
        List<String> path = new ArrayList<>();
        StringBuilder fullSB = new StringBuilder();
        if (parent != null) {
            path.addAll(parent.path);
            fullSB.append(parent.fullKey);
        }
        if (!name.isEmpty()) {
            if (fullSB.length() > 0) {
                fullSB.append(".");
            }
            path.add(name);
            fullSB.append(name);
        }
        this.name = Key.unescapeName(name);
        this.path = Collections.unmodifiableList(path);
        this.fullKey = fullSB.toString();
    }

    @Override
    public KeyImpl parent() {
        if (isRoot()) {
            throw new IllegalStateException("Attempting to get parent of a root node. Guard by isRoot instead");
        }
        return parent;
    }

    @Override
    public boolean isRoot() {
        return (null == parent);
    }

    /**
     * Creates new root instance of KeyImpl.
     *
     * @return new instance of KeyImpl.
     */
    static KeyImpl of() {
        return new KeyImpl(null, "");
    }

    /**
     * Creates new instance of KeyImpl.
     *
     * @param key key
     * @return new instance of KeyImpl.
     */
    static KeyImpl of(String key) {
        return of().child(key);
    }

    /**
     * Creates new child instance of KeyImpl.
     *
     * @param key sub-key
     * @return new child instance of KeyImpl.
     */
    KeyImpl child(String key) {
        return child(Arrays.asList(key.split("\\.")));
    }

    /**
     * Creates new child instance of KeyImpl.
     *
     * @param key sub-key
     * @return new child instance of KeyImpl.
     */
    @Override
    public KeyImpl child(Key key) {
        final List<String> path;
        if (key instanceof KeyImpl) {
            path = ((KeyImpl) key).path;
        } else {
            path = new LinkedList<>();
            while (!key.isRoot()) {
                path.add(0, key.name());
                key = key.parent();
            }
        }
        return child(path);
    }

    private KeyImpl child(List<String> path) {
        KeyImpl result = this;
        for (String name : path) {
            if (name.isEmpty()) {
                continue;
            }
            result = new KeyImpl(result, name);
        }
        return result;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return fullKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KeyImpl key = (KeyImpl) o;
        return Objects.equals(name, key.name)
            && Objects.equals(parent, key.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parent);
    }

    @Override
    public int compareTo(Key that) {
        return toString().compareTo(that.toString());
    }
}
