/**
 * This shows full set of SPIs to support
 *  - mutable configuration
 *  - parsable config sources
 *  - lazy config sources
 * based on tree structure.
 *
 * The config sources can be classified based on the following aspects:
 *
 * Eagerness
 * 1. Eager configuration sources - these know the full set of properties at a given point in time (file, classpath, URL with json)
 *  a) Parsable config source - provides a source of data to be parsed (file, classpath, git)
 *  b) Node config source - provides a source of data in a parsed form (Map, Properties, System props, env vars)
 * 2. Lazy configuration sources - sources that cannot load everything or do not have knowledge of full set of
 *  configuration keys (database with big number of records, JNDI etc.)
 *
 * Mutability
 * 1. Mutable configuration sources - these can watch/poll for changes and provide an updated set of data
 *  a) Pollable source - source that can determine whether a change occurred based regular polling requests (system props)
 *  b) Watchable source - source that can determine change based on a change watcher mechanism (not regular polling) - file system
 *  c) Event source - source generates its own change events through other means
 * 2. Immutable configuration sources - no changes are possible (such as classpath, environment variables)
 */
package jakarta.config.spi;
