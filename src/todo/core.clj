(ns todo.core
  (:require [todo.events :as e]
            [clojure.core.async :as async :refer (chan dropping-buffer go-loop put! <!)]
            [seesaw.core :as ss]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [seesaw.dev :refer (show-events show-options)])
  (:import [javax.swing DefaultListModel ListModel]
           [javax.swing.plaf.metal MetalBorders$TextFieldBorder]))

(defonce ^{:doc "Application state."}
  *state
  (atom {:todos {} :selected-todo nil}))

(def width  800)
(def height 400)

(defn- select-first
  "Helper for selecting the first element by class or id."
  [frame ^clojure.lang.Keyword kw]
  (let [result (ss/select frame [kw])]
    (if (seq? result)
      (first result)
      result)))

(defn- render-todo-list
  "Sorts the todo list, putting completed todos at the bottom. Then sorts todos alphabetically by title."
  [todos]
  (let [sorted-todos (sort-by (fn [[k v]] [(get v :complete?) k]) todos)]
    (map (fn [[name value]]
           (if (:complete? value)
             (str "COMPLETE - " name)
             name))
         sorted-todos)))

(comment
  (let [todos {"todo" {:notes "I need to do this thing.\n\nI did it!", :complete? true}
               "" {:notes ""}
               "another todo" {:notes "I'm not done yet!"}
               "asdfasdf" {:notes "asdfasd", :complete? true}}]
    (render-todo-list todos))
  )

(defn- create-list-model
  "Generates a ListModel when given a todo map."
  ^ListModel [^clojure.lang.PersistentArrayMap todos]
  (let [model (DefaultListModel.)
        elements (render-todo-list todos)]
    (doseq [k elements]
      (.addElement model k))
    model))

(defn- remove-complete
  "Remove the 'COMPLETE - ' prefix for completed todos."
  [^String s]
  (str/replace s "COMPLETE - " ""))

(defn- set-notes [todos listbox]
  (when (ss/selection listbox)
    (let [selected-todo (remove-complete (ss/selection listbox))
          notes (select-first (ss/to-root listbox) :#notes)
          new-note-text (:notes (todos selected-todo))]
      (ss/config! notes :text new-note-text))))

(defn- get-source
  "Gets the source widget for the event."
  [x]
  (.getSource x))

(comment
  (remove-complete "abc")
  (remove-complete "COMPLETE - abc"))

(defn- get-frame
  "Gets the frame from an event."
  [event]
  (-> event
      get-source
      ss/to-root))

(defn- get-selected-todo
  "When given an event object, returns the currently selected todo in the app."
  [event]
  (-> event
      get-frame
      (select-first :#todo-list)
      ss/selection
      remove-complete))

(comment
  (declare -main)
  (-main)
  )

(defn- create-widgets
  "Return widgets."
  [out-channel]
  (let
   [list (ss/listbox :id :todo-list
                     :model (create-list-model (:todos @*state))
                     :maximum-size [(/ width 2) :by height]
                     :listen [:selection
                              (fn [x]
                                (when-not (.getValueIsAdjusting x)
                                  (put! out-channel [::e/todo-selected {:event x :state *state}])
                                  (let [listbox (get-source x)]
                                    (ss/config! (select-first (ss/to-root listbox) :#notes) :editable? true)
                                    (set-notes (:todos @*state) listbox))))])
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
                            (put! out-channel [::e/note-written {:event x :state *state}])
                              (let [frame (get-frame x)
                                    n (select-first frame :#notes)
                                    selected-todo (get-selected-todo x)]
                                (swap! *state #(assoc-in % [:todos selected-todo :notes] (.getText n)))))])
    add-fn (fn [x]
             (put! out-channel [::e/add-note {:event x :state *state}])
             (let [todo (ss/config add-text :text)
                   todos (:todos @*state)]
               (if-not (contains? todos todo)
                 (do
                   (swap! *state #(assoc % :todos (conj todos [todo {:notes ""}])))
                   (.setModel list (create-list-model (:todos @*state))))
                 (ss/config! error-text :text (str "Error: there is already a todo named " todo)))))
    add-btn (ss/button :listen [:action add-fn] :text "Add")
    h-panel (ss/horizontal-panel :items [add-text add-btn])
    complete-button (ss/button :text "Complete"
                               :listen [:action (fn [x]
                                                  (put! out-channel [::e/complete-task {:event x :state *state}])
                                                  (swap!
                                                   *state
                                                   #(assoc-in % [:todos (get-selected-todo x) :complete?] true))
                                                  (.setModel (select-first (get-frame x) :#todo-list)
                                                             (create-list-model (:todos @*state))))])
    save-button (ss/button :text "Save" :listen [:action (fn [_] (spit "data.edn" @*state))])
    frame (ss/frame
           :minimum-size  [width :by height]
           :title "To Dos"
           :content (ss/grid-panel :columns 2
                                   :items
                                   [(ss/vertical-panel
                                     :items [h-panel
                                             error-text
                                             (ss/scrollable list)])
                                    (ss/vertical-panel
                                     :items [notes
                                             (ss/horizontal-panel :items [complete-button save-button])])])
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
  "Runs the GUI and hydrates the state."
  [& _]
  (ss/native!)
  (when (.exists (io/file "data.edn"))
    (reset! *state (edn/read-string (slurp "data.edn"))))
  (let
   [event-channel (chan (dropping-buffer 100))
    update-channel (chan (dropping-buffer 100))
    {:keys [frame] :as widgets} (create-widgets event-channel)]
    (go-loop []
      (e/handle-event (<! event-channel))
      ; Need to feed result of handle-event to update-channel
      (recur))
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
  (reset! *state {:todos {} :selected-todo nil})
  @*state)