(ns todo.events)

(defmulti handle-event first)

(defmethod handle-event ::todo-selected [_]
  (println "Selected todo!"))

(defmethod handle-event ::note-written [_]
  (println "note written4"))

(defmethod handle-event ::add-note [_]
  (println "note added"))