(ns client.game.game-helper-func
   (:require [quil.core :as q]))

(def dark-purple [110 0 123])
(def light-purple [182 127 189])
(def white [237 239 239])

(defn draw-grid [cell-size]
  (let [w (/ (q/width) cell-size)
        h (/ (q/height) cell-size)]
    (doseq [y (range h)
            x (range w)]
      (let [color (cond
                    (even? y) (if (even? x) dark-purple light-purple)
                    :else     (if (even? x) light-purple white))]
        (q/fill (apply q/color color))
        (q/rect (* x cell-size) (* y cell-size) cell-size cell-size)))))