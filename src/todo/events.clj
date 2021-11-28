(ns todo.events)

(defmulti handle-event first)

(defmethod handle-event ::todo-selected [[_ {:keys [selected-todo]}]]
  (println "Selected todo!" selected-todo)
  )

(defmethod handle-event ::note-written [[_ {:keys [note]}]]
  (println "note written4" note))

(defmethod handle-event ::note-added [[_ {:keys [new-note]}]]
  (println "note added" new-note))

(defmethod handle-event ::complete-todo [[_ {:keys [todo]}]]
  (println "task completed" todo))