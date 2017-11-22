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
         (.connect pg #js {:database (:name db)
                           :host (:server-name db)
                           :password (:password db)
                           :port (:server-port db)
                           :user (:username db)}))
    channel))

(defn disconnect
  "Disconnect from the database."
  [db]
  (some-> db :connection (.end))
  (assoc db :connection nil))

(defn- to-rows
  "Convert the `result-set` into a seq of Clojure maps."
  [db result-set]
  (when result-set
    (mapv #(driver/to-row db %1) (.-rows result-set))))

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
    (merge db {:eval-fn execute
               :sql-placeholder util/sql-placeholder-count})))

(s/fdef db :args (s/cat :db any? :opts (s/? (s/nilable map?))))
