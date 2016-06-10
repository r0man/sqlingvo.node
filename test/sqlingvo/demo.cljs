(ns sqlingvo.node-demo
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async]
            [cljs.pprint :refer [pprint]]
            [sqlingvo.core :as sql]
            [sqlingvo.node :as db :refer-macros [<? <!?]]))

(def db (db/db "postgres://tiger:scotch@localhost/sqlingvo_node"))

(go (let [db (<? (db/connect db))]
      (pprint (<!? (sql/select db [:*]
                     (sql/from :information_schema.tables)
                     (sql/where '(= :table_name "pg_statistic")))))
      (db/disconnect db)))
