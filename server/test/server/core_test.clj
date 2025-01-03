(ns server.core-test
  (:require
   [midje.sweet :refer [facts fact =>]]
   [server.core :refer :all]
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

(let [field-size 500
      grid-size 20
      snake1 [[240 120] [260 120]]
      snake2 [[100 200] [120 200]]]
  (facts "Testing generate-valid-coordinate-pair"
         (fact "Generated coordinate pair should be within the field bounds"
               (let [[x y] (generate-valid-coordinate-pair-ball field-size grid-size snake1 snake2)]
                 (in-bounds? [x y] field-size grid-size) => true))
         (fact "Generated coordinate pair should not be in snake1"
               (let [coordinate (generate-valid-coordinate-pair-ball field-size grid-size snake1 snake2)]
                 (vector-contains? snake1 coordinate) => nil))
         (fact "Generated coordinate pair should not be in snake2" 
                 (let [coordinate (generate-valid-coordinate-pair-ball field-size grid-size snake1 snake2)]
                   (vector-contains? snake2 coordinate) => nil))))

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
