(ns clj-helix.manager-test
  (:require [clojure.test :refer :all]
            [clj-helix.logging :refer [mute]]
            [clj-helix.fsm :refer [fsm]]
            [clj-helix.admin-test :refer [fsm-def]]
            [clj-helix.manager :refer :all]))

(use-fixtures :once #(mute (%)))

(deftest manager-test
  (let [fsm (fsm fsm-def
                 (:slave :offline [p m c]
                         (println p "offline"))

                 (:offline :slave [p m c]
                           (println p "coming up...")
                           (Thread/sleep 1000)
                           (println p "online as slave"))

                 (:master :slave [p m c]
                          (println p "demoting...")
                          (Thread/sleep 1000)
                          (println p "demoted"))

                 (:slave :master [p m c]
                         (println p "promoting...")
                         (Thread/sleep 1000)
                         (println p "promoted")))

        instance {:zookeeper "localhost:2181"
                  :cluster   :helix-test
                  :instance  {:host "localhost"}}
        c (controller (assoc-in instance [:instance :port] 7000))

        instance (assoc instance :fsm fsm)
        p1 (participant (assoc-in instance [:instance :port] 7001))
        p2 (participant (assoc-in instance [:instance :port] 7002))]
    
    (Thread/sleep 5000)

    (disconnect! p1)
    (Thread/sleep 4000)

    (prn "shutting down.")
    (dorun (map disconnect! [p1 p2 c]))))
