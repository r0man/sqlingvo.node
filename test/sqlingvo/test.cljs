(ns sqlingvo.test
  (:require [doo.runner :refer-macros [doo-tests]]
            [sqlingvo.node-test]))

(doo-tests 'sqlingvo.node-test)
