(ns sqlingvo.node.driver)

(defmacro with-connection
  "Open a database connection, bind the connected `db` to `db-sym`,
  evaluate `body` and close the connection again."
  [[db-sym db & [opts]] & body]
  `(sqlingvo.node.driver/with-connection*
     ~db (fn [~db-sym] ~@body) ~opts))
