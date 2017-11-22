(ns sqlingvo.node.async)

(defmacro <?
  "Get a value from `channel`. If the value is an exception throw it,
  otherwise return the value."
  [channel]
  `(sqlingvo.node.async/throw-err (cljs.core.async/<! ~channel)))

(defmacro <!?
  "Execute the SQL `statement` and take a value from the returned
  channel. If the value is a JavaScript error throw it, otherwise
  return the value."
  [statement & [opts]]
  `(sqlingvo.node.async/<? (sqlingvo.node.async/execute ~statement ~opts)))
