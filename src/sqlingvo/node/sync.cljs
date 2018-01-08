(ns sqlingvo.node.sync
  (:require [cljs.nodejs :as node]
            [clojure.spec.alpha :as s]
            [sqlingvo.core :as sql]
            [sqlingvo.node.driver :as driver]
            [sqlingvo.url :as url]
            [sqlingvo.util :as util]))

(def Client
  "The native Node.js PostgreSQL client."
  (node/require "pg-native"))

(defrecord PgSyncDriver [connection db]
  driver/Driver
  (-close [driver]
    (some-> connection .-pq .finish)
    (assoc driver :connection nil))

  (-connection [driver]
    connection)

  (-execute [driver [sql & args] opts]
    {:pre connection}
    (.prepareSync connection sql sql (count args))
    (->> (.executeSync connection sql (clj->js args))
         (mapv #(driver/to-row db %))))

  (-open [driver opts]
    (let [connection (Client.)]
      (.connectSync connection (url/format db))
      (assoc driver :connection connection)))

  (-query [driver [sql & args] opts]
    {:pre connection}
    (->> (.querySync connection sql (clj->js args))
         (mapv #(driver/to-row db %)))))

(extend-protocol IPrintWithWriter
  PgSyncDriver
  (-pr-writer [this writer opts]
    (-write writer "#<sqlingvo.node.sync.PgSyncDriver>")))

(defn db
  "Returns a new database that uses the pg-sync driver."
  [spec & [opts]]
  (let [db (sql/db spec opts)]
    (merge db {:eval-fn driver/execute
               :driver (map->PgSyncDriver {:db db})
               :sql-placeholder util/sql-placeholder-count})))

(s/fdef db :args (s/cat :db any? :opts (s/? (s/nilable map?))))
