(ns indef.core
  (:require
   [cider.nrepl.middleware.util.cljs :as cljs-utils]
   [cider.nrepl.middleware.util.error-handling :refer [base-error-response]]
   [indef.indef-impl :refer [handle-indef]]
   [nrepl.middleware :as middleware :refer [set-descriptor!]]
   [nrepl.middleware.caught :as caught]
   [nrepl.misc :refer [response-for] :as misc]
   [nrepl.transport :as t])
  (:import
   [nrepl.transport Transport]))

(defn cljs-transport
  [{:keys [^Transport transport] :as msg} post-proc]
  (reify Transport
    (recv [_this]
      (.recv transport))

    (recv [_this timeout]
      (.recv transport timeout))

    (send [this response]

      (cond (contains? response :value)
            (let [rsp-val (:value response)
                  ;; this is HACKY, but the ClojureScript middleware can
                  ;; return (:value response) as a Map/Vector/etc or the thing as a String
                  ;; if it contains things like [#flow-storm.types/value-ref 5]
                  rsp-val (if (string? rsp-val)
                            (read-string rsp-val)
                            rsp-val)
                  processed-val (post-proc rsp-val)
                  rsp (response-for msg processed-val)]
              (.send transport rsp))

            ;; If the eval errored, propagate the exception as error in the
            ;; inspector middleware, so that the client CIDER code properly
            ;; renders it instead of silently ignoring it.
            (and (contains? (:status response) :eval-error)
                 (contains? response ::caught/throwable))
            (let [e (::caught/throwable response)
                  resp (base-error-response msg e :inspect-eval-error :done)]
              (.send transport resp))

            :else (.send transport response))
      this)))


(def descriptor
  (cljs-utils/expects-piggieback
   {:handles {"indef"
              {:doc "Inline def function arguments"
               :requires {}
               :optional {}
               :returns {}}}}))

(defn wrap-indef
  "Middleware that provides indef functionality"
  [next-handler]
  (fn [{:keys [op] :as msg}]
    (case op
      "indef" (handle-indef next-handler msg)
      (next-handler msg))))

(set-descriptor! #'wrap-indef {:requires #{"clone"}})

(comment

  (require '[nrepl.server :as ser])
  (def nrep (ser/start-server :port 55804
                              :handler (ser/default-handler #'wrap-indef)))

  (require '[nrepl.core :as nrepl])
  (with-open [conn (nrepl/connect :port 55804)]
     (-> (nrepl/client conn 1000)    ; message receive timeout required
         ;(nrepl/message {:op "inspect-nrebl" :code "[1 2 3 4 5 6 7 8 9 10 {:a :b :c :d :e #{5 6 7 8 9 10}}]"})
         (nrepl/message {:op "indef" :code "(defn moi [hei jes] jes )"})
         nrepl/response-values)))
