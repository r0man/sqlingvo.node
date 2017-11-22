(ns sqlingvo.node.driver
  (:require [clojure.spec.alpha :as s]
            [goog.object :as gobj]
            [sqlingvo.core :as sql]
            [sqlingvo.db :as db]
            [sqlingvo.url :as url])
  (:require-macros [sqlingvo.node.driver :refer [with-connection]]))

(defprotocol Driver
  (-close [driver])
  (-connection [driver])
  (-execute [driver sql opts])
  (-open [driver opts])
  (-query [driver sql opts]))

(defn to-row
  "Convert the JavaScript `obj` to a ClojureScript row."
  [db obj]
  (let [convert (or (:sql-identifier db) identity)]
    (->> (reduce #(assoc! %1 (keyword (convert %2)) (gobj/get obj %2))
                 (transient {})
                 (gobj/getKeys obj))
         (persistent! ))))

(s/fdef to-row
  :args (s/cat :db sql/db? :obj any?))

(defmulti ^:private protocol-method
  "Returns the protocol method used to query or execute the SQL
  statement of `ast`, either `-query` or `-execute`."
  (fn [ast] (-> ast :op keyword)))

(defn open
  "Open the database."
  [db & [opts]]
  (-open db opts))

(s/fdef open
  :args (s/cat :db sql/db? :opts (s/? (s/nilable  map?))))

(defn connection
  "Returns the database connection if connected, nil otherwise."
  [db]
  (-connection db))

(s/fdef connection
  :args (s/cat :db sql/db?))

(defn connected?
  [db]
  (some? (-connection db)))

(s/fdef connected?
  :args (s/cat :db sql/db?))

(defn close
  "Close the database."
  [db]
  (-close db))

(s/fdef close
  :args (s/cat :db sql/db?))

(defn with-connection*
  "Open a database connection, call `f` with the connected `db` as
  argument and close the connection again."
  [db f & [opts]]
  (if (connection db)
    (f db)
    (let [db (open db opts)]
      (try (f db)
           (finally (close db))))))

(defn execute
  "Execute `statement` against a database."
  [statement & [opts]]
  (let [{:keys [db] :as ast} (sql/ast statement)]
    (with-connection [db db]
      (let [sql (sql/sql ast)
            protocol-method (protocol-method ast)]
        (with-meta
          (case (:op ast)
            :create-table
            (if (seq (rest sql))
              ;; TODO: sql-str only works with PostgreSQL driver
              ;; (protocol-method db (sql-str stmt) opts)
              (throw (ex-info "TODO: CREATE TABLE" {}))
              (protocol-method db sql opts))
            (protocol-method db sql opts))
          {:sqlingvo/execute-statement statement
           :sqlingvo/execute-statement-opts opts
           :datumbazo/stmt statement
           :datumbazo/opts opts})))))

(defmethod protocol-method :delete [ast]
  (if (:returning ast) -query -execute))

(defmethod protocol-method :except [_] -query)

(defmethod protocol-method :explain [_] -query)

(defmethod protocol-method :insert [ast]
  (if (:returning ast) -query -execute))

(defmethod protocol-method :intersect [_] -query)

(defmethod protocol-method :select [_] -query)

(defmethod protocol-method :union [_] -query)

(defmethod protocol-method :update [ast]
  (if (:returning ast) -query -execute))

(defmethod protocol-method :values [_] -query)

(defmethod protocol-method :with [ast]
  (protocol-method (:query ast)))

(defmethod protocol-method :default [ast] -execute)

(extend-type db/Database
  Driver
  (-close [db]
    (update db :driver -close))

  (-connection [db]
    (-connection (:driver db)))

  (-execute [db sql opts]
    (-execute (:driver db) sql opts))

  (-open [db opts]
    (update db :driver -open opts))

  (-query [db sql opts]
    (-query (:driver db) sql opts))

  IPrintWithWriter
  (-pr-writer [db writer opts]
    (-write writer (str "#<sqlingvo.db.Database: " (url/format db) ">"))))
