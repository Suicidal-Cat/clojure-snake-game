(ns client.game.game-helper-func
  (:require [quil.core :as q]))

(def dark-red [183 73 73])
(def light-red [217 149 149])
(def white [237 239 239])
(def light-brown [247 213 161])
(def dark-brown [201 176 103])
(def light-purple [197 154 252])
(def dark-purple [146 84 227])
(def colors [:red :green :yellow])

(defn draw-grid [cell-size]
  (q/stroke 0)
  (q/stroke-weight 0)
  (let [w (/ (q/width) cell-size)
        h (/ (q/height) cell-size)]
    (doseq [y (range h)
            x (range w)]
      (let [color (cond
                    (even? y) (if (even? x) dark-red light-red)
                    :else     (if (even? x) light-red white))]
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

(defn get-food-image []
  (rand-nth ["strawberryCake.png","cherryCake.png","appleCake.png",
             "apple.png","banana.png","cherry.png",
             "grape.png","honey.png","lemon.png",
             "strawberry.png"]))

(defn draw-grid-standard [cell-size]
   (q/background (apply q/color light-purple))
   (q/no-stroke)
   (let [w (/ (q/width) cell-size)
         h (/ (q/height) cell-size)
         square-size (- cell-size 3)
         radius 8]
     (doseq [y (range h)
             x (range w)]
       (q/fill (apply q/color dark-purple))
       (q/rect (+ (* x cell-size) (/ (- cell-size square-size) 2))
               (+ (* y cell-size) (/ (- cell-size square-size) 2))
               square-size
               square-size
               radius))))

;; pulse animation
(defn pulse-normal [min max]
  (let [t (/ (+ 1 (Math/sin (/ (q/frame-count) 13))) 2)]
    (+ min (* t (- max min)))))

(defn move-y [range base-y]
  (let [t (/ (q/frame-count) 10.0)]
    (+ base-y (* (q/sin t) range))))

;; draw player indicaator
(defn draw-indicator [image-key [x y]]
  (q/image (q/state (keyword image-key)) x y 30 40))

; draw indicator
(defn draw-player-indicator[indicator-img game-state player-id]
  (when (:snake1-id @game-state)
    (let [[head _] (if (= @player-id (:snake1-id @game-state))
                     (:snake1 @game-state)
                     (:snake2 @game-state))] 
      (swap! (q/state-atom) assoc :ind-y (move-y 5 (- (head 1) 45)))
      (draw-indicator indicator-img [(head 0) (q/state :ind-y)]))))