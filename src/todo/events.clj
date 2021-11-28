(ns todo.events
  (:require [clojure.string :as str]))

(defn remove-complete
  "Remove the 'COMPLETE - ' prefix for completed todos."
  [^String s]
  (str/replace s "COMPLETE - " ""))

(defmulti handle-event first)

(defmethod handle-event ::todo-selected [[_ {:keys [selected-todo state]}]]
  (let [new-text (get-in state [:todos (remove-complete selected-todo) :notes])]
    {:update
     {:#notes
      {:editable? true
       :text new-text}}}))

(defmethod handle-event ::note-written [[_ {:keys [note]}]]
  (println "note written4" note))

(defmethod handle-event ::note-added [[_ {:keys [new-note]}]]
  (println "note added" new-note))

(defmethod handle-event ::complete-todo [[_ {:keys [todo]}]]
  (println "task completed" todo))