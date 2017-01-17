package org.dockfx;

import java.util.LinkedList;
import java.util.Properties;

/**
 * ContentHolder has common functions for storing persistent object for node
 *
 * @author HongKee Moon
 */
public class ContentHolder {
    /**
     * The enum ContentHolder Type.
     */
    public enum Type {
        /**
         * The SplitPane.
         */
        SplitPane,
        /**
         * The TabPane.
         */
        TabPane,
        /**
         * The Collection.
         */
        Collection,
        /**
         * The DockNode.
         */
        DockNode
    }

    private Properties properties = new Properties();
    private LinkedList children = new LinkedList();
    private Type type;

    public ContentHolder() {

    }

    public ContentHolder(Type type) {
        this.type = type;
    }

    public void addProperty(Object key, Object value) {
        properties.put(key, value);
    }

    public void addChild(Object child) {
        children.add(child);
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public LinkedList getChildren() {
        return children;
    }

    public void setChildren(LinkedList children) {
        this.children = children;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
