(ns server.core-test
  (:require
   [midje.sweet :refer [=> fact facts]]
   [server.core :refer :all]
   [server.main-game :as main]
   [server.singleplayer-game :as single]
   [server.game-helper-func :refer :all]))

(fact "in-bounds? checks if a position is within the playing field"
      (in-bounds? [50 50] 200 20) => true
      (in-bounds? [10 50] 200 20) => false
      (in-bounds? [190 50] 200 20) => false
      (in-bounds? [50 10] 200 20) => false
      (in-bounds? [50 190] 200 20) => false)

(fact "vector-contains? checks if an element is in a vector"
      (vector-contains? [1 2 3] 2) => true
      (vector-contains? [1 2 3] 4) => nil
      (vector-contains? [] 1) => nil)

(facts "Testing generate-valid-coordinate-pair"
       (let [field-size 500
             grid-size 20
             snake1 [[240 120] [260 120]]
             snake2 [[100 200] [120 200]]
             [x y] (generate-valid-coordinate-pair-ball field-size grid-size snake1 snake2)
             coordinate [x y]]
         (fact (in-bounds? [x y] field-size grid-size) => true)
         (fact (vector-contains? snake1 coordinate) => nil)
         (fact (vector-contains? snake2 coordinate) => nil)))

(facts "Testing find-players-by-socket"
       (let [game1 {:player1 {:socket "socket1"} :player2 {:socket "socket2"}}
             game2 {:player1 {:socket "socket3"}}
             online-games {:1 (atom game1)
                           :2 (atom game2)}]

         (fact "Should find players by matching socket"
               (deref (find-players-by-socket "socket1" online-games)) => {:player1 {:socket "socket1"}
                                                                             :player2 {:socket "socket2"}})

         (fact "Should find players for another matching socket"
               (deref (find-players-by-socket "socket3" online-games)) => {:player1 {:socket "socket3"}})

         (fact "Should return nil for a socket that doesn't exist"
               (find-players-by-socket "socket6" online-games) => nil)))

(facts "Move snake"
       (main/move-snake [[50 50] [50 60] [50 70]] :up 10) => [[50 40] [50 50] [50 60]]
       (main/move-snake [[50 50] [50 60] [50 70]] :down 10) => [[50 60] [50 50] [50 60]]
       (main/move-snake [[50 50] [50 60] [50 70]] :left 10) => [[40 50] [50 50] [50 60]]
       (main/move-snake [[50 50] [50 60] [50 70]] :right 10) => [[60 50] [50 50] [50 60]]
       (single/move-snake [[5 5] [5 6] [5 7]] :up 1 10) => [[5 4] [5 5] [5 6]]
       (single/move-snake [[5 5] [5 4] [5 3]] :down 1 10) => [[5 6] [5 5] [5 4]]
       (single/move-snake [[5 5] [6 5] [7 5]] :left 1 10) => [[4 5] [5 5] [6 5]]
       (single/move-snake [[5 5] [4 5] [3 5]] :right 1 10) => [[6 5] [5 5] [4 5]]
       (single/move-snake [[5 0] [5 9] [5 8]] :up 1 10) => [[5 9] [5 0] [5 9]]
       (single/move-snake [[5 9] [5 8] [5 7]] :down 1 10) => [[5 0] [5 9] [5 8]]
       (single/move-snake [[0 5] [9 5] [8 5]] :left 1 10) => [[9 5] [0 5] [9 5]]
       (single/move-snake [[9 5] [8 5] [7 5]] :right 1 10) => [[0 5] [9 5] [8 5]])

(facts "On eat snake"
       (let [game-state (atom {:snake1 [[162 108] [135 108] [108 108] [81 108]]
                               :snake2 [[405 486] [432 486] [459 486] [486 486]]
                               :ball [351/2 243/2]
                               :score [0 0]})]
         (main/update-game-on-eat game-state 27)
         (fact (count (:snake1 @game-state)) => 5)
         (fact (not= (:ball @game-state) [162 108]) => true)
         (fact (:score @game-state) => [1 0])
         (swap! game-state (fn [game-state] (assoc game-state :ball [837/2 999/2])))
         (main/update-game-on-eat game-state 27)
         (fact (count (:snake2 @game-state)) => 5)
         (fact (not= (:ball @game-state) [162 108]) => true)
         (fact (:score @game-state) => [1 1])
         (main/update-game-on-eat game-state 27)
         (fact (count (:snake1 @game-state)) => 5)
         (fact (count (:snake2 @game-state)) => 5)
         (fact (:score @game-state) => [1 1])
         (single/update-game-on-eat game-state 27)
         (fact (count (:snake1 @game-state)) => 5)
         (swap! game-state (fn [game-state] (assoc game-state :ball [351/2 243/2])))
         (single/update-game-on-eat game-state 27)
         (fact (count (:snake1 @game-state)) => 6)))