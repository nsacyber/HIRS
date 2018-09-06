package hirs.collector;

import org.springframework.plugin.core.Plugin;

/**
 * Interface representing a Spring Plugin that is a HIRS {@link Collector}.
 * Collector Plugins are loaded by the CollectorPluginManager at runtime
 * through the Spring Plugin Framework. The Plugin&lt;T&gt;.supports() method must be implemented,
 * but the plugin delimiter feature is not currently used in this system.
 */
public interface CollectorPlugin extends Collector, Plugin<String> {
}
