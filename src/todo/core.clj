(ns todo.core
  (:require
   [seesaw.core :as ss]
   [clojure.reflect :as reflect]
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

(defn- set-notes [todos listbox]
  (let [selected-todo (ss/selection listbox)
        notes (select-first (ss/to-root listbox) :#notes)
        new-note-text (:notes (todos selected-todo))]
    (ss/config! notes :text new-note-text)))

(defn- create-widgets
  "Return widgets."
  []
  (let
   [list (ss/listbox :id :todo-list
                     :model (keys (:todos @*state))
                     :maximum-size [(/ width 2) :by height]
                     :listen [:selection
                              (fn [x]
                                (let [listbox (.getSource x)]
                                  (ss/config! (select-first (ss/to-root listbox) :#notes) :editable? true)
                                  (set-notes (:todos @*state) listbox)))])
    add-text (ss/text :id :add-text
                      :text ""
                      :editable? true
                      :maximum-size [(/ width 2) :by 30])
    error-text (ss/text :id :error-text
                        :text ""
                        :editable? false
                        :maximum-size [(/ width 2) :by 0])
    notes (ss/text :id :notes
                   :editable? (not (nil? (ss/selection list)))
                   :multi-line? true
                   :border (MetalBorders$TextFieldBorder.)
                   :listen [:key-released
                            (fn [x]
                              (let [frame (ss/to-root (.getSource x))
                                    n (select-first frame :#notes)
                                    selected-note (ss/selection (select-first frame :#todo-list))]
                                (swap! *state #(assoc-in % [:todos selected-note :notes] (.getText n)))))])
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
           :minimum-size  [width :by height]
           :title "To Dos"
           :content (ss/grid-panel :columns 2
                                   :items
                                   [(ss/vertical-panel
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
        ss/pack!
        (ss/move-to! 100 0)
        ss/show!)))

(comment
  (ss/config!
   (select-first *frame :#notes)
   :listen [:key-pressed (fn [_] (println "from config2"))])
  (-main)
  (ss/selection (select-first *frame :#todo-list))
  ss/pack!
  (show-events (ss/listbox))
  (type (ss/text))
  (reset! *state {:todos {}})
  @*state)