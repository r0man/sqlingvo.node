(ns sqlingvo.node.async
  (:require [clojure.spec.alpha :as s]
            [cljs.core.async :as async]
            [cljs.nodejs :as node]
            [goog.object :as gobj]
            [sqlingvo.core :as sql]
            [sqlingvo.db :as db]
            [sqlingvo.node.driver :as driver]
            [sqlingvo.util :as util]))

(def pg
  "The Node.js PostgreSQL client."
  (let [package (node/require "pg")]
    (or (.-native package) package)))

(def Pool
  "The database connection pool."
  (.-Pool pg))

(def Client
  "The database client."
  (.-Client pg))

(defn throw-err
  "If `x` is a JavaScript error throw it, otherwise return `x`."
  [x]
  (if (instance? js/Error x)
    (throw x)
    x))

(defn connected?
  "Returns true if `db` is connected to the database, otherwise false."
  [db]
  (some? (:connection db)))

(defn- config
  "Returns the `db` config."
  [db]
  {:connectionTimeoutMillis (:connection-timeout-millis db)
   :database (:name db)
   :host (:server-name db)
   :idleTimeoutMillis (:idle-timeout-millis db)
   :max? (:max? db)
   :password (:password db)
   :port (:server-port db)
   :user (:username db)})

(defn- connect-client
  "Connect to the database using a new client."
  [db]
  (let [channel (async/chan)
        client (Client. (clj->js (config db)))]
    (->> (fn [error]
           (if error
             (do (async/put! channel error)
                 (async/close! channel))
             (->> (assoc db :connection client)
                  (async/put! channel))))
         (.connect client))
    channel))

(defn- connect-pool
  "Connect to the database using a pool."
  [db]
  (let [channel (async/chan)]
    (->> (fn [error client]
           (->> (or error (assoc db :connection client))
                (async/put! channel))
           (async/close! channel))
         (.connect (:pool db)))
    channel))

(defn- disconnect-client
  "Disconnect a client from the database."
  [db]
  (let [channel (async/chan)]
    (->> (fn [error]
           (->> (or error (dissoc db :connection))
                (async/put! channel))
           (async/close! channel))
         (.end (:connection db)))
    channel))

(defn- disconnect-pool
  "Disconnect a client from the database pool."
  [db]
  (let [channel (async/chan)]
    (try (.release (:connection db))
         (->> (dissoc db :connection)
              (async/put! channel))
         (catch js/Error error
           (async/put! channel error))
         (finally
           (async/close! channel)))
    channel))

(defn- to-rows
  "Convert the `result-set` into a seq of Clojure maps."
  [db result-set]
  (when result-set
    (mapv #(driver/to-row db %1) (.-rows result-set))))

(defn start
  "Start the `db` connection pool."
  [db]
  (let [channel (async/chan)]
    (try (let [pool (Pool. (clj->js (config db)))]
           (->> (assoc db :pool pool)
                (async/put! channel)))
         (catch js/Error e
           (async/put! channel e))
         (finally
           (async/close! channel)))
    channel))

(defn stop
  "Stop the `db` connection pool."
  [db]
  (let [channel (async/chan)]
    (->> (fn [error]
           (->> (or error (dissoc db :pool))
                (async/put! channel))
           (async/close! channel))
         (.end (:pool db)))
    channel))

(defn connect
  "Connect to the database."
  [db]
  (if (:pool db)
    (connect-pool db)
    (connect-client db)))

(defn disconnect
  "Disconnect from the database."
  [db]
  (if (:pool db)
    (disconnect-pool db)
    (disconnect-client db)))

(defn execute
  "Execute `stmt` against a database and return a channel that will
  contain the results or an error."
  [stmt & [opts]]
  (let [{:keys [db] :as ast} (sql/ast stmt)
        [sql & args] (sql/sql ast)]
    (assert (connected? db))
    (let [channel (async/chan)]
      (.query (:connection db) sql (into-array args)
              (fn [error results]
                (try (if error
                       (async/put! channel error)
                       (->> (to-rows db results)
                            (async/put! channel)))
                     (finally (async/close! channel)))))
      channel)))

(defn db
  "Return an new database."
  [spec & [opts]]
  (let [db (sql/db spec opts)]
    (->> {:eval-fn execute
          :sql-placeholder util/sql-placeholder-count}
         (merge db))))

(s/fdef db :args (s/cat :db any? :opts (s/? (s/nilable map?))))
