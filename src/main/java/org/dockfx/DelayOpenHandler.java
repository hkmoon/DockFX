package org.dockfx;

/**
 * To support the delayed open process for some specific applications, this interface implementation
 * is used.
 */
@FunctionalInterface
public interface DelayOpenHandler {
    DockNode open(String nodeName, double width, double height);
}
