(ns sqlingvo.node-test
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [clojure.string :as str])
  (:require [cljs.core.async :as async]
            [cljs.test :refer-macros [async deftest is]]
            [cljs.pprint :refer [pprint]]
            [clojure.string :as str]
            [sqlingvo.core :as sql]
            [sqlingvo.node :as node :refer-macros [<? <!?]]))

(def db (node/db "postgres://tiger:scotch@localhost/sqlingvo_node"))

(def country-data
  [{:code "de" :name "Germany"}
   {:code "es" :name "Spain"}])

(defn create-countries
  "Create the countries table."
  [db]
  (sql/create-table
      db :countries
    (sql/column :id :serial :primary-key? true)
    (sql/column :name :text :unique? true)
    (sql/column :code :text)))

(defn drop-countries
  "Drop the countries table."
  [db]
  (sql/drop-table
      db [:countries]
    (sql/if-exists true)))

(defn insert-countries
  "Drop the countries table."
  [db]
  (sql/insert
      db :countries []
    (sql/values country-data)
    (sql/returning :*)))

(defn countries
  "Return all countries."
  [db]
  (sql/select
      db [:*]
    (sql/from :countries)))

(deftest test-db
  (is (= (:url (node/db (:url db)))
         (:url db))))

(deftest test-connect
  (async done
    (go (let [db (<? (node/connect db))]
          (is (:connection db))
          (node/disconnect db)
          (done)))))

(deftest test-connect-error
  (async done
    (go (try [db (<? (node/connect "postgres://localhost/unknown_db"))]
             (assert false "Connection error expected.")
             (catch js/Error e
               (let [message (str/replace (.-message e) #"\n" "")]
                 (is (re-matches #".*does not exist.*" message))))
             (finally (done))))))

(deftest test-disconnect
  (async done
    (go (let [db (node/disconnect (<? (node/connect db)))]
          (is (nil? (:connection db)))
          (done)))))

(deftest test-run-queries
  (async done
    (go (let [db (<? (node/connect db))
              _ (<!? (drop-countries db))
              _ (<!? (create-countries db))]
          (is (= (<!? (insert-countries db))
                 (<!? (countries db))))
          _ (<!? (drop-countries db))
          (done)))))
