(ns run-plotter.utils
  (:require
    [goog.string :as gstring]))

(defn format-distance
  ([distance-in-meters units]
   (format-distance distance-in-meters units 1))
  ([distance-in-meters units decimal-places]
   (let [value-in-km (/ distance-in-meters 1000)
         value (if (= units :miles)
                 (* value-in-km 0.621371)
                 value-in-km)]
     (gstring/format (str "%." decimal-places "f %s") value (name units)))))

(defn display-toast
  [message]
  (js/bulmaToast.toast #js {:message message
                            :position "top-center"
                            :type "is-success"}))