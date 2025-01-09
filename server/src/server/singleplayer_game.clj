(ns server.singleplayer-game
  (:require
   [ring.websocket :as ws]
   [server.game-helper-func :refer [generate-valid-coordinate-pair-ball
                                    vector-contains?]]))