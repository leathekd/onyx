(ns onyx.system
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :refer [fatal]]
            [onyx.logging-configuration :as logging-config]
            [onyx.peer.virtual-peer :refer [virtual-peer]]
            [onyx.queue.hornetq :refer [hornetq]]
            [onyx.log.zookeeper :refer [zookeeper]]))

(def development-components [:logging-config :log :queue])

(def peer-components [:logging-config :log :queue :virtual-peer])

(defn rethrow-component [f]
  (try
    (f)
    (catch Exception e
      (fatal e)
      (throw (.getCause e)))))

(defrecord OnyxDevelopmentEnv []
  component/Lifecycle
  (start [this]
    (rethrow-component
     #(component/start-system this development-components)))
  (stop [this]
    (rethrow-component
     #(component/stop-system this development-components))))

(defrecord OnyxPeer []
  component/Lifecycle
  (start [this]
    (rethrow-component
     #(component/start-system this peer-components)))
  (stop [this]
    (rethrow-component
     #(component/stop-system this peer-components))))

(defn onyx-development-env
  [onyx-id config]
  (map->OnyxDevelopmentEnv
   {:logging-config (logging-config/logging-configuration onyx-id config)
    :log (component/using (zookeeper onyx-id config) [:logging-config])
    :queue (component/using (hornetq onyx-id config) [:log])}))

(defn onyx-peer
  [onyx-id config]
  (map->OnyxPeer
   {:logging-config (logging-config/logging-configuration onyx-id (:logging config))
    :log (component/using (zookeeper onyx-id config) [:logging-config])
    :queue (component/using (hornetq onyx-id config) [:log])
    :virtual-peer (component/using (virtual-peer config) [:log])}))

