(ns todo.core
  (:require
   [seesaw.core :as ss]
   [seesaw.dev :refer (show-events show-options)]))

(defn -main [& _]
  (let [list (ss/listbox :model ["first thing"
                                 "second thing"])
        add-text (ss/text :text "" :editable? true)
        add-fn (fn [_]
                 (let [to-do (ss/config add-text :text)
                       model (.getModel list)]
                   (.addElement model to-do)))
        add-btn (ss/button :listen [:action add-fn] :text "Add")
        h-panel (ss/horizontal-panel :items [add-text add-btn])
        frame (ss/frame :title "To Dos"
                        :content (ss/vertical-panel :items [h-panel (ss/scrollable list)])
                        :on-close :dispose)]
    (-> frame
        ss/pack!
        (ss/move-to! 100 0)
        ss/show!)))

(comment
  (-main)
  )