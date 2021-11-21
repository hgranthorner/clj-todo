(ns todo.core
  (:require
   [seesaw.core :as ss]
   [seesaw.dev :refer (show-events show-options)])
  (:import [javax.swing DefaultListModel ListModel]
           [javax.swing.plaf.metal MetalBorders$TextFieldBorder]))

(defonce ^{:doc "Application state."}
  *state
  (atom {:todos {}}))

(def width  800)
(def height 400)

(defn- select-first
  "Helper for selecting the first element by class."
  [frame ^clojure.lang.Keyword kw]
  (let [result (ss/select frame [kw])]
    (if (seq? result)
      (first result)
      result)))

(defn- create-list-model ^ListModel [^clojure.lang.PersistentArrayMap todos]
  (let [model (DefaultListModel.)]
    (doseq [k (keys todos)]
      (.addElement model k))
    model))

(defn- create-widgets
  "Return widgets."
  []
  (let
   [list (ss/listbox :model
                     (mapv keys (:todos @*state)))
    add-text (ss/text :id :add-text
                      :text ""
                      :editable? true)
    error-text (ss/text :id :error-text
                        :text ""
                        :editable? false
                        :minimum-size [0 :by 0])
    notes (ss/text :id :notes
                   :editable? true
                   :multi-line? true
                   :border (MetalBorders$TextFieldBorder.)
                   :listen [:key-typed (fn [x] (println x))])
    add-fn (fn [_]
             (let [todo (ss/config add-text :text)
                   todos (:todos @*state)]
               (if-not (contains? todos todo)
                 (do
                   (swap! *state #(assoc % :todos (conj todos [todo {:notes ""}])))
                   (.setModel list (create-list-model (:todos @*state))))
                 (ss/config! error-text :text (str "Error: there is already a todo named " todo)))))
    add-btn (ss/button :listen [:action add-fn] :text "Add")
    h-panel (ss/horizontal-panel :items [add-text add-btn])
    frame (ss/frame
           :size  [width :by height]
           :title "To Dos"
           :content (ss/horizontal-panel
                     :maximum-size [(/ width 2) :by height]
                     :items
                     [(ss/vertical-panel
                       :maximum-size [(/ width 2) :by height]
                       :items [h-panel
                               error-text
                               (ss/scrollable list)])
                      notes])
           :on-close :dispose)]
    {:list list
     :add-fn add-fn
     :add-text add-text
     :error-text error-text
     :notes notes
     :add-btn add-btn
     :h-panel h-panel
     :frame frame}))

(defn -main
  "Runs the GUI."
  [& _]
  (let [{:keys [frame]} (create-widgets)]
    (def ^:dynamic *frame frame)
    (-> frame
        (ss/move-to! 100 0)
        ss/show!)))

(comment
  (ss/config!
   (select-first *frame :#notes)
   :listen [:key-pressed (fn [_] (println "from config2"))])
  (-main)
  ss/pack!
  (show-options (ss/vertical-panel))
  (type (ss/text))
  @*state)