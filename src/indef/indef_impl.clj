(ns indef.indef-impl
  (:require
   [cider.nrepl.middleware.util.cljs :as cljs]
   [clojure.string :as string]
   [clojure.walk :as walk]))

(defn- get-argument-symbols [argument-vector]
  (let [symbols (atom [])]
    (walk/prewalk (fn [node]
                    (when (symbol? node)
                      (reset! symbols (conj @symbols node))) node)
                  argument-vector)
    @symbols))

(defn- argument-symbols->inline-defs [argument-symbols]
  (->> argument-symbols
       (map (fn [symbol]
              (list 'def symbol symbol)))))

(defn- insert-inline-def-to-fn [form]
  (let [[args prepost-map body] form
        argument-symbols        (get-argument-symbols args)
        inline-defs             (argument-symbols->inline-defs argument-symbols)
        prepost-map* (if (nil? body) nil prepost-map)
        body* (if (nil? body) prepost-map body)]
    (remove nil? (concat (list args prepost-map* argument-symbols) inline-defs (list body*)))))

(defn- parser [forms]
  (let [expanded (macroexpand-1 forms)
        [_def-signature fn-name functions] expanded
        bodies-with-inline-defs (map insert-inline-def-to-fn (rest functions))]
    `(defn ~fn-name ~@bodies-with-inline-defs)))

(defn- eval-shadow-cljs
  [msg code ns]
  (let [build-id (-> msg :shadow.cljs.devtools.server.nrepl-impl/build-id)]
    (when-let [cljs-eval (resolve 'shadow.cljs.devtools.api/cljs-eval)]
      (cljs-eval build-id code {:ns (symbol ns)})
      {})))

(defn handle-indef
  [handler {:keys [code ns] :as msg}]
  (if (cljs/grab-cljs-env msg)
    (if (string/starts-with? code "(defn")
      (do
        (eval-shadow-cljs msg
                          (str (parser (read-string code)))
                          ns)
        {})
      {})
    (if (string/starts-with? code "(defn")
      (let [msg2 (assoc msg
                        :code (str (parser (read-string code)))
                        :op "eval")]
        (handler msg2))
      {})))
