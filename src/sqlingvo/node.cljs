(ns sqlingvo.node
  (:require [cljs.core.async :as async]
            [cljs.nodejs :as node]
            [goog.object :as gobj]
            [sqlingvo.core :as sql]
            [sqlingvo.db :as db]
            [sqlingvo.util :as util]))

(def pg
  "The Node.js PostgreSQL client."
  (let [package (node/require "pg")]
    (or (.-native package) package)))

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

(defn connect
  "Connect to the database."
  [db]
  (let [channel (async/chan)]
    (->> (fn [error connection done]
           (if error
             (do (async/put! channel error)
                 (async/close! channel))
             (->> (assoc db :connection connection)
                  (async/put! channel))))
         (.connect pg (:url db)))
    channel))

(defn disconnect
  "Disconnect from the database."
  [db]
  (some-> db :connection (.end))
  (assoc db :connection nil))

(defn- to-rows
  "Convert the `result-set` into a seq of Clojure maps."
  [result-set]
  (when result-set
    (map (fn [row]
           (reduce
            (fn [m k]
              (assoc m (keyword k) (gobj/get row k)))
            {} (gobj/getKeys row)))
         (.-rows result-set))))

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
                       (->> (to-rows results)
                            (async/put! channel)))
                     (finally (async/close! channel)))))
      channel)))

(defn db
  "Return an new database."
  [url]
  (db/postgresql
   {:eval-fn execute
    :sql-placeholder util/sql-placeholder-count
    :url url}))