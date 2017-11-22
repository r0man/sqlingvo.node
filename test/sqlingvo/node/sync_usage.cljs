(ns sqlingvo.node.sync-usage
  (:require [cljs.pprint :refer [pprint]]
            [sqlingvo.core :as sql]
            [sqlingvo.node.sync :as db]))

(def db (db/db "postgresql://tiger:scotch@localhost/sqlingvo_node"))

(pprint @(sql/select db [:*]
           (sql/from :information_schema.tables)
           (sql/where '(= :table_name "pg_statistic"))))
