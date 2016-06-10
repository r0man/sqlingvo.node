(ns sqlingvo.node)

(defmacro <?
  "Get a value from `channel`. If the value is an exception throw it,
  otherwise return the value."
  [channel]
  `(sqlingvo.node/throw-err (cljs.core.async/<! ~channel)))

(defmacro <!?
  "Execute the SQL `statement` and take a value from the returned
  channel. If the value is a JavaScript error throw it, otherwise
  return the value."
  [statement & [opts]]
  `(sqlingvo.node/<? (sqlingvo.node/execute ~statement ~opts)))
