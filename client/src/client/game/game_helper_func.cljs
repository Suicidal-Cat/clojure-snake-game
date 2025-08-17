(ns client.game.game-helper-func
   (:require [quil.core :as q]))

(def dark-purple [110 0 123])
(def light-purple [182 127 189])
(def white [237 239 239])
(def colors [:red :green :yellow])

(defn draw-grid [cell-size] 
  (q/stroke 0)
  (q/stroke-weight 0)
  (let [w (/ (q/width) cell-size)
        h (/ (q/height) cell-size)]
    (doseq [y (range h)
            x (range w)]
      (let [color (cond
                    (even? y) (if (even? x) dark-purple light-purple)
                    :else     (if (even? x) light-purple white))]
        (q/fill (apply q/color color))
        (q/rect (* x cell-size) (* y cell-size) cell-size cell-size)))))

(defn draw-snake [head-im body-im snake size]
  (let [[head & body] snake
        [x y] head]
    (q/image head-im x y size size)
    (doseq [[x y] body]
      (q/image body-im x y size size))))

(defn snake-images [c]
  {:body (q/load-image (str "/images/b" (name c) ".png"))
   :head (q/load-image (str "/images/h" (name c) ".png"))})

(defn random-snake-images []
  (let [[c1 c2] (shuffle colors)
        imgs1 (snake-images c1)
        imgs2 (snake-images c2)]
    {:body1 (:body imgs1)
     :head1 (:head imgs1)
     :body2 (:body imgs2)
     :head2 (:head imgs2)}))

(defn random-snake-image []
  (let [c (rand-nth colors)]
    {:body (q/load-image (str "/images/b" (name c) ".png"))
     :head (q/load-image (str "/images/h" (name c) ".png"))}))