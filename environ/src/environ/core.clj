(ns environ.core
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

(defn- keywordize [s]
  (-> (str/lower-case s)
      (str/replace "_" "-")
      (str/replace "." "-")
      (keyword)))

(defn- sanitize [k]
  (let [s (keywordize (name k))]
    (if-not (= k s) (println "Warning: environ key " k " was has been corrected to " s))
    s))

(defn- read-system-env []
  (->> (System/getenv)
       (map (fn [[k v]] [(keywordize k) v]))
       (into {})))

(defn- read-system-props []
  (->> (System/getProperties)
       (map (fn [[k v]] [(keywordize k) v]))
       (into {})))

(defn- read-env-file [file-name]
  (let [env-file (io/file file-name)]
    (if (.exists env-file)
      (into {} (for [[k v] (read-string (slurp env-file))]
                 [(sanitize k) v])))))

(defn- load-subconfig [source-fn]
  (let [cfg (source-fn)]
    (if-let [subconf-file (:config cfg)]
      (merge cfg (read-env-file subconf-file))
      cfg)))

(def ^{:doc "A map of environment variables."}
  env
  (->> [(partial read-env-file ".lein-env") read-system-props read-system-env]
       (map load-subconfig)
       (apply merge)))
