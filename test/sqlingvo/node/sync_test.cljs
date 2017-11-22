(ns sqlingvo.node.sync-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [sqlingvo.core :as sql]
            [sqlingvo.node.driver :as driver]
            [sqlingvo.node.sync :as sync]))

(def db-spec
  "postgresql://tiger:scotch@localhost/sqlingvo_node")

(defn underscore [s]
  (str/replace (name s) "-" "_"))

(defn hypenate [s]
  (str/replace (name s) "_" "-"))

(def db
  (sync/db db-spec
           {:sql-name underscore
            :sql-identifier hypenate}))

(deftest test-db
  (let [db (sync/db db-spec)]
    (is (sql/db? db))
    (is (instance? sync/PgSyncDriver (:driver db)))
    (is (= (:eval-fn db) driver/execute))))

(deftest test-open
  (let [db (driver/open (sync/db db-spec))]
    (is (sql/db? db))
    (is (driver/connection db))
    (is (instance? sync/PgSyncDriver (:driver db)))
    (is (= (:eval-fn db) driver/execute))))

(deftest test-query-protocol-select-1
  (let [db (driver/open (sync/db db-spec))]
    (is (= (driver/-query (:driver db) ["SELECT 1"] nil)
           [{:?column? 1}]))))

(deftest test-execute-protocol-vacuum
  (let [db (driver/open (sync/db db-spec))]
    (is (= (driver/-execute (:driver db) ["VACUUM"] nil) []))))

(deftest test-query-driver
  (let [db (sync/db db-spec)]
    (is (= @(sql/select db [1])
           [{:?column? 1}]))))

(deftest test-execute-driver
  (let [db (sync/db db-spec)]
    (is (= @(sql/drop-table db [:test-execute-driver]
              (sql/if-exists true))
           []))))

(deftest test-hyphenate
  (let [db (driver/open db)]
    (is (= @(sql/select db [:*]
              (sql/from :information-schema.tables)
              (sql/where '(= :table-name "pg_statistic")))
           [{:commit-action nil,
             :reference-generation nil,
             :is-typed "NO",
             :is-insertable-into "YES",
             :table-catalog "sqlingvo_node",
             :user-defined-type-schema nil,
             :user-defined-type-name nil,
             :user-defined-type-catalog nil,
             :table-schema "pg_catalog",
             :self-referencing-column-name nil,
             :table-name "pg_statistic",
             :table-type "BASE TABLE"}]))))
