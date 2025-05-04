(ns client.helper-func
  (:require [reagent.core :as r]
            [clojure.edn :as edn]))

(def img-atom (r/atom nil))

;take a screenshoot of the canvas
(defn save-region-screenshot! [x y width height]
  (let [canvas (.getElementById js/document "defaultCanvas0")
        ctx (.getContext canvas "2d")
        image-data (.getImageData ctx x y width height)
        temp-canvas (js/document.createElement "canvas")
        temp-ctx (.getContext temp-canvas "2d")]
    (set! (.-width temp-canvas) width)
    (set! (.-height temp-canvas) height)
    (.putImageData temp-ctx image-data 0 0)
    (let [base64 (.toDataURL temp-canvas)]
      (reset! img-atom base64))))

(defn set-local-storage [key value]
  (.setItem js/localStorage key value))

(defn get-local-storage [key]
  (.getItem js/localStorage key))

(defn get-user-info []
  (edn/read-string (get-local-storage "user")))

(defn get-player-id []
  (let [user (get-user-info)]
    (if (some? user)
      (:id user)
      (.now js/Date))))
