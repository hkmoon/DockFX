package org.dockfx;

import java.util.Map;

/**
 * To support the delayed open process for some specific applications, this interface implementation
 * is used.
 */
@FunctionalInterface
public interface DelayOpenHandler {
    DockNode open(String nodeId,
            String nodeTitle,
            double width,
            double height,
            Map<String, Object> properties);
}
