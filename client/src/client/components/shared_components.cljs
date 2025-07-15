(ns client.components.shared-components)

(defn spinner []
  [:div {:class "spinner"}])

(defn divider [title]
  [:div {:class "divider"} title])