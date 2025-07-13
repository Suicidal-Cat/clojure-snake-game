(ns client.helper-func
  (:require [reagent.core :as r]
            [clojure.edn :as edn]))

;; screenshot
(def img-atom (r/atom nil))

;; game mode enum
(def game-mode-enum {:time "Time" :cake "Cake"})

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

(defn format-time [ms]
  (let [total-seconds (js/Math.floor (/ ms 1000))
        minutes (js/Math.floor (/ total-seconds 60))
        seconds (mod total-seconds 60)]
    (str minutes ":" (if (< seconds 10) (str "0" seconds) seconds))))
